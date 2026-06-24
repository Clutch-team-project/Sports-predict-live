-- 내 미정산 예측 확인 (로그인 유저 ID 확인 먼저)
SELECT p.prediction_id, p.sport_code, p.predicted_result, p.is_correct, u.login_id
FROM prediction p
JOIN users u ON p.user_id = u.user_id
WHERE p.is_correct IS NULL
ORDER BY p.created_at DESC
LIMIT 10;

-- 적중으로 정산 (알림: "승부예측 적중! +100P")
UPDATE prediction
SET is_correct    = true,
    actual_result = predicted_result,
    points_earned = 100
WHERE prediction_id = [위에서 확인한 prediction_id];

-- 틀림으로 정산 (알림: "아쉽게도 틀렸습니다")
UPDATE prediction
SET is_correct    = false,
    actual_result = CASE predicted_result
                        WHEN 'HOME_WIN' THEN 'AWAY_WIN'
                        WHEN 'AWAY_WIN' THEN 'HOME_WIN'
                        ELSE 'HOME_WIN'
                    END,
    points_earned = 0
WHERE prediction_id = [위에서 확인한 prediction_id];

-- 예측 원복
UPDATE prediction SET is_correct = NULL, actual_result = NULL, points_earned = 0
WHERE prediction_id = [id];