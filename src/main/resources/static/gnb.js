(function () {

    /* 인증 API 공통 fetch (토큰 만료 시 자동 재발급) */
    window.authFetch = async function (url, options) {
        options = options || {};
        options.headers = options.headers || {};
        options.headers['Authorization'] = 'Bearer ' + localStorage.getItem('accessToken');

        var res = await fetch(url, options);

        // Access Token 만료 시 재발급 시도
        if (res.status === 401) {
            var refreshToken = localStorage.getItem('refreshToken');
            if (!refreshToken) {
                localStorage.clear();
                location.href = '/login';
                return res;
            }

            var reissueRes = await fetch('http://localhost:8080/api/auth/reissue', {
                method: 'POST',
                headers: {'Authorization': 'Bearer ' + refreshToken}
            });

            if (!reissueRes.ok) {
                // Refresh Token도 만료 → 로그아웃
                localStorage.clear();
                location.href = '/login';
                return res;
            }

            var data = await reissueRes.json();
            localStorage.setItem('accessToken', data.accessToken);
            localStorage.setItem('refreshToken', data.refreshToken);

            // 원래 요청 재시도
            options.headers['Authorization'] = 'Bearer ' + data.accessToken;
            return await fetch(url, options);
        }

        return res;
    };

    var files = {
        home: '/',
        football: '/soccer',
        baseball: '/baseball',
        lol: '/lol',
        news: '/news',
        schedule: '/schedule',
        board: '/board',
        member: '/login',
        loginSuccess: '/login-success',
        userInfo: '/user-info',
        // 종목별 순위 페이지
        baseballStandings: '/baseball/standings',
        soccerStandings: '/soccer/standings',
        lolStandings: '/lol/standings',
    };

    var current = decodeURIComponent((location.pathname.split('/').pop() || '').toLowerCase());

    /* 유틸 */
    function go(file) {
        if (file) location.href = file;
    }

    function on(el, file) {
        if (!el || !file || el.dataset.frontLinked) return;
        el.dataset.frontLinked = '1';
        el.style.cursor = 'pointer';
        el.addEventListener('click', function (e) {
            e.stopPropagation();
            go(file);
        });
    }

    /* 스타일 주입 */
    function injectStyles() {
        var s = document.createElement('style');
        s.textContent = [
            'body>header,body>nav,#boxHeaderBar,#boxPageNav,#gnbWrap{display:none!important;}',
            'body{background-color:#cbd5e1!important;background-image:linear-gradient(rgba(148,163,184,.1) 1px,transparent 1px),linear-gradient(90deg,rgba(148,163,184,.1) 1px,transparent 1px)!important;background-size:24px 24px!important;}',
            '.fl-shell{width:min(1024px,calc(100% - 48px));margin:0 auto;padding:16px 0 0;position:relative;z-index:10000;}',
            '.fl-bar{height:58px;background:#fff;border:1px solid #cbd5e1;border-radius:16px;box-shadow:0 4px 12px rgba(15,23,42,.14);display:flex;align-items:center;justify-content:space-between;padding:0 20px;font-family:"Noto Sans KR",-apple-system,sans-serif;}',
            '.fl-logo{color:#2563eb;font-size:20px;font-weight:900;letter-spacing:-.03em;cursor:pointer;}',
            '.fl-sports{display:flex;align-items:center;gap:34px;font-size:17px;font-weight:900;color:#64748b;}',
            '.fl-sport{position:relative;padding:18px 0;cursor:pointer;transition:color .15s;}',
            '.fl-sport:hover{color:#1e293b;}',
            '.fl-sport.is-active{color:#2563eb;}',
            '.fl-sport.is-active::after{content:"";position:absolute;left:0;right:0;bottom:12px;height:2px;background:#3b82f6;border-radius:10px;}',
            '.fl-menu{position:absolute;left:50%;top:100%;transform:translateX(-50%) translateY(8px);min-width:300px;background:#fff;border:1px solid #cbd5e1;border-radius:16px;box-shadow:0 12px 32px rgba(15,23,42,.18);padding:10px;display:flex;align-items:center;justify-content:center;gap:4px;opacity:0;pointer-events:none;transition:opacity .14s,transform .14s;}',
            '.fl-sport:hover .fl-menu{opacity:1;pointer-events:auto;transform:translateX(-50%) translateY(0);}',
            '.fl-menu button{border:0;background:transparent;color:#64748b;font-size:14px;font-weight:900;padding:9px 16px;border-radius:10px;cursor:pointer;font-family:inherit;white-space:nowrap;}',
            '.fl-menu button:hover{background:#eff6ff;color:#2563eb;}',
            '.fl-actions{display:flex;gap:20px;font-size:14px;font-weight:900;color:#334155;}',
            '.fl-actions span{cursor:pointer;}',
            '.fl-actions span:hover{color:#0f172a;}',
            '.page-label{visibility:hidden!important;}'
        ].join('');
        document.head.appendChild(s);
    }

    /* 종목 아이템 HTML */
    function sportItem(label, target, active, menuItems) {
        var cls = 'fl-sport' + (active ? ' is-active' : '');
        var goAttr = target ? ' data-fl-go="' + target + '"' : '';
        var menuHtml = '';
        if (menuItems && menuItems.length) {
            menuHtml = '<div class="fl-menu">';
            menuItems.forEach(function (item) {
                menuHtml += '<button type="button" data-fl-go="' + item.file + '">' + item.label + '</button>';
            });
            menuHtml += '</div>';
        }
        return '<div class="' + cls + '"' + goAttr + '><span>' + label + '</span>' + menuHtml + '</div>';
    }

    /* GNB 주입 */
    function installTopbar() {
        if (document.getElementById('flTopbar')) return;
        injectStyles();

        /* 홈: 뉴스 / 일정 / 게시판 */
        var homeMenu = [
            {label: '뉴스', file: files.news},
            {label: '일정', file: files.schedule},
            {label: '게시판', file: files.board}
        ];

        /* 종목별 서브메뉴 */
        var baseballMenu = [
            {label: '뉴스', file: files.news},
            {label: '일정', file: files.schedule},
            {label: '순위', file: files.baseballStandings},
            {label: '게시판', file: files.board}
        ];
        var soccerMenu = [
            {label: '뉴스', file: files.news},
            {label: '일정', file: files.schedule},
            {label: '순위', file: files.soccerStandings},
            {label: '게시판', file: files.board}
        ];
        var lolMenu = [
            {label: '뉴스', file: files.news},
            {label: '일정', file: files.schedule},
            {label: '순위', file: files.lolStandings},
            {label: '게시판', file: files.board}
        ];

        var isHome = current === '' || current === '/' || current.indexOf('home') !== -1;
        var isFootball = current.indexOf('soccer') !== -1;
        var isBaseball = current.indexOf('baseball') !== -1;
        var isLol = current.indexOf('lol') !== -1;

        var shell = document.createElement('div');
        shell.id = 'flTopbar';
        shell.className = 'fl-shell';
        shell.innerHTML =
            '<div class="fl-bar">' +
            '<div class="fl-logo" data-fl-go="' + files.home + '">AI.MATCH</div>' +
            '<div class="fl-sports">' +
            sportItem('홈', files.home, isHome, homeMenu) +
            sportItem('축구', files.football, isFootball, soccerMenu) +
            sportItem('야구', files.baseball, isBaseball, baseballMenu) +
            sportItem('LOL', files.lol, isLol, lolMenu) +
            '</div>' +
            '<div class="fl-actions">' +
            (localStorage.getItem('accessToken')
                ? (function () {
                    var nickname = localStorage.getItem('nickname');
                    var loginId = localStorage.getItem('loginId') || '';
                    var display = nickname
                        ? nickname
                        : (loginId.length > 3
                            ? loginId.substring(0, 3) + '*'.repeat(loginId.length - 3)
                            : loginId);
                    return '<span id="fl-nickname" data-fl-go="' + files.userInfo + '" style="cursor:pointer">' + display + '</span>' +
                        '<span id="fl-logout" style="cursor:pointer;color:#ef4444;">로그아웃</span>';
                })()
                : '<span data-fl-member="1">로그인</span>') +
            '<span>사이트맵</span>' +
            '</div>' +
            '</div>';

        document.body.insertBefore(shell, document.body.firstChild);

        shell.querySelectorAll('[data-fl-go]').forEach(function (el) {
            on(el, el.getAttribute('data-fl-go'));
        });
        shell.querySelectorAll('[data-fl-member]').forEach(function (el) {
            on(el, files.member);
        });

        // 닉네임 클릭 → 내 정보
        var nicknameEl = shell.querySelector('#fl-nickname');
        if (nicknameEl) {
            on(nicknameEl, files.userInfo);
        }

        // 로그아웃 클릭
        var logoutEl = shell.querySelector('#fl-logout');
        if (logoutEl) {
            logoutEl.style.cursor = 'pointer';
            logoutEl.addEventListener('click', function () {
                var token = localStorage.getItem('accessToken');
                if (token) {
                    fetch('http://localhost:8080/api/auth/logout', {
                        method: 'POST',
                        headers: {'Authorization': 'Bearer ' + token}
                    }).finally(function () {
                        localStorage.clear();
                        location.href = files.member;
                    });
                } else {
                    localStorage.clear();
                    location.href = files.member;
                }
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', installTopbar);
    } else {
        installTopbar();
    }

})();