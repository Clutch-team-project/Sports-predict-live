"""
KBO 선수 명단(로스터) 크롤러
- KBO 공식 선수 검색 페이지에서 구단별 전체 선수를 수집해 player 테이블에 upsert
- 출전 기록이 없는 선수도 등록되므로 경기 라인업 표시에 활용 가능
- 기록 크롤러(kbo_crawler.py)와 동일한 external_player_id(KBO playerId)를 사용하므로
  먼저 실행해두면 기록 크롤러가 같은 선수에 기록만 얹는다

실행 방법:
    pip install requests beautifulsoup4 pymysql
    python kbo_roster_crawler.py

권장 실행 주기: 주 1회 (트레이드/등록 변동 반영)
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

URL_SEARCH = "https://www.koreabaseball.com/Player/Search.aspx"

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    )
}

# 검색 페이지 팀 코드 → DB team.name (검색 폼 순회용)
TEAM_CODE_MAP = {
    "LG": "LG",
    "KT": "KT",
    "SS": "삼성",
    "HT": "KIA",
    "HH": "한화",
    "OB": "두산",
    "NC": "NC",
    "SK": "SSG",
    "LT": "롯데",
    "WO": "키움",
}

# 검색 결과의 팀명 표기 → DB team.name 보정 (그 외는 표기 그대로 DB 팀명과 일치)
TEAM_NAME_ALIAS = {
    "고양": "키움",  # 키움 산하 퓨처스(고양 히어로즈)
}

# ASP.NET 컨트롤 prefix
CTL = "ctl00$ctl00$ctl00$cphContents$cphContents$cphContents$"


def get_db():
    config = DB_CONFIG.copy()
    config["password"] = config["password"].encode("utf-8")
    return pymysql.connect(**config)


def load_team_map(conn) -> dict:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT t.name, t.team_id FROM team t "
            "JOIN sport s ON t.sport_id = s.sport_id "
            "WHERE s.code = 'baseball'"
        )
        return {row[0]: row[1] for row in cur.fetchall()}


#  검색 페이지 크롤링 (ASP.NET postback)

def extract_hidden_fields(soup: BeautifulSoup) -> dict:
    return {
        inp["name"]: inp.get("value", "")
        for inp in soup.select('input[type="hidden"]')
        if inp.get("name", "").startswith("__")
    }


def parse_rows(soup: BeautifulSoup) -> list[dict]:
    results = []
    for tr in soup.select("tbody tr"):
        tds = tr.find_all("td")
        if len(tds) < 5:
            continue

        a = tds[1].find("a", href=True)
        if not a:
            continue
        m = re.search(r"playerId=(\d+)", a["href"])
        if not m:
            continue

        jersey_raw = tds[0].get_text(strip=True)
        birth_raw  = tds[4].get_text(strip=True)

        results.append({
            "external_player_id": m.group(1),
            "jersey_number": int(jersey_raw) if jersey_raw.isdigit() else None,
            "name":          a.get_text(strip=True),
            "team_code":     tds[2].get_text(strip=True),
            "position":      tds[3].get_text(strip=True),  # 투수/포수/내야수/외야수
            "birth_date":    birth_raw if re.match(r"\d{4}-\d{2}-\d{2}", birth_raw) else None,
        })
    return results


def crawl_team_roster(session: requests.Session, team_code: str) -> list[dict]:
    # 1. 초기 페이지에서 hidden 필드 확보
    res = session.get(URL_SEARCH, headers=HEADERS, timeout=15)
    res.raise_for_status()
    soup = BeautifulSoup(res.text, "html.parser")

    def post(event_target: str, hidden: dict) -> BeautifulSoup:
        data = dict(hidden)
        data["__EVENTTARGET"] = event_target
        data[CTL + "ddlTeam"] = team_code
        data[CTL + "ddlPosition"] = ""
        data[CTL + "txtSearchPlayerName"] = ""
        r = session.post(URL_SEARCH, data=data, headers={
            **HEADERS,
            "Content-Type": "application/x-www-form-urlencoded",
            "Referer": URL_SEARCH,
        }, timeout=15)
        r.raise_for_status()
        return BeautifulSoup(r.text, "html.parser")

    # 2. 팀 선택 → 1페이지
    soup = post(CTL + "ddlTeam", extract_hidden_fields(soup))
    players = parse_rows(soup)

    # 3. 페이지네이션 (btnNo2, btnNo3 ... 링크가 있는 동안 순회)
    page = 2
    while True:
        pager_id = f"ucPager_btnNo{page}"
        if not soup.find("a", id=re.compile(pager_id + "$")):
            break
        soup = post(CTL + f"ucPager$btnNo{page}", extract_hidden_fields(soup))
        rows = parse_rows(soup)
        if not rows:
            break
        players.extend(rows)
        page += 1

    return players


#  DB 저장

def save_roster(conn, team_map: dict, data: list[dict]):
    sql_select = "SELECT player_id FROM player WHERE source='kbo' AND external_player_id=%s"
    sql_insert = """
        INSERT INTO player (team_id, source, external_player_id, name, position, jersey_number, birth_date)
        VALUES (%s, 'kbo', %s, %s, %s, %s, %s)
    """
    sql_update = """
        UPDATE player
        SET team_id=%s, name=%s, position=%s, jersey_number=%s, birth_date=%s
        WHERE player_id=%s
    """
    inserted = updated = skipped = 0
    with conn.cursor() as cur:
        for row in data:
            # 결과 테이블의 팀명은 한글 구단명(삼성, KIA 등)으로 DB team.name과 동일
            display_name = row["team_code"]
            team_name = TEAM_NAME_ALIAS.get(display_name, display_name)
            team_id = team_map.get(team_name)
            if not team_id:
                print(f"  [SKIP] 팀 매핑 실패: {row['team_code']} ({row['name']})")
                skipped += 1
                continue

            cur.execute(sql_select, (row["external_player_id"],))
            existing = cur.fetchone()
            if existing:
                cur.execute(sql_update, (
                    team_id, row["name"], row["position"],
                    row["jersey_number"], row["birth_date"],
                    existing[0],
                ))
                updated += 1
            else:
                cur.execute(sql_insert, (
                    team_id, row["external_player_id"], row["name"],
                    row["position"], row["jersey_number"], row["birth_date"],
                ))
                inserted += 1
    conn.commit()
    print(f"  저장 완료: 신규 {inserted}명 / 갱신 {updated}명 / 스킵 {skipped}명")


#  메인

def main():
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] KBO 선수 명단 크롤링 시작")

    conn = get_db()
    session = requests.Session()
    try:
        team_map = load_team_map(conn)

        for team_code, team_name in TEAM_CODE_MAP.items():
            print(f"\n▶ {team_name} ({team_code}) 명단 크롤링...")
            players = crawl_team_roster(session, team_code)
            print(f"  수집: {len(players)}명")
            save_roster(conn, team_map, players)
    finally:
        conn.close()

    print(f"\n[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] 완료")


if __name__ == "__main__":
    main()
