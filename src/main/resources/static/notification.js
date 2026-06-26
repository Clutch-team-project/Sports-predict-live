(function () {
    if (!('Notification' in window)) return;

    const BASE_URL         = window.location.origin;
    const SPORT_EMOJI      = { baseball: '⚾', soccer: '⚽', lol: '🎮' };
    const ALERT_BEFORE_MIN = 10; // 경기 시작 몇 분 전 알림

    // localStorage로 유지해 새로고침 후 중복 알림 방지
    const STORAGE_KEY_MATCHES = 'notifiedMatches';
    const STORAGE_KEY_PREDS   = 'notifiedPreds';

    function loadSet(key) {
        try { return new Set(JSON.parse(localStorage.getItem(key)) || []); } catch { return new Set(); }
    }
    function saveSet(key, set) {
        try { localStorage.setItem(key, JSON.stringify([...set])); } catch {}
    }

    const notifiedMatches = loadSet(STORAGE_KEY_MATCHES);
    const notifiedPreds   = loadSet(STORAGE_KEY_PREDS);
    const predSnapshot    = {};        // predictionId → isCorrect (이전 상태)

    // 하루가 바뀌면 경기 알림 기록 초기화
    const todayKey = new Date().toISOString().slice(0, 10);
    if (localStorage.getItem('notifiedMatchesDate') !== todayKey) {
        notifiedMatches.clear();
        saveSet(STORAGE_KEY_MATCHES, notifiedMatches);
        localStorage.setItem('notifiedMatchesDate', todayKey);
    }

    function notify(title, body) {
        if (Notification.permission !== 'granted') return;
        new Notification(title, { body, icon: '/favicon.ico' });
    }

    // ── 경기 시작 N분 전 알림 (관심 팀 경기만) ──────────────────────
    async function checkMatchStart() {
        // 로그인하지 않으면 관심 팀을 알 수 없으므로 알림 불필요
        if (!localStorage.getItem('accessToken')) return;

        const today   = new Date();
        const dateStr = `${today.getFullYear()}${String(today.getMonth()+1).padStart(2,'0')}${String(today.getDate()).padStart(2,'0')}`;

        try {
            // 관심 팀 ID 목록 조회
            const favRes = await authFetch(`${BASE_URL}/api/users/me/favorites/ids`);
            if (!favRes.ok) return;
            const favoriteIds = await favRes.json(); // Long[]
            if (!favoriteIds.length) return;

            const favSet = new Set(favoriteIds.map(id => Number(id)));

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
                // 관심 팀이 참여하는 경기만 처리
                if (!favSet.has(Number(m.homeTeamId)) && !favSet.has(Number(m.awayTeamId))) return;

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
                    saveSet(STORAGE_KEY_MATCHES, notifiedMatches);
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
                    const emoji = SPORT_EMOJI[p.sportCode] || '';
                    notify(
                        nowCorrect ? `${emoji} 승부예측 적중!` : `${emoji} 승부예측 결과`,
                        nowCorrect ? '+100P 획득했습니다'       : '아쉽게도 틀렸습니다'
                    );
                    notifiedPreds.add(key);
                    saveSet(STORAGE_KEY_PREDS, notifiedPreds);
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
