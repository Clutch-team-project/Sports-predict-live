(function () {
    var files = {
        home: 'home.html',
        baseball: 'baseball-live-match.html',
        football: 'soccer-live-match.html',
        board: '/board/list',
        news: 'news.html',
        playerProfile: 'player-profile.html',
        playerStat: 'player-stats.html',
        schedule: 'match-schedule.html',
        teamProfile: 'team-page.html',
        teamStat: 'team-stats.html',
        teamRanking: 'team-standings.html',
        member: 'login.html'
    };

    var text = {
        login: '\uB85C\uADF8\uC778',
        join: '\uD68C\uC6D0\uAC00\uC785',
        loginAction: '\uB85C\uADF8\uC778\uD558\uAE30',
        ranking: '\uC21C\uC704',
        news: '\uB274\uC2A4',
        schedule: '\uC77C\uC815',
        board: '\uAC8C\uC2DC\uD310'
    };

    var path = location.pathname.toLowerCase();
    var current = decodeURIComponent((path.split('/').pop() || ''));

    // ⭐️ 수정됨: /board/ 로 시작하는 모든 경로(read, modify 등)에서 게시판 탭이 활성화되도록 완벽 처리
    function isCurrent(file) {
        if (file === files.board && path.indexOf('/board') === 0) {
            return true;
        }
        return current === file.toLowerCase();
    }

    function go(file) {
        if (file) location.href = file;
    }

    function on(el, file) {
        if (!el || !file || el.dataset.frontLinked) return;
        el.dataset.frontLinked = '1';
        el.style.cursor = 'pointer';
        el.addEventListener('click', function (event) {
            event.stopPropagation();
            go(file);
        });
    }

    function direct(items, targets) {
        Array.prototype.forEach.call(items, function (item, index) {
            on(item, targets[index]);
        });
    }

    function compact(el) {
        return (el.textContent || '').replace(/\s+/g, '');
    }

    function bindLogo() {
        Array.prototype.forEach.call(document.querySelectorAll('span, .logo, .front-logo'), function (el) {
            if ((el.textContent || '').indexOf('AI.MATCH') !== -1) on(el, files.home);
        });
    }

    function bindMemberLinks() {
        Array.prototype.forEach.call(document.querySelectorAll('span, button'), function (el) {
            var value = compact(el);
            if (value === text.login || value === text.join || value === text.loginAction) on(el, files.member);
        });
    }

    // ⭐️ 원본 그대로 유지: 상단바 생성 및 전체 레이아웃 CSS 주입 (창 사이즈 깨짐 방지)
    function installUnifiedTopbar() {
        if (document.getElementById('frontUnifiedTopbar')) return;

        var style = document.createElement('style');
        style.textContent = [
            'body{background-color:#cbd5e1!important;background-image:linear-gradient(rgba(148,163,184,.1) 1px,transparent 1px),linear-gradient(90deg,rgba(148,163,184,.1) 1px,transparent 1px)!important;background-size:24px 24px!important;}',
            'body>header,body>nav.gnb,body>nav.w-full,#boxHeaderBar,#boxPageNav,.topbar,.sport-nav,.sub-nav{display:none!important;}',
            '.front-shell{width:min(1152px,calc(100% - 48px));margin:0 auto;padding:16px 0 0;position:relative;z-index:10000;}',
            '.front-bar{height:58px;border:1px solid #cbd5e1;background:#fff;border-radius:16px;box-shadow:0 4px 12px rgba(15,23,42,.14);display:flex;align-items:center;justify-content:space-between;padding:0 18px;font-family:"Noto Sans KR",-apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;}',
            'body:not(.front-broadcast-light):not(.front-home)>main{width:min(1152px,calc(100% - 48px))!important;max-width:none!important;padding-left:0!important;padding-right:0!important;}',
            'body.front-home #canvasViewport>div{width:min(1152px,calc(100% - 48px))!important;max-width:none!important;}',
            '.front-logo{color:#2563eb;font-size:20px;font-weight:900;letter-spacing:-.03em;cursor:pointer;white-space:nowrap;}',
            '.front-sports{display:flex;align-items:center;justify-content:center;gap:34px;color:#64748b;font-size:17px;font-weight:900;}',
            '.front-sport{position:relative;padding:18px 0;cursor:pointer;}',
            '.front-sport.is-active{color:#2563eb;}',
            '.front-sport.is-active:after{content:"";position:absolute;left:0;right:0;bottom:12px;height:2px;background:#3b82f6;border-radius:10px;}',
            '.front-sport.is-disabled{cursor:default;}',
            '.front-menu{position:absolute;left:50%;top:100%;transform:translateX(-50%) translateY(8px);min-width:360px;border:1px solid #cbd5e1;background:#fff;border-radius:16px;box-shadow:0 12px 32px rgba(15,23,42,.18);padding:10px;display:flex;align-items:center;justify-content:center;gap:8px;opacity:0;pointer-events:none;transition:opacity .14s ease,transform .14s ease;}',
            '.front-sport:hover .front-menu,.front-sport:focus-within .front-menu{opacity:1;pointer-events:auto;transform:translateX(-50%) translateY(0);}',
            '.front-menu button{border:0;background:transparent;color:#64748b;font-size:14px;font-weight:900;padding:9px 14px;border-radius:10px;cursor:pointer;font-family:inherit;white-space:nowrap;}',
            '.front-menu button:hover{background:#eff6ff;color:#2563eb;}',
            '.front-actions{display:flex;align-items:center;gap:18px;color:#334155;font-size:14px;font-weight:900;white-space:nowrap;}',
            '.front-actions span{cursor:pointer;}',
            '.front-actions span:hover{color:#0f172a;}',
            '.page-label{visibility:hidden!important;}',
            'body.front-broadcast-light{--bg:#cbd5e1;--panel:#ffffff;--panel-2:#f8fafc;--card:#ffffff;--line:#cbd5e1;--line-soft:#e2e8f0;--text:#0f172a;--muted:#64748b;--shadow:0 8px 22px rgba(15,23,42,.12);color:#0f172a!important;}',
            'body.front-broadcast-light .page{padding-top:14px;}',
            'body.front-broadcast-light .scoreboard,body.front-broadcast-light .panel,body.front-broadcast-light .side-panel,body.front-broadcast-light .relay-card,body.front-broadcast-light .tabs{background:#fff!important;border-color:#cbd5e1!important;box-shadow:0 8px 22px rgba(15,23,42,.12)!important;color:#0f172a!important;}',
            'body.front-broadcast-light .scoreboard{background:linear-gradient(180deg,#fff 0%,#f8fafc 100%)!important;}',
            'body.front-broadcast-light .score-head,body.front-broadcast-light .panel-head,body.front-broadcast-light .relay-toolbar,body.front-broadcast-light .inning-board{background:#f8fafc!important;border-color:#e2e8f0!important;color:#0f172a!important;}',
            'body.front-broadcast-light .team-name,body.front-broadcast-light .panel-title,body.front-broadcast-light .relay-toolbar strong,body.front-broadcast-light .timeline-title,body.front-broadcast-light .atbat-summary strong,body.front-broadcast-light .lineup-team h4{color:#0f172a!important;}',
            'body.front-broadcast-light .team-meta,body.front-broadcast-light .score-caption,body.front-broadcast-light .panel-sub,body.front-broadcast-light .timeline-desc,body.front-broadcast-light .atbat-summary span,body.front-broadcast-light .lineup-item em,body.front-broadcast-light .state-note{color:#64748b!important;}',
            'body.front-broadcast-light .tab{color:#64748b!important;}',
            'body.front-broadcast-light .tab.active{background:#2563eb!important;color:#fff!important;box-shadow:none!important;}',
            'body.front-broadcast-light .match-phase,body.front-broadcast-light .timeline-content,body.front-broadcast-light .atbat-card,body.front-broadcast-light .lineup-team,body.front-broadcast-light .lineup-header,body.front-broadcast-light .prediction-card,body.front-broadcast-light .chat-msg{background:#f8fafc!important;border-color:#dbe3ee!important;color:#0f172a!important;}',
            'body.front-broadcast-light .record-team,body.front-broadcast-light .record-group,body.front-broadcast-light .stat-table-wrap{background:#f8fafc!important;border-color:#dbe3ee!important;color:#0f172a!important;}',
            'body.front-broadcast-light .record-team-head,body.front-broadcast-light .record-summary{background:#fff!important;border-color:#e2e8f0!important;color:#0f172a!important;}',
            'body.front-broadcast-light .team-record-row,body.front-broadcast-light .team-record-label,body.front-broadcast-light .team-record-bar,body.front-broadcast-light .team-record-values{background:transparent!important;color:#0f172a!important;}',
            'body.front-broadcast-light .team-record-row{border-color:#e2e8f0!important;}',
            'body.front-broadcast-light .team-record-label,body.front-broadcast-light .team-record-values,body.front-broadcast-light .team-record-values span{color:#0f172a!important;font-weight:900!important;}',
            'body.front-broadcast-light .team-record-bar{background:#e2e8f0!important;border-color:#cbd5e1!important;}',
            'body.front-broadcast-light .record-team-head h3,body.front-broadcast-light .record-group h4,body.front-broadcast-light .stat-table-wrap h3,body.front-broadcast-light .record-summary b{color:#0f172a!important;}',
            'body.front-broadcast-light .record-team-head span,body.front-broadcast-light .record-summary span{color:#64748b!important;}',
            'body.front-broadcast-light .record-table table,body.front-broadcast-light .stat-table{background:#fff!important;color:#0f172a!important;}',
            'body.front-broadcast-light .record-table table th,body.front-broadcast-light .record-table table td,body.front-broadcast-light .stat-table th,body.front-broadcast-light .stat-table td{background:#fff!important;color:#334155!important;border-color:#e2e8f0!important;}',
            'body.front-broadcast-light .record-table table th:first-child,body.front-broadcast-light .record-table table td:first-child,body.front-broadcast-light .stat-table th:first-child,body.front-broadcast-light .stat-table td:first-child{color:#0f172a!important;}',
            'body.front-broadcast-light .record-table .sum,body.front-broadcast-light .stat-table .sum{background:#eff6ff!important;color:#0f172a!important;}',
            'body.front-broadcast-light .stat-table-wrap::-webkit-scrollbar-thumb{background:#cbd5e1!important;}',
            'body.front-broadcast-light .bar-wrap{background:#e2e8f0!important;border-color:#cbd5e1!important;}',
            'body.front-broadcast-light .bench-card{background:#f8fafc!important;border-color:#dbe3ee!important;color:#0f172a!important;}',
            'body.front-broadcast-light .bench-card h4,body.front-broadcast-light .bench-list b{color:#0f172a!important;}',
            'body.front-broadcast-light .bench-list span{border-color:#e2e8f0!important;color:#334155!important;}',
            'body.front-broadcast-light .bench-list em{color:#2563eb!important;font-weight:950!important;}',
            'body.front-broadcast-light .bench-list .subbed em{color:#059669!important;}',
            'body.front-broadcast-light .sub-detail{color:#475569!important;}',
            'body.front-broadcast-light .legend{color:#334155!important;}',
            'body.front-broadcast-light .legend b{color:#0f172a!important;}',
            'body.front-broadcast-light .phase-score,body.front-broadcast-light .phase-arrow,body.front-broadcast-light .refresh-btn,body.front-broadcast-light .situation-refresh,body.front-broadcast-light [class*="refresh"]{color:#2563eb!important;}',
            'body.front-broadcast-light .phase-head{background:#eaf2ff!important;color:#0f172a!important;border-color:#dbeafe!important;}',
            'body.front-broadcast-light .phase-title,body.front-broadcast-light .phase-score{color:#0f172a!important;font-weight:950!important;}',
            'body.front-broadcast-light .timeline-item.goal .timeline-content,body.front-broadcast-light .timeline-item.system .timeline-content,body.front-broadcast-light .current-atbat{background:#eff6ff!important;border-color:#bfdbfe!important;}',
            'body.front-broadcast-light .timeline::before{background:#cbd5e1!important;}',
            'body.front-broadcast-light .minute-badge,body.front-broadcast-light .event-icon,body.front-broadcast-light .event-type{background:#fff!important;border-color:#cbd5e1!important;color:#2563eb!important;}',
            'body.front-broadcast-light .chat-user{color:#2563eb!important;}',
            'body.front-broadcast-light .chat-user span:last-child,body.front-broadcast-light .chat-text{color:#475569!important;}',
            'body.front-broadcast-light .chat-form{border-color:#e2e8f0!important;background:#fff!important;}',
            'body.front-broadcast-light .chat-form input{background:#f8fafc!important;border-color:#cbd5e1!important;color:#0f172a!important;}',
            'body.front-broadcast-light .chat-form button{background:#2563eb!important;color:#fff!important;}',
            'body.front-broadcast-light table th,body.front-broadcast-light table td{color:#334155!important;border-color:#e2e8f0!important;}',
            'body.front-broadcast-light .inning-board th:first-child,body.front-broadcast-light .inning-board td:first-child{color:#0f172a!important;}',
            'body.front-broadcast-light .field,body.front-broadcast-light .formation-field{box-shadow:inset 0 0 0 1px rgba(255,255,255,.25),0 8px 18px rgba(15,23,42,.12)!important;}',
            'body.front-broadcast-light .lineup-player{border-color:#cbd5e1!important;color:#0f172a!important;}',
            '@media(max-width:720px){.front-shell{width:calc(100% - 24px)}body:not(.front-broadcast-light):not(.front-home)>main{width:calc(100% - 24px)!important}body.front-home #canvasViewport>div{width:calc(100% - 24px)!important}.front-bar{height:auto;min-height:58px;gap:12px;flex-wrap:wrap;padding:12px 14px}.front-sports{order:3;width:100%;gap:22px;font-size:15px}.front-menu{min-width:min(360px,calc(100vw - 32px));gap:4px}.front-menu button{font-size:13px;padding:8px 10px}}'
        ].join('');
        document.head.appendChild(style);

        if (isCurrent(files.baseball) || isCurrent(files.football)) {
            document.body.classList.add('front-broadcast-light');
        }
        if (isCurrent(files.home) || current === '') {
            document.body.classList.add('front-home');
        }

        var shell = document.createElement('div');
        shell.id = 'frontUnifiedTopbar';
        shell.className = 'front-shell';

        // ⭐️ 원본 그대로 유지: 드롭다운 서브 메뉴도 빠짐없이 렌더링
        shell.innerHTML =
            '<div class="front-bar">' +
            '<div class="front-logo">AI.MATCH</div>' +
            '<div class="front-sports">' +
            sportItem('\uD648', files.home, isCurrent(files.home) || current === '', false, true) +
            sportItem('\uCD95\uAD6C', files.football, isCurrent(files.football)) +
            sportItem('\uC57C\uAD6C', files.baseball, isCurrent(files.baseball) || isCurrent(files.schedule) || isCurrent(files.teamProfile) || isCurrent(files.teamStat) || isCurrent(files.teamRanking) || isCurrent(files.playerProfile) || isCurrent(files.playerStat)) +
            sportItem('LOL', null, false, true, false) +
            '</div>' +
            '<div class="front-actions"><span data-front-member="1">\uB85C\uADF8\uC778</span><span>\uC0AC\uC774\uD2B8\uB9F5</span></div>' +
            '</div>';
        document.body.insertBefore(shell, document.body.firstChild);

        Array.prototype.forEach.call(shell.querySelectorAll('[data-front-go]'), function (el) {
            var target = el.getAttribute('data-front-go');
            if (target) on(el, target);
        });
        Array.prototype.forEach.call(shell.querySelectorAll('[data-front-member]'), function (el) {
            on(el, files.member);
        });
    }

    // ⭐️ 원본 그대로 유지: 서브메뉴(front-menu) 구성 로직
    function sportItem(label, target, active, disabled, shortMenu) {
        return '<div class="front-sport' + (active ? ' is-active' : '') + (disabled ? ' is-disabled' : '') + '"' + (target ? ' data-front-go="' + target + '"' : '') + '>' +
            '<span>' + label + '</span>' +
            (shortMenu === false ? '' : '<div class="front-menu">' +
                '<button type="button" data-front-go="' + files.news + '">' + text.news + '</button>' +
                '<button type="button" data-front-go="' + files.schedule + '">' + text.schedule + '</button>' +
                (shortMenu ? '' : '<button type="button" data-front-go="' + files.teamRanking + '">' + text.ranking + '</button>') +
                '<button type="button" data-front-go="' + files.board + '">' + text.board + '</button>' +
                '</div>') +
            '</div>';
    }

    // ⭐️ 핵심 실행부: 게시판 외 다른 페이지 전용 바인딩 함수들은 모두 제거했습니다.
    installUnifiedTopbar();
    bindLogo();
    bindMemberLinks();
})();