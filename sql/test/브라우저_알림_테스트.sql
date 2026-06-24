-- 현재 상태 확인
SELECT match_id, home_team_id, away_team_id, scheduled_at, status
FROM `match`
WHERE status = 'scheduled'
  AND DATE(scheduled_at) = CURDATE()
LIMIT 5;

-- 9분 후로 변경
UPDATE `match`
SET scheduled_at = DATE_ADD(NOW(), INTERVAL 9 MINUTE)
WHERE match_id = [위에서 확인한 match_id];

-- 원복
UPDATE `match`
SET scheduled_at = [원래 시간]
WHERE match_id = [match_id];

-- 경기 시간 원복
UPDATE match SET scheduled_at = [원래 시간] WHERE match_id = [id];