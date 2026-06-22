"""
LCK 크롤러
- 선수 기록 (player_season_stat_lol) ← 네이버 esports API

팀 순위는 lolesports API 실시간 조회로 전환되어 크롤링 불필요.

실행 방법:
    pip install requests pymysql
    python lck_crawler.py

※ league_id는 시즌마다 바뀌므로 하단 업데이트 필요
"""

import requests
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

SEASON = "2026"

#  네이버 esports API 설정
# 시즌마다 league_id가 바뀜 (현재: lck_2026)
NAVER_LEAGUE_ID  = "lck_2026"
NAVER_PLAYER_URL = f"https://esports-api.game.naver.com/service/v1/ranking/{NAVER_LEAGUE_ID}/player"

NAVER_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36",
    "Accept":     "application/json, text/plain, */*",
    "Origin":     "https://game.naver.com",
    "Referer":    f"https://game.naver.com/esports/League_of_Legends/record/lck/player/{NAVER_LEAGUE_ID}",
}


#  DB 연결
def get_db():
    config = DB_CONFIG.copy()
    config["password"] = config["password"].encode("utf-8")
    return pymysql.connect(**config)


#  선수 기록 크롤링 (네이버 esports API)
def crawl_player_stats() -> list[dict]:
    res = requests.get(NAVER_PLAYER_URL, headers=NAVER_HEADERS, timeout=10)
    res.raise_for_status()
    data = res.json()

    results = []
    for p in data.get("content", []):
        add = p.get("addInfo", {})
        nickname = p.get("player", {}).get("nickName", "")
        if not nickname:
            continue

        kills   = add.get("kills", 0) or 0
        deaths  = add.get("deaths", 0) or 0
        assists = add.get("assists", 0) or 0
        games   = add.get("competeSetCount", 0) or 0
        kda_raw = add.get("kda", 0) or 0
        kda     = round(kda_raw, 2) if deaths > 0 else None

        avg_kills   = round(kills   / games, 2) if games > 0 else 0
        avg_deaths  = round(deaths  / games, 2) if games > 0 else 0
        avg_assists = round(assists / games, 2) if games > 0 else 0

        results.append({
            "nickname":     nickname,
            "games_played": games,
            "kda":          kda,
            "avg_kills":    avg_kills,
            "avg_deaths":   avg_deaths,
            "avg_assists":  avg_assists,
            "win_rate":     round(p.get("winRate", 0), 3),
        })
    return results


#  DB 저장: 선수 기록
def save_player_stats(conn, data: list[dict]):
    sql_find_player = """
        SELECT player_id FROM player
        WHERE source = 'lol' AND external_player_id = %s
    """
    sql_find_stat = """
        SELECT player_season_stat_id FROM player_season_stat
        WHERE player_id = %s AND season = %s
    """
    sql_insert_stat = """
        INSERT INTO player_season_stat (player_id, season, games_played, updated_at)
        VALUES (%s, %s, %s, NOW())
    """
    sql_upsert_lol = """
        INSERT INTO player_season_stat_lol
            (player_season_stat_id, kda, avg_kills, avg_deaths, avg_assists, cs_per_min, win_rate)
        VALUES (%s, %s, %s, %s, %s, 0, %s)
        ON DUPLICATE KEY UPDATE
            kda=%s, avg_kills=%s, avg_deaths=%s, avg_assists=%s, win_rate=%s
    """
    sql_update_base = """
        UPDATE player_season_stat SET games_played=%s, updated_at=NOW()
        WHERE player_season_stat_id=%s
    """
    saved = skipped = 0
    with conn.cursor() as cur:
        for row in data:
            cur.execute(sql_find_player, (row["nickname"],))
            player = cur.fetchone()
            if not player:
                print(f"  [SKIP] 선수 없음: {row['nickname']}")
                skipped += 1
                continue
            player_id = player[0]

            cur.execute(sql_find_stat, (player_id, SEASON))
            stat = cur.fetchone()
            if stat:
                stat_id = stat[0]
                cur.execute(sql_update_base, (row["games_played"], stat_id))
            else:
                cur.execute(sql_insert_stat, (player_id, SEASON, row["games_played"]))
                stat_id = cur.lastrowid

            cur.execute(sql_upsert_lol, (
                stat_id,
                row["kda"], row["avg_kills"], row["avg_deaths"],
                row["avg_assists"], row["win_rate"],
                row["kda"], row["avg_kills"], row["avg_deaths"],
                row["avg_assists"], row["win_rate"],
            ))
            saved += 1
    conn.commit()
    print(f"  선수 기록 저장 완료: {saved}건 / 스킵 {skipped}건")


#  메인
def main():
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] LCK 선수 기록 크롤링 시작")

    conn = get_db()
    try:
        print("\n▶ LCK 선수 기록 크롤링 (네이버 esports API)...")
        player_data = crawl_player_stats()
        save_player_stats(conn, player_data)
    finally:
        conn.close()

    print(f"\n[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] 완료")


if __name__ == "__main__":
    main()