-- ================================================
-- AI.MATCH LCK 초기 데이터
-- ※ LCK 팀 순위(team_season_stat)는 lolesports API 실시간 조회로 제공되므로
--   이 파일은 선수(player) FK 참조를 위한 팀(team) 데이터만 입력합니다.
--
-- 팀명(name)은 lolesports API 응답의 code 값으로 저장
-- (예: "HLE", "T1", "GEN" ...)
-- ================================================

-- LCK 10개 팀 (2026 스플릿 2 기준)
INSERT INTO team (sport_id, name, location, founded_year, emblem_url)
SELECT s.sport_id, t.name, t.location, t.founded_year, NULL
FROM sport s
JOIN (
    SELECT 'HLE' AS name, '서울' AS location, 2012 AS founded_year UNION ALL
    SELECT 'T1',           '서울',             2003               UNION ALL
    SELECT 'GEN',          '서울',             2017               UNION ALL
    SELECT 'KT',           '수원',             2012               UNION ALL
    SELECT 'DK',           '서울',             2017               UNION ALL
    SELECT 'BRO',          '서울',             2019               UNION ALL
    SELECT 'BFX',          '부산',             2020               UNION ALL
    SELECT 'KRX',          '서울',             2017               UNION ALL
    SELECT 'NS',           '서울',             2019               UNION ALL
    SELECT 'DNS',          '서울',             2014
) t ON 1=1
WHERE s.code = 'lol'
ON DUPLICATE KEY UPDATE
    location     = VALUES(location),
    founded_year = VALUES(founded_year);