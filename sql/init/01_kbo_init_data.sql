-- ================================================
-- AI.MATCH KBO 초기 데이터
-- 실행 순서: sport → team
-- ================================================

-- 1. 종목 데이터
INSERT INTO sport (name, code) VALUES
    ('야구', 'baseball'),
    ('축구', 'soccer'),
    ('리그 오브 레전드', 'lol')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2. KBO 10개 구단
INSERT INTO team (sport_id, name, location, founded_year, emblem_url)
SELECT s.sport_id, t.name, t.location, t.founded_year, NULL
FROM sport s
JOIN (
    SELECT 'LG'   AS name, '서울'  AS location, 1982 AS founded_year UNION ALL
    SELECT 'KT',            '수원',              2013               UNION ALL
    SELECT '삼성',           '대구',              1982               UNION ALL
    SELECT 'KIA',           '광주',              1982               UNION ALL
    SELECT '한화',           '대전',              1986               UNION ALL
    SELECT '두산',           '서울',              1982               UNION ALL
    SELECT 'NC',            '창원',              2011               UNION ALL
    SELECT 'SSG',           '인천',              2000               UNION ALL
    SELECT '롯데',           '부산',              1975               UNION ALL
    SELECT '키움',           '서울',              2008
) t ON 1=1
WHERE s.code = 'baseball'
ON DUPLICATE KEY UPDATE
    location     = VALUES(location),
    founded_year = VALUES(founded_year);