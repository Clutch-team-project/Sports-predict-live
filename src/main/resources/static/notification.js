(function () {
    if (!('Notification' in window)) return;

    const BASE_URL         = 'http://localhost:8080';
    const SPORT_EMOJI      = { baseball: '⚾', soccer: '⚽', lol: '🎮' };
    const ALERT_BEFORE_MIN = 10; // 경기 시작 몇 분 전 알림

    const notifiedMatches = new Set(); // 이미 알림 보낸 경기 키
    const notifiedPreds   = new Set(); // 이미 알림 보낸 예측 ID
    const predSnapshot    = {};        // predictionId → isCorrect (이전 상태)

    function notify(title, body) {
        if (Notification.permission !== 'granted') return;
        new Notification(title, { body, icon: '/favicon.ico' });
    }

    // ── 경기 시작 N분 전 알림 ────────────────────────────────────
    async function checkMatchStart() {
        const today   = new Date();
        const dateStr = `${today.getFullYear()}${String(today.getMonth()+1).padStart(2,'0')}${String(today.getDate()).padStart(2,'0')}`;

        try {
            const [bbRes, scRes, lolRes] = await Promise.all([
                fetch(`${BASE_URL}/api/schedule/baseball?date=${dateStr}`),
                fetch(`${BASE_URL}/api/schedule/soccer?date=${dateStr}`),
                fetch(`${BASE_URL}/api/schedule/lol?date=${dateStr}`)
            ]);
            const all = [];
            if (bbRes.ok)  { (await bbRes.json()).forEach(m  => all.push({ ...m,  sport: 'baseball' })); }
            if (scRes.ok)  { (await scRes.json()).forEach(m  => all.push({ ...m,  sport: 'soccer'   })); }
            if (lolRes.ok) { (await lolRes.json()).forEach(m => all.push({ ...m,  sport: 'lol'      })); }

            all.filter(m => m.status === 'scheduled').forEach(m => {
                const key = `${m.sport}-${m.matchId}`;
                if (notifiedMatches.has(key)) return;

                // scheduledAt: "HH:mm" 형식
                if (!m.scheduledAt || !m.scheduledAt.includes(':')) return;
                const [h, min] = m.scheduledAt.split(':').map(Number);
                const matchTime = new Date(today);
                matchTime.setHours(h, min, 0, 0);

                const diffMin = (matchTime - today) / 60000;
                if (diffMin > 0 && diffMin <= ALERT_BEFORE_MIN) {
                    notify(
                        `${SPORT_EMOJI[m.sport]} 경기 시작 ${ALERT_BEFORE_MIN}분 전!`,
                        `${m.awayTeamName} vs ${m.homeTeamName}`
                    );
                    notifiedMatches.add(key);
                }
            });
        } catch (_) {}
    }

    // ── 승부예측 결과 알림 (로그인 필요) ──────────────────────────
    async function checkPredResults() {
        try {
            const res = await authFetch(`${BASE_URL}/api/predictions/me`);
            if (!res.ok) return;
            const preds = await res.json();

            preds.forEach(p => {
                const key        = String(p.predictionId);
                const nowCorrect = p.isCorrect ?? null;
                const hasPrev    = Object.prototype.hasOwnProperty.call(predSnapshot, key);

                // 이전에 null(미정산)이었다가 이번에 결과가 나온 경우만 알림
                if (hasPrev && predSnapshot[key] === null && nowCorrect !== null && !notifiedPreds.has(key)) {
                    const emoji = SPORT_EMOJI[p.sportCode] || '🏆';
                    notify(
                        nowCorrect ? `${emoji} 승부예측 적중!` : `${emoji} 승부예측 결과`,
                        nowCorrect ? '+100P 획득했습니다 🎯'    : '아쉽게도 틀렸습니다'
                    );
                    notifiedPreds.add(key);
                }

                predSnapshot[key] = nowCorrect;
            });
        } catch (_) {}
    }

    // ── 초기화 ─────────────────────────────────────────────────────
    function init() {
        // 알림 권한 요청 (한 번만, 기본 상태일 때)
        if (Notification.permission === 'default') {
            Notification.requestPermission();
        }

        // 경기 시작 알림: 로그인 여부 무관
        checkMatchStart();
        setInterval(checkMatchStart, 60_000);

        // 예측 결과 알림: 로그인 필요
        if (localStorage.getItem('accessToken')) {
            checkPredResults();
            setInterval(checkPredResults, 120_000);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
