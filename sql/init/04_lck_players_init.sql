-- ================================================
-- AI.MATCH LCK 선수 초기 데이터 (2026 스플릿 2 기준)
-- external_player_id = 네이버 esports API nickName 기준
-- name = 대표 닉네임으로 저장 (UI 표시용), 주석에 실명 표기
-- 네이버 esports API(lck_2026) 출전 기록 기준 자동 생성 — 1군 10팀 55명 (교체 선수 포함)
-- ================================================

INSERT INTO player (team_id, source, external_player_id, name, position)
SELECT t.team_id, 'lol', p.nickname, p.name, p.position
FROM team t
JOIN (
    -- GEN (젠지)
    SELECT 'GEN' AS team_code, 'Kiin' AS name, 'TOP' AS position, 'Kiin' AS nickname UNION ALL -- 김기인
    SELECT 'GEN', 'Canyon', 'JGL', 'Canyon' UNION ALL -- 김건부
    SELECT 'GEN', 'Chovy', 'MID', 'Chovy' UNION ALL -- 정지훈
    SELECT 'GEN', 'Ruler', 'ADC', 'Ruler' UNION ALL -- 박재혁
    SELECT 'GEN', 'Duro', 'SUP', 'Duro' UNION ALL -- 주민규
    -- HLE (한화생명)
    SELECT 'HLE', 'Zeus', 'TOP', 'Zeus' UNION ALL -- 최우제
    SELECT 'HLE', 'Kanavi', 'JGL', 'Kanavi' UNION ALL -- 서진혁
    SELECT 'HLE', 'Zeka', 'MID', 'Zeka' UNION ALL -- 김건우
    SELECT 'HLE', 'Gumayusi', 'ADC', 'Gumayusi' UNION ALL -- 이민형
    SELECT 'HLE', 'Delight', 'SUP', 'Delight' UNION ALL -- 유환중
    -- T1 (T1)
    SELECT 'T1', 'Doran', 'TOP', 'Doran' UNION ALL -- 최현준
    SELECT 'T1', 'Oner', 'JGL', 'Oner' UNION ALL -- 문현준
    SELECT 'T1', 'Faker', 'MID', 'Faker' UNION ALL -- 이상혁
    SELECT 'T1', 'Peyz', 'ADC', 'Peyz' UNION ALL -- 김수환
    SELECT 'T1', 'Keria', 'SUP', 'Keria' UNION ALL -- 류민석
    -- KT (KT)
    SELECT 'KT', 'PerfecT', 'TOP', 'PerfecT' UNION ALL -- 이승민
    SELECT 'KT', 'Cuzz', 'JGL', 'Cuzz' UNION ALL -- 문우찬
    SELECT 'KT', 'Bdd', 'MID', 'Bdd' UNION ALL -- 곽보성
    SELECT 'KT', 'Aiming', 'ADC', 'Aiming' UNION ALL -- 김하람
    SELECT 'KT', 'Effort', 'SUP', 'Effort' UNION ALL -- 이상호
    -- DK (DK)
    SELECT 'DK', 'Siwoo', 'TOP', 'Siwoo' UNION ALL -- 전시우
    SELECT 'DK', 'Lucid', 'JGL', 'Lucid' UNION ALL -- 최용혁
    SELECT 'DK', 'Sharvel', 'JGL', 'Sharvel' UNION ALL -- 김단우
    SELECT 'DK', 'ShowMaker', 'MID', 'ShowMaker' UNION ALL -- 허수
    SELECT 'DK', 'Smash', 'ADC', 'Smash' UNION ALL -- 신금재
    SELECT 'DK', 'Career', 'SUP', 'Career' UNION ALL -- 오형석
    -- NS (농심)
    SELECT 'NS', 'Kingen', 'TOP', 'Kingen' UNION ALL -- 황성훈
    SELECT 'NS', 'Sponge', 'JGL', 'Sponge' UNION ALL -- 배영준
    SELECT 'NS', 'Scout', 'MID', 'Scout' UNION ALL -- 이예찬
    SELECT 'NS', 'Diable', 'ADC', 'Diable' UNION ALL -- 남대근
    SELECT 'NS', 'Lehends', 'SUP', 'Lehends' UNION ALL -- 손시우
    -- BFX (BFX)
    SELECT 'BFX', 'Clear', 'TOP', 'Clear' UNION ALL -- 송현민
    SELECT 'BFX', 'Raptor', 'JGL', 'Raptor' UNION ALL -- 전어진
    SELECT 'BFX', 'VicLa', 'MID', 'VicLa' UNION ALL -- 이대광
    SELECT 'BFX', 'Daystar', 'MID', 'Daystar' UNION ALL -- 유지명
    SELECT 'BFX', 'Taeyoon', 'ADC', 'Taeyoon' UNION ALL -- 김태윤
    SELECT 'BFX', 'Kellin', 'SUP', 'Kellin' UNION ALL -- 김형규
    -- KRX (KRX)
    SELECT 'KRX', 'Rich', 'TOP', 'Rich' UNION ALL -- 이재원
    SELECT 'KRX', 'Willer', 'JGL', 'Willer' UNION ALL -- 김정현
    SELECT 'KRX', 'Ucal', 'MID', 'Ucal' UNION ALL -- 손우현
    SELECT 'KRX', 'LazyFeel', 'ADC', 'LazyFeel' UNION ALL -- Tran Bao Minh
    SELECT 'KRX', 'Jiwoo', 'ADC', 'Jiwoo' UNION ALL -- 정지우
    SELECT 'KRX', 'Andil', 'SUP', 'Andil' UNION ALL -- 문관빈
    -- BRO (한진 브리온)
    SELECT 'BRO', 'Casting', 'TOP', 'Casting' UNION ALL -- 신민제
    SELECT 'BRO', 'GIDEON', 'JGL', 'GIDEON' UNION ALL -- 김민성
    SELECT 'BRO', 'Roamer', 'MID', 'Roamer' UNION ALL -- 조우진
    SELECT 'BRO', 'Loki', 'MID', 'Loki' UNION ALL -- 이상민
    SELECT 'BRO', 'Teddy', 'ADC', 'Teddy' UNION ALL -- 박진성
    SELECT 'BRO', 'Namgung', 'SUP', 'Namgung' UNION ALL -- 남궁성훈
    -- DNS (DNS)
    SELECT 'DNS', 'DuDu', 'TOP', 'DuDu' UNION ALL -- 이동주
    SELECT 'DNS', 'Pyosik', 'JGL', 'Pyosik' UNION ALL -- 홍창현
    SELECT 'DNS', 'Clozer', 'MID', 'Clozer' UNION ALL -- 이주현
    SELECT 'DNS', 'deokdam', 'ADC', 'deokdam' UNION ALL -- 서대길
    SELECT 'DNS', 'Peter', 'SUP', 'Peter' UNION ALL -- 정윤수
    SELECT 'DNS', 'Life', 'SUP', 'Life' -- 김정민
) p ON t.name = p.team_code
JOIN sport s ON t.sport_id = s.sport_id AND s.code = 'lol'
ON DUPLICATE KEY UPDATE
    team_id  = VALUES(team_id),
    name     = VALUES(name),
    position = VALUES(position);
