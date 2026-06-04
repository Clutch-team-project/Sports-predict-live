(function () {
    // ⭐️ [핵심 수정] 앞에 '/'를 붙여 절대 경로화하고, 뒤의 '.html' 확장자를 모두 제거하여 스프링 컨트롤러 주소와 일치시켰습니다.
    var files = {
        home: '/',
        baseball: '/baseball-live-match',
        football: '/soccer-live-match',
        board: '/board/list',
        news: '/news',
        playerProfile: '/player-profile',
        playerStat: '/player-stats',
        schedule: '/match-schedule',
        teamProfile: '/team-page',
        teamStat: '/team-stats',
        teamRanking: '/team-standings',
        member: '/login' // 👈 이제 /board/ 가 붙지 않고 무조건 http://localhost:8080/login 으로 이동합니다.
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

    // 현재 주소의 맨 마지막 파트 추출 기능 (비교용)
    var current = decodeURIComponent((path.split('/').pop() || ''));

    // /board/ 로 시작하는 모든 경로(list, read, register 등)에서 게시판 탭 활성화
    function isCurrent(file) {
        if (file === files.board && path.indexOf('/board') === 0) {
            return true;
        }
        // 절대 경로 비교를 위해 주소록 파일 매칭 수정
        var cleanFile = file.replace('/', '');
        return current === cleanFile.toLowerCase();
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

    function compact(el) {
        return (el.textContent || '').replace(/\s+/g, '');
    }

    // 로고 클릭 바인딩
    function bindLogo() {
        Array.prototype.forEach.call(document.querySelectorAll('span, .logo, .front-logo'), function (el) {
            if ((el.textContent || '').indexOf('AI.MATCH') !== -1) on(el, files.home);
        });
    }

    // HTML 내부의 쌩 글자 로그인/회원가입 버튼 감지 및 이동
    function bindMemberLinks() {
        Array.prototype.forEach.call(document.querySelectorAll('span, button'), function (el) {
            var value = compact(el);
            if (value === text.login || value === text.join || value === text.loginAction) on(el, files.member);
        });
    }

    // 상단바 렌더링 및 레이아웃 유지
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
            '@media(max-width:720px){.front-shell{width:calc(100% - 24px)}body:not(.front-broadcast-light):not(.front-home)>main{width:calc(100% - 24px)!important}body.front-home #canvasViewport>div{width:calc(100% - 24px)!important}.front-bar{height:auto;min-height:58px;gap:12px;flex-wrap:wrap;padding:12px 14px}.front-sports{order:3;width:100%;gap:22px;font-size:15px}.front-menu{min-width:min(360px,calc(100vw - 32px));gap:4px}.front-menu button{font-size:13px;padding:8px 10px}}'
        ].join('');
        document.head.appendChild(style);

        var shell = document.createElement('div');
        shell.id = 'frontUnifiedTopbar';
        shell.className = 'front-shell';

        var isHome = path === '/' || path === '';

        shell.innerHTML =
            '<div class="front-bar">' +
            '<div class="front-logo" data-front-go="' + files.home + '">AI.MATCH</div>' +
            '<div class="front-sports">' +
            sportItem('\uD648', files.home, isHome, false, true) +
            sportItem('\uCD95\uAD6C', files.football, path.indexOf('/soccer') === 0) +
            sportItem('\uC57C\uAD6C', files.baseball, path.indexOf('/baseball') === 0 || path.indexOf('/match') === 0 || path.indexOf('/team') === 0 || path.indexOf('/player') === 0) +
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

    // 마우스 오버 시 하위 서브메뉴 렌더링
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

    // 실행 구역
    installUnifiedTopbar();
    bindLogo();
    bindMemberLinks();
})();