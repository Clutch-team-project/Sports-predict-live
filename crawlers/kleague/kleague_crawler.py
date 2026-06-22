"""
K리그 크롤러 (K리그1 전용)
- 팀 순위 (team_season_stat)
- 경기 일정/결과 (match)
- 선수 기록 (player_season_stat_soccer) ← Selenium 필요

실행 방법:
    pip install requests pymysql selenium webdriver-manager
    python kleague_crawler.py
    python kleague_crawler.py --month 05   # 특정 월 경기 일정만
"""

import re
import time
import requests
import pymysql
from datetime import datetime

from selenium import webdriver
from selenium.webdriver.chrome.service import Service
from selenium.webdriver.chrome.options import Options
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from webdriver_manager.chrome import ChromeDriverManager

#  DB 설정
DB_CONFIG = {
    "host":     "localhost",
    "port":     3306,
    "user":     "root",
    "password": "1234",
    "database": "ai_match",
    "charset":  "utf8mb4",
}

SEASON    = "2026"
LEAGUE_ID = "1"  # K리그1 고정

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    ),
    "Content-Type": "application/json;charset=UTF-8",
    "Referer":      "https://www.kleague.com/",
}

URL_TEAM_RANK = "https://www.kleague.com/record/teamRank.do"
URL_SCHEDULE  = "https://www.kleague.com/getScheduleList.do"


#  공통 유틸 

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


def get_recent_form(cur, team_id: int) -> str:
    """match 테이블에서 팀별 최근 5경기 W/D/L 문자열 반환"""
    sql = """
        SELECT
            CASE
                WHEN home_team_id = %s AND home_score > away_score THEN 'W'
                WHEN home_team_id = %s AND home_score < away_score THEN 'L'
                WHEN away_team_id = %s AND away_score > home_score THEN 'W'
                WHEN away_team_id = %s AND away_score < home_score THEN 'L'
                ELSE 'D'
            END AS result
        FROM `match`
        WHERE (home_team_id = %s OR away_team_id = %s)
          AND status = 'finished'
          AND sport_id = (SELECT sport_id FROM sport WHERE code = 'soccer')
        ORDER BY scheduled_at DESC
        LIMIT 5
    """
    cur.execute(sql, (team_id,) * 6)
    results = [r[0] for r in cur.fetchall()]
    results.reverse()
    return "".join(results)


def get_or_create_player(cur, team_map: dict, row: dict) -> int | None:
    team_id = team_map.get(row["team_name"])
    if not team_id:
        print(f"  [SKIP] 팀 없음: {row['team_name']}")
        return None

    external_id = row.get("external_id", row["name"])

    cur.execute(
        "SELECT player_id FROM player WHERE source='kleague' AND external_player_id=%s",
        (external_id,)
    )
    existing = cur.fetchone()
    if existing:
        return existing[0]

    cur.execute(
        "SELECT player_id FROM player WHERE name=%s AND team_id=%s AND source='kleague'",
        (row["name"], team_id)
    )
    existing = cur.fetchone()
    if existing:
        cur.execute(
            "UPDATE player SET external_player_id=%s WHERE player_id=%s",
            (external_id, existing[0])
        )
        return existing[0]

    cur.execute(
        "INSERT INTO player (team_id, source, external_player_id, name, position) "
        "VALUES (%s, 'kleague', %s, %s, %s)",
        (team_id, external_id, row["name"], row.get("position", ""))
    )
    print(f"  [NEW] 선수 등록: {row['name']} ({row['team_name']})")
    return cur.lastrowid


def get_or_create_season_stat(cur, player_id: int, season: str) -> int:
    cur.execute(
        "SELECT player_season_stat_id FROM player_season_stat WHERE player_id=%s AND season=%s",
        (player_id, season)
    )
    row = cur.fetchone()
    if row:
        return row[0]
    cur.execute(
        "INSERT INTO player_season_stat (player_id, season, games_played, updated_at) "
        "VALUES (%s, %s, 0, NOW())",
        (player_id, season)
    )
    return cur.lastrowid


#  1. 팀 순위 

def crawl_team_rank() -> list[dict]:
    params = {
        "leagueId":   LEAGUE_ID,
        "year":       SEASON,
        "stadium":    "all",
        "recordType": "rank",
    }
    res = requests.get(URL_TEAM_RANK, params=params, headers=HEADERS, timeout=10)
    res.raise_for_status()
    data = res.json()

    results = []
    for t in data.get("data", {}).get("teamRank", []):
        recent_raw = [t.get(f"game0{i}", "") for i in range(1, 7)]
        mapping    = {"승": "W", "무": "D", "패": "L"}
        form       = "".join(mapping.get(g, "") for g in recent_raw if g)
        recent_form = form[-5:] if len(form) >= 5 else form

        results.append({
            "team_name":      t["teamName"],
            "rank":           t["rank"],
            "wins":           t["winCnt"],
            "draws":          t["tieCnt"],
            "losses":         t["lossCnt"],
            "points_for":     t["gainGoal"],
            "points_against": t["lossGoal"],
            "win_rate":       round(t["winCnt"] / t["gameCount"], 3) if t["gameCount"] > 0 else 0,
            "recent_form":    recent_form,
        })
    return results


def save_team_rank(conn, team_map: dict, data: list[dict]):
    sql_select = """
        SELECT team_season_stat_id FROM team_season_stat
        WHERE team_id = %s AND sport_id = (SELECT sport_id FROM sport WHERE code = 'soccer')
          AND season = %s
    """
    sql_insert = """
        INSERT INTO team_season_stat
            (team_id, sport_id, season, wins, draws, losses,
             points_for, points_against, win_rate, `rank`, recent_form, updated_at)
        VALUES (%s, (SELECT sport_id FROM sport WHERE code='soccer'), %s,
                %s, %s, %s, %s, %s, %s, %s, %s, NOW())
    """
    sql_update = """
        UPDATE team_season_stat
        SET wins=%s, draws=%s, losses=%s, points_for=%s, points_against=%s,
            win_rate=%s, `rank`=%s, recent_form=%s, updated_at=NOW()
        WHERE team_season_stat_id = %s
    """
    saved = updated = skipped = 0
    with conn.cursor() as cur:
        for row in data:
            team_id = team_map.get(row["team_name"])
            if not team_id:
                print(f"  [SKIP] 팀 없음: {row['team_name']}")
                skipped += 1
                continue

            recent_form = get_recent_form(cur, team_id)
            cur.execute(sql_select, (team_id, SEASON))
            existing = cur.fetchone()

            if existing:
                cur.execute(sql_update, (
                    row["wins"], row["draws"], row["losses"],
                    row["points_for"], row["points_against"],
                    row["win_rate"], row["rank"], recent_form,
                    existing[0],
                ))
                updated += 1
            else:
                cur.execute(sql_insert, (
                    team_id, SEASON,
                    row["wins"], row["draws"], row["losses"],
                    row["points_for"], row["points_against"],
                    row["win_rate"], row["rank"], recent_form,
                ))
                saved += 1
    conn.commit()
    print(f"  팀 순위 저장 완료: 신규 {saved}건 / 업데이트 {updated}건 / 스킵 {skipped}건")


#  2. 경기 일정/결과 

def crawl_schedule(month: str = None) -> list[dict]:
    """month: '01'~'12'. None이면 당월."""
    if month is None:
        month = datetime.today().strftime("%m")

    payload = {
        "leagueId": LEAGUE_ID,
        "year":     SEASON,
        "month":    month,
        "teamId":   "",
        "ticketYn": "",
    }
    res = requests.post(URL_SCHEDULE, json=payload, headers=HEADERS, timeout=10)
    res.raise_for_status()
    game_list = res.json().get("data", {}).get("scheduleList", []) or []

    results = []
    for game in game_list:
        if not isinstance(game, dict):
            continue
        try:
            game_date = game.get("gameDate", "").replace(".", "-")
            game_time = game.get("gameTime", "18:00")
            if not game_date:
                continue

            home_name = game.get("homeTeamName", "").strip()
            away_name = game.get("awayTeamName", "").strip()
            if not home_name or not away_name:
                continue

            status_raw = str(game.get("gameStatus", "")).strip()
            if status_raw == "FE":
                status = "finished"
            elif status_raw == "RE":
                status = "in_progress"
            elif status_raw == "CA":
                status = "cancelled"
            else:
                status = "scheduled"

            results.append({
                "home_name":    home_name,
                "away_name":    away_name,
                "home_score":   int(game.get("homeGoal", 0) or 0),
                "away_score":   int(game.get("awayGoal", 0) or 0),
                "status":       status,
                "venue":        game.get("fieldName", "") or "",
                "scheduled_at": f"{game_date} {game_time}:00",
            })
        except Exception as e:
            print(f"  [WARN] 경기 파싱 오류: {e}")
    return results


def save_schedule(conn, team_map: dict, data: list[dict]):
    sql_select = """
        SELECT match_id, status FROM `match`
        WHERE sport_id = (SELECT sport_id FROM sport WHERE code = 'soccer')
          AND home_team_id = %s AND away_team_id = %s
          AND DATE(scheduled_at) = DATE(%s)
    """
    sql_insert = """
        INSERT INTO `match`
            (sport_id, home_team_id, away_team_id, scheduled_at,
             status, home_score, away_score, venue, season, created_at, updated_at)
        VALUES ((SELECT sport_id FROM sport WHERE code='soccer'),
                %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
    """
    sql_update = """
        UPDATE `match` SET status=%s, home_score=%s, away_score=%s, updated_at=NOW()
        WHERE match_id=%s
    """
    saved = updated = skipped = 0
    with conn.cursor() as cur:
        for row in data:
            home_id = team_map.get(row["home_name"])
            away_id = team_map.get(row["away_name"])
            if not home_id:
                print(f"  [SKIP] 홈팀 없음: {row['home_name']}")
                skipped += 1
                continue
            if not away_id:
                print(f"  [SKIP] 원정팀 없음: {row['away_name']}")
                skipped += 1
                continue

            cur.execute(sql_select, (home_id, away_id, row["scheduled_at"]))
            existing = cur.fetchone()

            if existing:
                match_id, cur_status = existing
                if cur_status == "finished":
                    continue
                cur.execute(sql_update, (row["status"], row["home_score"], row["away_score"], match_id))
                updated += 1
            else:
                cur.execute(sql_insert, (
                    home_id, away_id, row["scheduled_at"],
                    row["status"], row["home_score"], row["away_score"],
                    row["venue"], SEASON,
                ))
                saved += 1
    conn.commit()
    print(f"  경기 일정 저장 완료: 신규 {saved}건 / 업데이트 {updated}건 / 스킵 {skipped}건")


#  3. 선수 기록 (Selenium)

# 순회할 record type 목록 — 각 타입의 상위 30명을 수집해 전체 커버리지를 높임
# 17개 컬럼은 타입과 무관하게 동일(골·도움·경고 등)하며 정렬 기준만 바뀜
RECORD_TYPES = ["GOAL", "ASSIST", "GAMECNT", "WARN", "CLEAN"]


def _parse_row(row) -> dict | None:
    """tbody tr 한 행을 파싱해 선수 dict 반환. 파싱 불가 시 None."""
    cols = row.find_elements(By.TAG_NAME, "td")
    if len(cols) < 15:
        return None

    name      = cols[1].text.strip()
    team_name = cols[2].text.strip().split("\n")[-1].strip()
    if not name or not team_name:
        return None

    onclick = cols[1].get_attribute("onclick") or ""
    pid_match = re.search(r"playerId=(\d+)", onclick)
    external_id = pid_match.group(1) if pid_match else name

    def safe_int(text):
        try:
            return int(text.strip() or 0)
        except ValueError:
            return 0

    return {
        "name":         name,
        "team_name":    team_name,
        "external_id":  external_id,
        "games_played": safe_int(cols[14].text),
        "goals":        safe_int(cols[3].text),
        "assists":      safe_int(cols[4].text),
        "yellow_cards": safe_int(cols[11].text),
        "red_cards":    safe_int(cols[12].text),
        "clean_sheets": safe_int(cols[13].text),
    }


def crawl_players() -> list[dict]:
    """
    RECORD_TYPES 각각의 상위 30명을 수집한 뒤 external_id 기준으로 병합.
    동일 선수가 여러 타입에서 나오면 각 stat의 최댓값을 사용 (정렬 기준이 달라도
    모든 타입에서 전체 스탯이 표시되므로 값은 동일하나, 최댓값으로 안전하게 처리).
    """
    url = f"https://www.kleague.com/record/player.do?leagueId={LEAGUE_ID}&year={SEASON}"

    options = Options()
    options.add_argument("--headless")
    options.add_argument("--no-sandbox")
    options.add_argument("--disable-dev-shm-usage")
    options.add_argument("--window-size=1920,1080")
    options.add_argument(f"user-agent={HEADERS['User-Agent']}")
    driver = webdriver.Chrome(
        service=Service(ChromeDriverManager().install()), options=options
    )

    # external_id → 병합된 선수 dict
    merged: dict[str, dict] = {}

    try:
        driver.get(url)
        WebDriverWait(driver, 15).until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "table tbody tr"))
        )
        time.sleep(1)

        from selenium.webdriver.support.ui import Select as SeleniumSelect

        for rec_type in RECORD_TYPES:
            try:
                SeleniumSelect(driver.find_element(By.ID, "recordType")).select_by_value(rec_type)
                time.sleep(1.5)
            except Exception:
                print(f"  [WARN] recordType 전환 실패: {rec_type}")
                continue

            rows = driver.find_elements(By.CSS_SELECTOR, "table tbody tr")
            count = 0
            for row in rows:
                try:
                    p = _parse_row(row)
                    if not p:
                        continue
                    eid = p["external_id"]
                    if eid not in merged:
                        merged[eid] = p
                    else:
                        # 같은 선수 — 각 수치의 최댓값으로 갱신
                        m = merged[eid]
                        for k in ("games_played", "goals", "assists", "yellow_cards", "red_cards", "clean_sheets"):
                            m[k] = max(m[k], p[k])
                    count += 1
                except Exception as e:
                    print(f"  [WARN] 선수 파싱 오류 ({rec_type}): {e}")

            print(f"  {rec_type}: {count}명 수집")

    finally:
        driver.quit()

    result = list(merged.values())
    print(f"  중복 제거 후 총 {len(result)}명")
    return result


def save_players(conn, team_map: dict, data: list[dict]):
    sql_upsert = """
        INSERT INTO player_season_stat_soccer
            (player_season_stat_id, goals, assists, yellow_cards, red_cards, clean_sheets)
        VALUES (%s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE goals=%s, assists=%s, yellow_cards=%s, red_cards=%s, clean_sheets=%s
    """
    sql_update_base = "UPDATE player_season_stat SET games_played=%s, updated_at=NOW() WHERE player_season_stat_id=%s"
    saved = 0
    seen_stat_ids = []
    with conn.cursor() as cur:
        for row in data:
            player_id = get_or_create_player(cur, team_map, row)
            if not player_id:
                continue
            stat_id = get_or_create_season_stat(cur, player_id, SEASON)
            cur.execute(sql_update_base, (row["games_played"], stat_id))
            cur.execute(sql_upsert, (
                stat_id,
                row["goals"], row["assists"], row["yellow_cards"],
                row["red_cards"], row["clean_sheets"],
                row["goals"], row["assists"], row["yellow_cards"],
                row["red_cards"], row["clean_sheets"],
            ))
            seen_stat_ids.append(stat_id)
            saved += 1

        # 이번 크롤에 없는 선수 기록 삭제 (사이트 목록과 동기화)
        if seen_stat_ids:
            fmt = ",".join(["%s"] * len(seen_stat_ids))
            cur.execute(f"""
                DELETE FROM player_season_stat_soccer
                WHERE player_season_stat_id NOT IN ({fmt})
                  AND player_season_stat_id IN (
                      SELECT player_season_stat_id FROM player_season_stat WHERE season = %s
                  )
            """, seen_stat_ids + [SEASON])
            removed = cur.rowcount
            if removed:
                print(f"  목록 미등재 선수 기록 삭제: {removed}건")

    conn.commit()
    print(f"  선수 기록 저장 완료: {saved}건")


#  메인 

def main():
    import argparse
    parser = argparse.ArgumentParser(description="K리그 크롤러")
    parser.add_argument("--month", help="경기 일정 월 (01~12). 지정 시 일정만 실행")
    args = parser.parse_args()

    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] K리그 크롤링 시작")

    conn = get_db()
    try:
        team_map = load_team_map(conn)

        if args.month:
            print(f"\n▶ 경기 일정 크롤링 ({args.month}월)...")
            save_schedule(conn, team_map, crawl_schedule(month=args.month))
        else:
            print("\n▶ 팀 순위 크롤링...")
            save_team_rank(conn, team_map, crawl_team_rank())

            print(f"\n▶ 경기 일정 크롤링 ({datetime.today().strftime('%m')}월)...")
            save_schedule(conn, team_map, crawl_schedule())

            print("\n▶ 선수 기록 크롤링 (Selenium)...")
            save_players(conn, team_map, crawl_players())

    finally:
        conn.close()

    print(f"\n[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] 완료")


if __name__ == "__main__":
    main()