"""
KBO 크롤러
- 팀 순위 (team_season_stat)
- 타자 기록 (player_season_stat_baseball - 타자)
- 투수 기록 (player_season_stat_baseball - 투수)
- 경기 일정/결과 (match)

실행 방법:
    pip install requests beautifulsoup4 pymysql
    python kbo_crawler.py
    python kbo_crawler.py --date 20260608          # 특정 날짜
    python kbo_crawler.py --start 20260601 --end 20260630  # 날짜 범위
"""

import re
import requests
from bs4 import BeautifulSoup
import pymysql
from datetime import datetime, timedelta

#  DB 설정
DB_CONFIG = {
    "host":     "localhost",
    "port":     3306,
    "user":     "root",
    "password": "1234",
    "database": "ai_match",
    "charset":  "utf8mb4",
}

SEASON = "2026"

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/124.0.0.0 Safari/537.36"
    )
}

URL_TEAM_RANK    = "https://www.koreabaseball.com/Record/TeamRank/TeamRankDaily.aspx"
URL_HITTER_P1   = "https://www.koreabaseball.com/Record/Player/HitterBasic/Basic1.aspx"
URL_PITCHER_P1  = "https://www.koreabaseball.com/Record/Player/PitcherBasic/Basic1.aspx"
URL_SCHEDULE_API = "https://www.koreabaseball.com/ws/Schedule.asmx/GetScheduleList"

# ASP.NET 컨트롤 prefix (KBO 공통)
CTL = "ctl00$ctl00$ctl00$cphContents$cphContents$cphContents$"


#  공통 유틸

def get_soup(url: str) -> BeautifulSoup:
    res = requests.get(url, headers=HEADERS, timeout=10)
    res.raise_for_status()
    res.encoding = "utf-8"
    return BeautifulSoup(res.text, "html.parser")


def extract_all_form_fields(soup: BeautifulSoup) -> dict:
    """hidden input + select 현재 선택값을 모두 수집 (ASP.NET postback 전송용)"""
    fields = {}
    for inp in soup.select('input[type="hidden"]'):
        name = inp.get("name", "")
        if name:
            fields[name] = inp.get("value", "")
    for sel in soup.find_all("select"):
        name = sel.get("name", "")
        if not name:
            continue
        selected = sel.find("option", selected=True) or sel.find("option")
        fields[name] = selected.get("value", "") if selected else ""
    return fields


def get_all_player_pages(url: str, position_type: str) -> list[dict]:
    """ASP.NET 페이지네이션을 처리해 전체 선수 기록을 수집"""
    session = requests.Session()
    res = session.get(url, headers=HEADERS, timeout=10)
    res.raise_for_status()
    soup = BeautifulSoup(res.text, "html.parser")

    all_players = parse_players_from_table(soup, position_type)
    print(f"  1페이지: {len(all_players)}명")

    page = 2
    while True:
        if not soup.find("a", id=re.compile(f"ucPager_btnNo{page}$")):
            break

        form_data = extract_all_form_fields(soup)
        form_data["__EVENTTARGET"] = CTL + f"ucPager$btnNo{page}"
        form_data["__EVENTARGUMENT"] = ""

        res = session.post(url, data=form_data, headers={
            **HEADERS,
            "Content-Type": "application/x-www-form-urlencoded",
            "Referer": url,
        }, timeout=10)
        soup = BeautifulSoup(res.text, "html.parser")
        rows = parse_players_from_table(soup, position_type)
        if not rows:
            break
        all_players.extend(rows)
        print(f"  {page}페이지: {len(rows)}명")
        page += 1

    return all_players


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


def load_player_map(conn) -> dict:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT p.name, p.team_id, p.player_id FROM player p "
            "JOIN team t ON p.team_id = t.team_id "
            "JOIN sport s ON t.sport_id = s.sport_id "
            "WHERE s.code = 'baseball'"
        )
        return {(row[0], row[1]): row[2] for row in cur.fetchall()}


def load_player_id_map(conn) -> dict:
    """external_player_id(str) → name 맵 (야구 선수만)"""
    with conn.cursor() as cur:
        cur.execute(
            "SELECT p.external_player_id, p.name FROM player p "
            "JOIN team t ON p.team_id = t.team_id "
            "JOIN sport s ON t.sport_id = s.sport_id "
            "WHERE s.code = 'baseball' AND p.external_player_id IS NOT NULL"
        )
        return {str(row[0]): row[1] for row in cur.fetchall()}


def get_or_create_player(cur, team_map: dict, row: dict) -> int | None:
    team_id = team_map.get(row["team_name"])
    if not team_id:
        print(f"  [SKIP] 팀 없음: {row['team_name']}")
        return None

    ext_id = row.get("external_player_id")
    if not ext_id:
        print(f"  [SKIP] playerId 없음: {row['name']}")
        return None

    cur.execute(
        "SELECT player_id FROM player WHERE source=%s AND external_player_id=%s",
        ("kbo", ext_id)
    )
    existing = cur.fetchone()
    if existing:
        cur.execute(
            "UPDATE player SET team_id=%s, name=%s WHERE player_id=%s",
            (team_id, row["name"], existing[0])
        )
        return existing[0]

    position = "투수" if row["position_type"] == "pitcher" else "타자"
    cur.execute(
        "INSERT INTO player (team_id, source, external_player_id, name, position) "
        "VALUES (%s, 'kbo', %s, %s, %s)",
        (team_id, ext_id, row["name"], position)
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
          AND sport_id = (SELECT sport_id FROM sport WHERE code = 'baseball')
        ORDER BY scheduled_at DESC
        LIMIT 5
    """
    cur.execute(sql, (team_id,) * 6)
    results = [r[0] for r in cur.fetchall()]
    results.reverse()
    return "".join(results)


#  1. 팀 순위 

def crawl_team_rank() -> list[dict]:
    soup = get_soup(URL_TEAM_RANK)
    table = soup.find("table")
    if not table:
        return []

    headers = [th.get_text(strip=True) for th in table.select("thead th")]
    results = []
    for tr in table.select("tbody tr"):
        cols = [td.get_text(strip=True) for td in tr.find_all("td")]
        if len(cols) != len(headers):
            continue
        row = dict(zip(headers, cols))

        recent_raw = row.get("최근10경기", "")
        w = int(re.search(r"(\d+)승", recent_raw).group(1)) if re.search(r"(\d+)승", recent_raw) else 0
        d = int(re.search(r"(\d+)무", recent_raw).group(1)) if re.search(r"(\d+)무", recent_raw) else 0
        l = int(re.search(r"(\d+)패", recent_raw).group(1)) if re.search(r"(\d+)패", recent_raw) else 0
        form = ("W" * w + "D" * d + "L" * l)
        recent_form = form[-5:] if len(form) >= 5 else form

        results.append({
            "rank":        int(row.get("순위", 0)),
            "team_name":   row.get("팀명", "").strip(),
            "wins":        int(row.get("승", 0)),
            "losses":      int(row.get("패", 0)),
            "draws":       int(row.get("무", 0)),
            "win_rate":    float(row.get("승률", 0)),
            "recent_form": recent_form,
        })
    return results


def save_team_rank(conn, team_map: dict, data: list[dict]):
    sql_select = """
        SELECT team_season_stat_id FROM team_season_stat
        WHERE team_id = %s AND sport_id = (SELECT sport_id FROM sport WHERE code = 'baseball')
          AND season = %s
    """
    sql_insert = """
        INSERT INTO team_season_stat
            (team_id, sport_id, season, wins, draws, losses, win_rate, `rank`, recent_form,
             points_for, points_against, updated_at)
        VALUES (%s, (SELECT sport_id FROM sport WHERE code='baseball'), %s,
                %s, %s, %s, %s, %s, %s, 0, 0, NOW())
    """
    sql_update = """
        UPDATE team_season_stat
        SET wins=%s, draws=%s, losses=%s, win_rate=%s, `rank`=%s, recent_form=%s, updated_at=NOW()
        WHERE team_season_stat_id = %s
    """
    with conn.cursor() as cur:
        for row in data:
            team_id = team_map.get(row["team_name"])
            if not team_id:
                print(f"  [SKIP] 팀 없음: {row['team_name']}")
                continue

            recent_form = get_recent_form(cur, team_id)
            cur.execute(sql_select, (team_id, SEASON))
            existing = cur.fetchone()

            if existing:
                cur.execute(sql_update, (
                    row["wins"], row["draws"], row["losses"],
                    row["win_rate"], row["rank"], recent_form,
                    existing[0],
                ))
            else:
                cur.execute(sql_insert, (
                    team_id, SEASON,
                    row["wins"], row["draws"], row["losses"],
                    row["win_rate"], row["rank"], recent_form,
                ))
    conn.commit()
    print(f"  팀 순위 저장 완료: {len(data)}건")


#  2. 타자/투수 기록 

def parse_players_from_table(soup: BeautifulSoup, position_type: str) -> list[dict]:
    table = soup.find("table")
    if not table:
        return []

    headers = [th.get_text(strip=True) for th in table.select("thead th")]
    results = []

    for tr in table.select("tbody tr"):
        cells = tr.find_all("td")
        if len(cells) != len(headers):
            continue

        row = dict(zip(headers, [td.get_text(strip=True) for td in cells]))

        external_player_id = None
        for td in cells:
            a = td.find("a", href=True)
            if a and "playerId=" in a.get("href", ""):
                m = re.search(r"playerId=(\d+)", a["href"])
                if m:
                    external_player_id = m.group(1)
                break

        name      = row.get("선수명", "").strip()
        team_name = row.get("팀명", "").strip()
        if not name or not team_name:
            continue

        base = {
            "name":               name,
            "team_name":          team_name,
            "external_player_id": external_player_id,
            "position_type":      position_type,
            "games_played":       int(row.get("G", 0) or 0),
        }

        if position_type == "hitter":
            base.update({
                "batting_avg": float(row.get("AVG", 0) or 0),
                "hits":        int(row.get("H", 0) or 0),
                "home_runs":   int(row.get("HR", 0) or 0),
                "rbi":         int(row.get("RBI", 0) or 0),
            })
        else:
            base.update({
                "era":        float(row.get("ERA", 0) or 0),
                "wins":       int(row.get("W", 0) or 0),
                "losses":     int(row.get("L", 0) or 0),
                "strikeouts": int(row.get("SO", 0) or 0),
                "saves":      int(row.get("SV", 0) or 0),
                "holds":      int(row.get("HLD", 0) or 0),
            })

        results.append(base)
    return results


def save_hitters(conn, team_map: dict, data: list[dict]):
    sql_upsert = """
        INSERT INTO player_season_stat_baseball
            (player_season_stat_id, batting_avg, hits, home_runs, rbi)
        VALUES (%s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE batting_avg=%s, hits=%s, home_runs=%s, rbi=%s
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
                row["batting_avg"], row["hits"], row["home_runs"], row["rbi"],
                row["batting_avg"], row["hits"], row["home_runs"], row["rbi"],
            ))
            seen_stat_ids.append(stat_id)
            saved += 1

        # 이번 크롤에 없는 타자 기록 삭제 (사이트 목록과 동기화)
        # position 조건 제외: 로스터 크롤러가 등록한 선수는 '외야수'/'내야수'/'포수' 등
        # 세분화된 포지션을 가지므로 batting_avg 존재 여부로만 타자 기록을 식별한다
        if seen_stat_ids:
            fmt = ",".join(["%s"] * len(seen_stat_ids))
            cur.execute(f"""
                DELETE psb FROM player_season_stat_baseball psb
                JOIN player_season_stat ps ON psb.player_season_stat_id = ps.player_season_stat_id
                WHERE ps.season = %s
                  AND psb.batting_avg IS NOT NULL
                  AND psb.era IS NULL
                  AND psb.player_season_stat_id NOT IN ({fmt})
            """, [SEASON] + seen_stat_ids)
            removed = cur.rowcount
            if removed:
                print(f"  목록 미등재 타자 기록 삭제: {removed}건")

    conn.commit()
    print(f"  타자 기록 저장 완료: {saved}건")


def save_pitchers(conn, team_map: dict, data: list[dict]):
    sql_upsert = """
        INSERT INTO player_season_stat_baseball
            (player_season_stat_id, era, wins, losses, strikeouts, saves, holds)
        VALUES (%s, %s, %s, %s, %s, %s, %s)
        ON DUPLICATE KEY UPDATE era=%s, wins=%s, losses=%s, strikeouts=%s, saves=%s, holds=%s
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
                row["era"], row["wins"], row["losses"],
                row["strikeouts"], row["saves"], row["holds"],
                row["era"], row["wins"], row["losses"],
                row["strikeouts"], row["saves"], row["holds"],
            ))
            seen_stat_ids.append(stat_id)
            saved += 1

        # 이번 크롤에 없는 투수 기록 삭제 (사이트 목록과 동기화)
        if seen_stat_ids:
            fmt = ",".join(["%s"] * len(seen_stat_ids))
            cur.execute(f"""
                DELETE psb FROM player_season_stat_baseball psb
                JOIN player_season_stat ps ON psb.player_season_stat_id = ps.player_season_stat_id
                WHERE ps.season = %s
                  AND psb.era IS NOT NULL
                  AND psb.batting_avg IS NULL
                  AND psb.player_season_stat_id NOT IN ({fmt})
            """, [SEASON] + seen_stat_ids)
            removed = cur.rowcount
            if removed:
                print(f"  목록 미등재 투수 기록 삭제: {removed}건")

    conn.commit()
    print(f"  투수 기록 저장 완료: {saved}건")


#  3. 경기 일정/결과

URL_BOXSCORE_API  = "https://www.koreabaseball.com/ws/Schedule.asmx/GetBoxScore"
URL_GAMELIST_API  = "https://www.koreabaseball.com/ws/Main.asmx/GetKboGameList"


def crawl_starting_pitchers(date: str) -> dict:
    """
    GetKboGameList로 당일 선발 투수 반환.
    date: 'YYYYMMDD'
    반환: {(away_code, home_code): {"away_pitcher": str, "home_pitcher": str, "game_id": str}}
    """
    api_headers = {
        **HEADERS,
        "Referer": "https://www.koreabaseball.com/Schedule/GameCenter/Main.aspx",
        "X-Requested-With": "XMLHttpRequest",
        "Content-Type": "application/x-www-form-urlencoded; charset=UTF-8",
    }
    session = requests.Session()
    session.get("https://www.koreabaseball.com/", headers=api_headers, timeout=10)
    try:
        res = session.post(
            URL_GAMELIST_API,
            data={"leId": "1", "srId": "0,1,3,4,5,7", "date": date},
            headers=api_headers, timeout=10,
        )
        data = res.json()
    except Exception as e:
        print(f"  [WARN] GetKboGameList 실패 ({date}): {e}")
        return {}

    # KBO 팀코드 → 크롤러 팀명 매핑
    CODE_TO_NAME = {
        "KT": "KT", "OB": "두산", "SS": "삼성", "LG": "LG", "WO": "키움",
        "SK": "SSG", "NC": "NC", "LT": "롯데", "HT": "KIA", "HH": "한화",
    }

    result = {}
    for g in data.get("game", []):
        away_nm = (g.get("T_PIT_P_NM") or "").strip()
        home_nm = (g.get("B_PIT_P_NM") or "").strip()
        if not away_nm and not home_nm:
            continue
        away_team = CODE_TO_NAME.get(g.get("AWAY_ID", ""), g.get("AWAY_NM", ""))
        home_team = CODE_TO_NAME.get(g.get("HOME_ID", ""), g.get("HOME_NM", ""))
        key = (away_team, home_team)
        result[key] = {
            "away_pitcher": away_nm or None,
            "home_pitcher": home_nm or None,
            "game_id":      g.get("G_ID"),
        }
    return result


def crawl_pitcher(game_id: str, game_date: str, player_id_to_name: dict) -> dict:
    """
    GetBoxScore에서 승리·패전·세이브 투수 이름을 파싱해 반환.
    홈팀 투수 이름이 숫자(external_player_id)로 올 때는 player_id_to_name으로 변환.
    """
    api_headers = {
        **HEADERS,
        "Referer": (
            f"https://www.koreabaseball.com/Schedule/GameCenter/Review.aspx"
            f"?gameDate={game_date}&gameId={game_id}"
        ),
        "X-Requested-With": "XMLHttpRequest",
        "Content-Type":     "application/x-www-form-urlencoded; charset=UTF-8",
    }

    session = requests.Session()
    session.get(
        "https://www.koreabaseball.com/Schedule/GameCenter/Review.aspx",
        params={"gameDate": game_date, "gameId": game_id},
        headers=api_headers, timeout=10,
    )

    try:
        res = session.post(
            URL_BOXSCORE_API,
            data={"leId": "1", "srId": "0", "seasonId": SEASON, "gameId": game_id},
            headers=api_headers, timeout=10,
        )
        data = res.json()
    except Exception as e:
        print(f"  [WARN] GetBoxScore 실패 ({game_id}): {e}")
        return {}

    tables = data.get("tables", [])
    winning = losing = save = None

    for tbl in tables:
        hdr_texts = []
        for h in tbl.get("headers", []):
            for c in h.get("row", []):
                hdr_texts.append(re.sub(r"<[^>]+>", "", c.get("Text", "")).strip())

        if "등판" not in hdr_texts:
            continue

        result_idx = hdr_texts.index("결과") if "결과" in hdr_texts else 2

        for row in tbl.get("rows", []):
            cells = row.get("row", [])
            if len(cells) <= result_idx:
                continue

            name_raw = re.sub(r"<[^>]+>", "", cells[0].get("Text", "")).strip()
            result   = re.sub(r"<[^>]+>", "", cells[result_idx].get("Text", "")).strip()

            # 숫자 = external_player_id → 이름으로 변환
            name = player_id_to_name.get(name_raw, name_raw) if name_raw.isdigit() else name_raw

            if result == "승" and not winning:
                winning = name
            elif result == "패" and not losing:
                losing = name
            elif result == "세" and not save:
                save = name

    return {
        "winning_pitcher": winning,
        "losing_pitcher":  losing,
        "save_pitcher":    save,
    }


def crawl_schedule(date: str = None, month: str = None) -> list[dict]:
    """
    date:  'YYYYMMDD' 형식. 지정 시 해당 날짜만 반환.
    month: '01'~'12' 형식. 지정 시 해당 월 전체 반환.
    둘 다 None이면 당일.
    """
    if date:
        target_date = f"{date[:4]}-{date[4:6]}-{date[6:8]}"
        req_month   = date[4:6]
    elif month:
        target_date = None
        req_month   = month
    else:
        target_date = datetime.today().strftime("%Y-%m-%d")
        req_month   = datetime.today().strftime("%m")

    api_headers = {
        **HEADERS,
        "Referer":          "https://www.koreabaseball.com/Schedule/Schedule.aspx",
        "X-Requested-With": "XMLHttpRequest",
        "Content-Type":     "application/x-www-form-urlencoded; charset=UTF-8",
        "Accept":           "application/json, text/javascript, */*; q=0.01",
    }

    session = requests.Session()
    session.get("https://www.koreabaseball.com/Schedule/Schedule.aspx",
                headers=api_headers, timeout=10)

    payload = {
        "leId":      "1",
        "srIdList":  "0,9,6",
        "seasonId":  SEASON,
        "gameMonth": req_month,
        "teamId":    "",
    }
    res = session.post(URL_SCHEDULE_API, data=payload, headers=api_headers, timeout=10)
    res.raise_for_status()

    try:
        data = res.json()
    except Exception:
        print(f"  [WARN] JSON 파싱 실패: {date or month}")
        return []

    results = []
    current_date = ""

    for row_obj in data.get("rows", []):
        cells     = row_obj.get("row", [])
        raw_texts = [c.get("Text", "") for c in cells]
        texts     = [re.sub(r"<[^>]+>", "", t).strip() for t in raw_texts]
        if not texts:
            continue

        date_match = re.search(r"(\d{2})\.(\d{2})", texts[0])
        if date_match:
            current_date = f"{SEASON}-{date_match.group(1)}-{date_match.group(2)}"
            time_idx, game_idx, venue_idx = 1, 2, 7
        else:
            time_idx, game_idx, venue_idx = 0, 1, 6

        if not current_date:
            continue
        if target_date and current_date != target_date:
            continue
        if len(texts) <= game_idx:
            continue

        game_time = texts[time_idx] if re.search(r"\d{2}:\d{2}", texts[time_idx]) else "18:30"
        game_text = texts[game_idx]

        # KBO는 경기 시작 전에도 점수 칸에 "0vs0" 플레이스홀더를 동일한 마크업으로 렌더링하므로
        # 숫자vs숫자 패턴만으로는 종료 여부를 판단할 수 없음.
        # 리뷰/하이라이트 링크(점수 칸 바로 다음 칸)는 경기가 실제로 종료된 뒤에만 채워지므로
        # 이 링크의 존재 여부로 종료 여부를 판단한다.
        review_idx      = game_idx + 1
        has_review_link = review_idx < len(raw_texts) and "section=REVIEW" in raw_texts[review_idx]

        # review 링크에서 gameId 추출 (e.g., 20260613LTLG0)
        game_id = None
        if has_review_link:
            m = re.search(r"gameId=([\w]+)", raw_texts[review_idx])
            if m:
                game_id = m.group(1)

        score_match    = re.search(r"(.+?)(\d+)vs(\d+)(.+)", game_text)
        no_score_match = re.search(r"(.+?)vs(.+)", game_text)

        if score_match:
            away_name  = score_match.group(1).strip()
            away_score = int(score_match.group(2))
            home_score = int(score_match.group(3))
            home_name  = score_match.group(4).strip()
            if has_review_link:
                status = "finished"
            else:
                # 리뷰 링크가 없으면 아직 종료되지 않은 경기 — 시작 시각이 지났으면 진행중, 아니면 예정
                try:
                    game_dt = datetime.strptime(f"{current_date} {game_time}", "%Y-%m-%d %H:%M")
                except ValueError:
                    game_dt = None
                if game_dt and datetime.now() >= game_dt:
                    status = "in_progress"
                else:
                    status = "scheduled"
                    away_score = home_score = 0  # 시작 전 플레이스홀더 점수는 무시
        elif no_score_match:
            away_name  = no_score_match.group(1).strip()
            home_name  = no_score_match.group(2).strip()
            away_score = home_score = 0
            status     = "scheduled"
        else:
            continue

        if not away_name or not home_name:
            continue

        full_text = " ".join(texts)
        if "취소" in full_text or "우천" in full_text:
            status = "cancelled"

        results.append({
            "away_name":    away_name,
            "home_name":    home_name,
            "away_score":   away_score,
            "home_score":   home_score,
            "venue":        texts[venue_idx] if len(texts) > venue_idx else "",
            "status":       status,
            "scheduled_at": f"{current_date} {game_time}:00",
            "game_id":      game_id,
            "game_date":    current_date.replace("-", ""),
        })

    return results


def save_schedule(conn, team_map: dict, data: list[dict], player_id_to_name: dict = None,
                  starting_pitcher_map: dict = None):
    if player_id_to_name is None:
        player_id_to_name = {}
    if starting_pitcher_map is None:
        starting_pitcher_map = {}

    sql_select = """
        SELECT match_id, status, winning_pitcher FROM `match`
        WHERE sport_id = (SELECT sport_id FROM sport WHERE code = 'baseball')
          AND home_team_id = %s AND away_team_id = %s
          AND DATE(scheduled_at) = DATE(%s)
    """
    sql_insert = """
        INSERT INTO `match`
            (sport_id, home_team_id, away_team_id, scheduled_at,
             status, home_score, away_score, venue, season, created_at, updated_at)
        VALUES ((SELECT sport_id FROM sport WHERE code='baseball'),
                %s, %s, %s, %s, %s, %s, %s, %s, NOW(), NOW())
    """
    sql_update = """
        UPDATE `match` SET status=%s, home_score=%s, away_score=%s, updated_at=NOW()
        WHERE match_id=%s
    """
    sql_update_pitcher = """
        UPDATE `match`
        SET winning_pitcher=%s, losing_pitcher=%s, current_pitcher=%s, updated_at=NOW()
        WHERE match_id=%s
    """
    sql_update_starting = """
        UPDATE `match`
        SET starting_pitcher_away=%s, starting_pitcher_home=%s, updated_at=NOW()
        WHERE match_id=%s
    """
    saved = updated = skipped = pitcher_updated = starting_updated = 0
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
                match_id, cur_status, cur_winning_pitcher = existing
                if cur_status == "finished":
                    # 투수 데이터가 비어있고 game_id가 있으면 채우기
                    if not cur_winning_pitcher and row.get("game_id"):
                        pit = crawl_pitcher(row["game_id"], row["game_date"], player_id_to_name)
                        if pit.get("winning_pitcher") or pit.get("losing_pitcher"):
                            cur.execute(sql_update_pitcher, (
                                pit.get("winning_pitcher"),
                                pit.get("losing_pitcher"),
                                None,
                                match_id,
                            ))
                            pitcher_updated += 1
                    continue

                cur.execute(sql_update, (row["status"], row["home_score"], row["away_score"], match_id))
                updated += 1

                # 방금 finished 된 경우 투수 데이터 저장
                if row["status"] == "finished" and row.get("game_id"):
                    pit = crawl_pitcher(row["game_id"], row["game_date"], player_id_to_name)
                    cur.execute(sql_update_pitcher, (
                        pit.get("winning_pitcher"),
                        pit.get("losing_pitcher"),
                        None,
                        match_id,
                    ))
                    pitcher_updated += 1

                # 선발 투수 업데이트 (scheduled/in_progress)
                if row["status"] in ("scheduled", "in_progress"):
                    sp = starting_pitcher_map.get((row["away_name"], row["home_name"]))
                    if sp:
                        cur.execute(sql_update_starting, (
                            sp.get("away_pitcher"), sp.get("home_pitcher"), match_id,
                        ))
                        starting_updated += 1
            else:
                cur.execute(sql_insert, (
                    home_id, away_id, row["scheduled_at"],
                    row["status"], row["home_score"], row["away_score"],
                    row["venue"], SEASON,
                ))
                match_id = cur.lastrowid
                saved += 1

                # 새로 INSERT하면서 이미 finished인 경우 (과거 날짜 크롤링 등)
                if row["status"] == "finished" and row.get("game_id"):
                    pit = crawl_pitcher(row["game_id"], row["game_date"], player_id_to_name)
                    cur.execute(sql_update_pitcher, (
                        pit.get("winning_pitcher"),
                        pit.get("losing_pitcher"),
                        None,
                        match_id,
                    ))
                    pitcher_updated += 1

                # 선발 투수 저장 (새 경기)
                if row["status"] in ("scheduled", "in_progress"):
                    sp = starting_pitcher_map.get((row["away_name"], row["home_name"]))
                    if sp:
                        cur.execute(sql_update_starting, (
                            sp.get("away_pitcher"), sp.get("home_pitcher"), match_id,
                        ))
                        starting_updated += 1

    conn.commit()
    print(f"  경기 일정 저장 완료: 신규 {saved}건 / 업데이트 {updated}건 / 투수 {pitcher_updated}건 / 선발 {starting_updated}건 / 스킵 {skipped}건")


#  메인 

def main():
    import argparse
    parser = argparse.ArgumentParser(description="KBO 크롤러")
    parser.add_argument("--date",  help="특정 날짜 (YYYYMMDD)")
    parser.add_argument("--start", help="시작 날짜 (YYYYMMDD)")
    parser.add_argument("--end",   help="종료 날짜 (YYYYMMDD)")
    args = parser.parse_args()

    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] KBO 크롤링 시작")

    conn = get_db()
    try:
        team_map         = load_team_map(conn)
        player_id_to_name = load_player_id_map(conn)

        if args.date:
            print(f"\n▶ 경기 일정 크롤링 ({args.date})...")
            data = crawl_schedule(date=args.date)
            sp_map = crawl_starting_pitchers(args.date)
            save_schedule(conn, team_map, data, player_id_to_name, sp_map) if data else print("  경기 없음")

        elif args.start and args.end:
            start_dt  = datetime.strptime(args.start, "%Y%m%d")
            end_dt    = datetime.strptime(args.end,   "%Y%m%d")
            start_str = f"{args.start[:4]}-{args.start[4:6]}-{args.start[6:8]}"
            end_str   = f"{args.end[:4]}-{args.end[4:6]}-{args.end[6:8]}"
            print(f"\n▶ 경기 일정 크롤링 ({args.start} ~ {args.end})...")

            months = set()
            cur_dt = start_dt
            while cur_dt <= end_dt:
                months.add(cur_dt.strftime("%m"))
                cur_dt += timedelta(days=1)

            for m in sorted(months):
                all_data = crawl_schedule(month=m)
                filtered = [d for d in all_data if start_str <= d["scheduled_at"][:10] <= end_str]
                if filtered:
                    save_schedule(conn, team_map, filtered, player_id_to_name)
            print("  전체 처리 완료")

        else:
            today_str = datetime.today().strftime("%Y%m%d")
            print("\n▶ 경기 일정 크롤링 (당일)...")
            data = crawl_schedule()
            sp_map = crawl_starting_pitchers(today_str)
            save_schedule(conn, team_map, data, player_id_to_name, sp_map) if data else print("  경기 없음")

            print("\n▶ 팀 순위 크롤링...")
            save_team_rank(conn, team_map, crawl_team_rank())

            print("\n▶ 타자 기록 크롤링...")
            save_hitters(conn, team_map, get_all_player_pages(URL_HITTER_P1, "hitter"))

            print("\n▶ 투수 기록 크롤링...")
            save_pitchers(conn, team_map, get_all_player_pages(URL_PITCHER_P1, "pitcher"))

    finally:
        conn.close()

    print(f"\n[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] 완료")


if __name__ == "__main__":
    main()