"""
K리그 선수 명단(로스터) 크롤러 (K리그1 전용)
- kleague.com 선수 검색 페이지에서 구단별/포지션별 전체 선수를 수집해 player 테이블에 upsert
- 출전 기록이 없는 선수도 등록되므로 경기 라인업 표시에 활용 가능
- 기록 크롤러(kleague_crawler.py)와 동일한 external_player_id(K리그 playerId)를 사용
  (기록 크롤러가 이름으로 임시 등록한 선수는 이름+팀 매칭으로 ID를 보정해 병합)

실행 방법:
    pip install requests beautifulsoup4 pymysql
    python kleague_roster_crawler.py

권장 실행 주기: 주 1회 (이적/등록 변동 반영)
"""

import re
import requests
from bs4 import BeautifulSoup
import pymysql
from datetime import datetime

#  DB 설정
DB_CONFIG = {
    "host":     "localhost",
    "port":     3306,
    "user":     "root",
    "password": "1234",
    "database": "ai_match",
    "charset":  "utf8mb4",
}

LEAGUE_ID = "1"  # K리그1

URL_PLAYER = "https://www.kleague.com/player.do"

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    ),
    "Referer": "https://www.kleague.com/player.do",
}

# kleague.com 구단 코드 → DB team.name (K리그1 2026 12개 팀)
TEAM_CODE_MAP = {
    "K01": "울산",
    "K03": "포항",
    "K04": "제주",
    "K05": "전북",
    "K09": "서울",
    "K10": "대전",
    "K18": "인천",
    "K21": "강원",
    "K22": "광주",
    "K26": "부천",
    "K27": "안양",
    "K35": "김천",
}

# 검색 포지션 필터 값 → DB position 표기
POSITIONS = {
    "gk": "GK",
    "df": "DF",
    "mf": "MF",
    "fw": "FW",
}


def get_db():
    config = DB_CONFIG.copy()
    config["password"] = config["password"].encode("utf-8")
    return pymysql.connect(**config)


def load_team_map(conn) -> dict:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT t.name, t.team_id FROM team t "
            "JOIN sport s ON t.sport_id = s.sport_id "
            "WHERE s.code = 'soccer'"
        )
        return {row[0]: row[1] for row in cur.fetchall()}


#  선수 검색 페이지 크롤링

def crawl_team_position(session: requests.Session, team_code: str, pos: str) -> list[dict]:
    params = {
        "searchWord": "",
        "type":       "active",
        "leagueId":   LEAGUE_ID,
        "teamId":     team_code,
        "pos":        pos,
    }
    res = session.get(URL_PLAYER, params=params, headers=HEADERS, timeout=15)
    res.raise_for_status()
    soup = BeautifulSoup(res.text, "html.parser")

    results = []
    for card in soup.select("div.player-hover[onclick]"):
        m = re.search(r"onPlayerClicked\((\d+)\)", card.get("onclick", ""))
        if not m:
            continue

        name_el = card.select_one("span.name")
        if not name_el:
            continue
        # <span class="name">강현무<span class="small">서울</span></span> → 첫 텍스트만
        name = name_el.find(text=True, recursive=False)
        name = name.strip() if name else ""
        if not name:
            continue

        num_el = card.select_one("span.num")
        jersey = None
        if num_el:
            nm = re.search(r"No\.(\d+)", num_el.get_text(strip=True))
            if nm:
                jersey = int(nm.group(1))

        img_el = card.select_one(".img-box img")
        profile_image = img_el["src"] if img_el and img_el.get("src", "").startswith("http") else None

        results.append({
            "external_player_id": m.group(1),
            "name":          name,
            "position":      POSITIONS[pos],
            "jersey_number": jersey,
            "profile_image": profile_image,
        })
    return results


#  DB 저장

def save_roster(conn, team_id: int, data: list[dict]):
    sql_by_ext = "SELECT player_id FROM player WHERE source='kleague' AND external_player_id=%s"
    # 기록 크롤러가 이름을 임시 ID로 등록한 경우 병합용
    sql_by_name = "SELECT player_id FROM player WHERE source='kleague' AND name=%s AND team_id=%s"
    sql_insert = """
        INSERT INTO player (team_id, source, external_player_id, name, position, jersey_number, profile_image)
        VALUES (%s, 'kleague', %s, %s, %s, %s, %s)
    """
    sql_update = """
        UPDATE player
        SET team_id=%s, external_player_id=%s, name=%s, position=%s, jersey_number=%s, profile_image=%s
        WHERE player_id=%s
    """
    inserted = updated = 0
    with conn.cursor() as cur:
        for row in data:
            cur.execute(sql_by_ext, (row["external_player_id"],))
            existing = cur.fetchone()
            if not existing:
                cur.execute(sql_by_name, (row["name"], team_id))
                existing = cur.fetchone()

            if existing:
                cur.execute(sql_update, (
                    team_id, row["external_player_id"], row["name"], row["position"],
                    row["jersey_number"], row["profile_image"],
                    existing[0],
                ))
                updated += 1
            else:
                cur.execute(sql_insert, (
                    team_id, row["external_player_id"], row["name"], row["position"],
                    row["jersey_number"], row["profile_image"],
                ))
                inserted += 1
    conn.commit()
    print(f"  저장 완료: 신규 {inserted}명 / 갱신 {updated}명")


#  메인

def main():
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] K리그 선수 명단 크롤링 시작")

    conn = get_db()
    session = requests.Session()
    try:
        team_map = load_team_map(conn)

        for team_code, team_name in TEAM_CODE_MAP.items():
            team_id = team_map.get(team_name)
            if not team_id:
                print(f"\n▶ [SKIP] DB에 팀 없음: {team_name} ({team_code})")
                continue

            print(f"\n▶ {team_name} ({team_code}) 명단 크롤링...")
            players = []
            for pos in POSITIONS:
                players.extend(crawl_team_position(session, team_code, pos))
            print(f"  수집: {len(players)}명")
            save_roster(conn, team_id, players)
    finally:
        conn.close()

    print(f"\n[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] 완료")


if __name__ == "__main__":
    main()
