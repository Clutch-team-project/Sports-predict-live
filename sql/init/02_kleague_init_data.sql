-- ================================================
-- AI.MATCH K리그 초기 데이터
-- 실행 순서: sport 테이블에 soccer가 있어야 함
--           (kbo_init_data.sql 먼저 실행했다면 이미 있음)
-- ※ 팀명을 K리그 API 응답의 teamName(축약명)과 일치시켜 저장
-- ================================================

-- K리그1 12개 구단
INSERT INTO team (sport_id, name, location, founded_year, emblem_url)
SELECT s.sport_id, t.name, t.location, t.founded_year, NULL
FROM sport s
JOIN (
    SELECT '서울'  AS name, '서울'  AS location, 1983 AS founded_year UNION ALL
    SELECT '울산',           '울산',              1983               UNION ALL
    SELECT '전북',           '전주',              1994               UNION ALL
    SELECT '강원',           '강원',              2009               UNION ALL
    SELECT '포항',           '포항',              1973               UNION ALL
    SELECT '인천',           '인천',              2003               UNION ALL
    SELECT '안양',           '안양',              2013               UNION ALL
    SELECT '제주',           '제주',              1982               UNION ALL
    SELECT '부천',           '부천',              1995               UNION ALL
    SELECT '대전',           '대전',              1997               UNION ALL
    SELECT '김천',           '김천',              2020               UNION ALL
    SELECT '광주',           '광주',              2010
) t ON 1=1
WHERE s.code = 'soccer'
ON DUPLICATE KEY UPDATE
    location     = VALUES(location),
    founded_year = VALUES(founded_year);