// ==========================================
// 1. 공통 전역 변수 (중복 선언 방지를 위해 최상단에 1번만 선언)
// ==========================================
const token = localStorage.getItem('accessToken');


// ==========================================
// 2. LIST PAGE LOGIC (리스트 페이지 전용)
// ==========================================
function clickWriteBtn() {
    // 외부 JS에서는 타임리프([[${..}]])가 작동하지 않으므로 URL 파라미터에서 추출합니다.
    const urlParams = new URLSearchParams(window.location.search);
    const currentBoardType = urlParams.get('boardType') || '';

    if (!token) {
        if (confirm('로그인 후 이용 가능합니다.\n로그인 페이지로 이동하시겠습니까?')) {
            location.href = '/login';
        } else {
            location.href = currentBoardType ? '/board/list?boardType=' + currentBoardType : '/board/list';
        }
    } else {
        location.href = currentBoardType ? '/board/register?boardType=' + currentBoardType : '/board/register';
    }
}

function chgCategory(catValue) {
    document.getElementById('formCategory').value = catValue;
    document.getElementById('boardActionForm').submit();
}

function chgSort(sortValue) {
    document.getElementById('formSort').value = sortValue;
    document.getElementById('boardActionForm').submit();
}

function updatePlaceholder() {
    const typeSelect = document.getElementById('searchType');
    const keywordInput = document.getElementById('searchKeyword');
    if (!typeSelect || !keywordInput) return; // 화면에 검색창이 없으면 작동 중지

    switch(typeSelect.value) {
        case 'tc': keywordInput.placeholder = "🔍  제목+내용 검색"; break;
        case 't':  keywordInput.placeholder = "🔍  제목 검색"; break;
        case 'c':  keywordInput.placeholder = "🔍  내용 검색"; break;
        case 'w':  keywordInput.placeholder = "🔍  작성자 검색"; break;
        default:   keywordInput.placeholder = "🔍  검색어 입력"; break;
    }
}

document.addEventListener("DOMContentLoaded", function() {
    updatePlaceholder();
});


// ==========================================
// 3. MODIFY PAGE LOGIC (수정 페이지 전용)
// ==========================================
const modifyForm = document.getElementById('modifyForm');
if (modifyForm) { // 화면에 modifyForm이 있을 때만 아래 로직 실행
    if (!token) {
        alert('로그인 후 이용 가능합니다.');
        location.href = '/login';
    }

    modifyForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const boardId = document.getElementById('boardId').value;
        const category = document.getElementById('category').value;
        const title = document.getElementById('title').value.trim();
        const content = document.getElementById('content').value.trim();
        const boardType = document.getElementById('boardType').value;

        if(!title) { alert('제목을 입력해주세요.'); return; }
        if(!content) { alert('내용을 입력해주세요.'); return; }

        const formData = new URLSearchParams();
        formData.append('boardId', boardId);
        formData.append('category', category);
        formData.append('title', title);
        formData.append('content', content);

        window.authFetch('/board/modify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: formData.toString()
        })
            .then(async res => {
                if (res.ok) {
                    alert('게시글이 성공적으로 수정되었습니다.');
                    const pageInput = document.getElementById('page');
                    const boardTypeInput = document.getElementById('boardType');
                    const pageValue = (pageInput && pageInput.value) ? pageInput.value : '1';
                    const typeValue = (boardTypeInput && boardTypeInput.value) ? boardTypeInput.value : '';

                    let targetUrl = '/board/list?page=' + pageValue;
                    if (typeValue) targetUrl += '&boardType=' + typeValue;
                    location.href = targetUrl;
                } else {
                    const errorMessage = await res.text();
                    alert(errorMessage || '수정 중 오류가 발생했습니다.');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('서버와 통신 중 오류가 발생했습니다.');
            });
    });
}


// ==========================================
// 4. REGISTER PAGE LOGIC (등록 페이지 전용)
// ==========================================
const registerForm = document.getElementById('registerForm');
if (registerForm) { // 화면에 registerForm이 있을 때만 아래 로직 실행
    if (!token) {
        if (confirm('로그인 후 이용 가능합니다.\n로그인 페이지로 이동하시겠습니까?')) {
            location.href = '/login';
        } else {
            location.href = '/board/list';
        }
    }

    registerForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const category = document.getElementById('category').value;
        const title = document.getElementById('title').value.trim();
        const content = document.getElementById('content').value.trim();
        const boardTypeEl = document.getElementById('boardType');
        const boardType = boardTypeEl ? boardTypeEl.value : '';

        if(!category) { alert('카테고리를 선택해주세요.'); return; }
        if(!title) { alert('제목을 입력해주세요.'); return; }
        if(!content) { alert('내용을 입력해주세요.'); return; }

        const formData = new URLSearchParams();
        formData.append('category', category);
        formData.append('title', title);
        formData.append('content', content);
        if(boardType) formData.append('boardType', boardType);

        window.authFetch('/board/register', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded'
                // authFetch에서 자동으로 토큰을 넣어주므로 명시할 필요 없음
            },
            body: formData.toString()
        })
            .then(response => {
                if (response.redirected) {
                    location.href = response.url;
                } else if (response.status === 401 || response.status === 403) {
                    alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
                    location.href = '/login';
                } else if (response.ok) {
                    alert('게시글이 등록되었습니다.');
                    location.href = boardType ? '/board/list?boardType=' + boardType : '/board/list';
                } else {
                    alert('게시글 등록에 실패했습니다.');
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('서버와 통신 중 오류가 발생했습니다.');
            });
    });

    const btnCancel = document.querySelector('.btn-cancel');
    if (btnCancel) {
        btnCancel.removeAttribute('onclick');
        btnCancel.addEventListener('click', function() {
            const boardTypeEl = document.getElementById('boardType');
            const boardType = boardTypeEl ? boardTypeEl.value : '';
            location.href = boardType ? '/board/list?boardType=' + boardType : '/board/list';
        });
    }
}


// ==========================================
// 5. READ PAGE LOGIC (조회 페이지 전용)
// ==========================================
// 함수들은 다른 페이지에서 HTML onClick으로 호출될 때 에러가 나지 않도록 전역 함수로 유지합니다.

function removePost() {
    const boardId = document.getElementById('currentBoardId').value;
    if(!confirm('정말로 이 게시글을 삭제하시겠습니까?')) return;

    window.authFetch('/board/remove', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ boardId: boardId }).toString()
    })
        .then(async res => {
            if (res.ok) {
                alert('게시글이 삭제되었습니다.');
                location.href = '/board/list';
            } else {
                const errorMessage = await res.text();
                alert(errorMessage || '삭제 권한이 없거나 실패했습니다.');
            }
        }).catch(err => console.error(err));
}

function toggleBlind() {
    const boardId = document.getElementById('currentBoardId').value;
    if(!confirm('이 게시글의 블라인드 상태를 변경하시겠습니까?')) return;

    window.authFetch(`/board/admin/${boardId}/blind`, { method: 'PUT' })
        .then(res => {
            if (res.ok) {
                alert('블라인드 상태가 성공적으로 변경되었습니다.');
                location.reload();
            } else {
                alert('처리에 실패했거나 권한이 없습니다.');
            }
        }).catch(err => {
        console.error(err);
        alert('서버 통신 중 오류가 발생했습니다.');
    });
}

// 댓글 페이징 및 정렬 전역 상태
let currentReplyPage = 1;
let currentReplySort = 'like';

function printReplies(page = 1) {
    const boardIdEl = document.getElementById('currentBoardId');
    const replyList = document.getElementById('replyList');
    if (!boardIdEl || !replyList) return; // 읽기 페이지가 아니면 중지

    const boardId = boardIdEl.value;
    const loggedInUserId = document.getElementById('loggedInUserId')?.value;
    const loggedInUserRole = document.getElementById('loggedInUserRole')?.value || '';

    currentReplyPage = page;

    const blindMsgBox = document.getElementById('blindCommentMsg');
    if (blindMsgBox) {
        document.getElementById('replyInputArea').style.display = 'none';
        document.getElementById('replyDivider').style.display = 'none';
        if(document.getElementById('replySortArea')) document.getElementById('replySortArea').style.display = 'none';
        replyList.style.display = 'none';
        document.getElementById('replyPaging').style.display = 'none';

        window.authFetch(`/replies/list/${boardId}?page=1&size=1&sort=${currentReplySort}`)
            .then(res => res.json())
            .then(data => {
                const totalCount = data.total || 0;
                if (document.getElementById('replyCountMain')) document.getElementById('replyCountMain').innerText = totalCount;
                if (document.getElementById('replyCountSide')) document.getElementById('replyCountSide').innerText = totalCount;
            }).catch(err => console.error(err));
        return;
    }

    window.authFetch(`/replies/list/${boardId}?page=${page}&size=10&sort=${currentReplySort}`)
        .then(res => {
            if (!res.ok) throw new Error("서버 응답 오류");
            return res.json();
        })
        .then(data => {
            const totalCount = data.total || 0;
            const countMain = document.getElementById('replyCountMain');
            const countSide = document.getElementById('replyCountSide');

            if (countMain) countMain.innerText = totalCount;
            if (countSide) countSide.innerText = totalCount;

            let listStr = '';
            if (data.dtoList && data.dtoList.length > 0) {
                data.dtoList.forEach(reply => {
                    let btnStr = '';
                    let replyContentStr = reply.replyText;
                    if (reply.blinded) replyContentStr = `<span style="color:#94a3b8; font-style:italic;">블라인드 처리된 댓글입니다.</span>`;

                    if (!reply.blinded) {
                        const likeColor = reply.liked ? 'text-blue-600 font-bold' : 'text-slate-500';
                        btnStr += `<button onclick="likeReply(${reply.replyId})" class="text-xs ${likeColor} hover:underline mr-2">♥ ${reply.likeCount || 0}</button>`;

                        if (loggedInUserId && loggedInUserId != reply.userId && loggedInUserRole !== 'ROLE_ADMIN') {
                            const reportColor = reply.reported ? 'text-red-600 font-bold' : 'text-slate-500';
                            btnStr += `<button onclick="reportReply(${reply.replyId})" class="text-xs ${reportColor} hover:underline mr-2">신고</button>`;
                        }
                    }

                    if (loggedInUserId == reply.userId) {
                        const safeText = reply.replyText.replace(/'/g, "\\'").replace(/"/g, '\\"');
                        btnStr += `<button onclick="modifyReplyPrompt(${reply.replyId}, '${safeText}')" class="text-xs text-blue-500 font-semibold hover:underline mr-2">수정</button>`;
                    }

                    if (loggedInUserId == reply.userId || loggedInUserRole === 'ROLE_ADMIN') {
                        btnStr += `<button onclick="removeReply(${reply.replyId})" class="text-xs text-red-500 font-semibold hover:underline mr-2">삭제</button>`;
                    }

                    if (loggedInUserRole === 'ROLE_ADMIN') {
                        const blindBtnText = reply.blinded ? '블라인드 해제' : '블라인드';
                        btnStr += `<button onclick="toggleReplyBlind(${reply.replyId})" class="text-xs text-amber-600 font-semibold hover:underline">${blindBtnText}</button>`;
                    }

                    listStr += `
                        <li class="py-3 border-b border-slate-100">
                            <div class="flex justify-between items-center mb-1">
                                <div>
                                    <span class="font-bold text-sm text-slate-800">${reply.nickname || '익명'}</span>
                                    <span class="text-xs text-slate-400 ml-2">${reply.createdAt}</span>
                                </div>
                                <div>${btnStr}</div>
                            </div>
                            <div class="text-sm text-slate-600 break-all leading-relaxed">${replyContentStr}</div>
                        </li>
                    `;
                });
            } else {
                listStr = '<div class="text-center py-10 text-slate-400 text-sm">등록된 댓글이 없습니다. 첫 댓글을 남겨보세요!</div>';
            }

            replyList.innerHTML = listStr;

            let pagingStr = '';
            if (data.prev) pagingStr += `<li class="cursor-pointer px-3 py-1 bg-slate-50 border border-slate-200 rounded text-slate-600 hover:bg-blue-50" onclick="printReplies(${data.start - 1})">이전</li>`;
            for (let i = data.start; i <= data.end; i++) {
                const activeClass = (data.page === i) ? 'bg-blue-600 text-white border-blue-600' : 'bg-white border-slate-200 text-slate-600 hover:bg-slate-50';
                pagingStr += `<li class="cursor-pointer px-3 py-1 border rounded ${activeClass}" onclick="printReplies(${i})">${i}</li>`;
            }
            if (data.next) pagingStr += `<li class="cursor-pointer px-3 py-1 bg-slate-50 border border-slate-200 rounded text-slate-600 hover:bg-blue-50" onclick="printReplies(${data.end + 1})">다음</li>`;

            const replyPaging = document.getElementById('replyPaging');
            if (replyPaging) replyPaging.innerHTML = pagingStr;
        })
        .catch(err => console.error('댓글 로딩 에러:', err));
}

function removeReply(replyId) {
    if(!confirm('댓글을 삭제하시겠습니까?')) return;
    window.authFetch(`/replies/${replyId}`, { method: 'DELETE' })
        .then(async res => {
            if(res.ok) printReplies(currentReplyPage);
            else {
                const msg = await res.text();
                alert(msg || '댓글 삭제에 실패했습니다.');
            }
        }).catch(err => console.error('댓글 삭제 에러:', err));
}

function modifyReplyPrompt(replyId, originalText) {
    const newText = prompt("댓글을 수정하시겠습니까?", originalText);
    if (newText === null) return;
    if (newText.trim() === '') { alert("공백으로 수정할 수 없습니다."); return; }

    window.authFetch(`/replies/${replyId}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ replyText: newText.trim() })
    }).then(res => {
        if (res.ok) { alert("댓글이 수정되었습니다."); printReplies(currentReplyPage); }
        else alert("수정 권한이 없거나 오류가 발생했습니다.");
    }).catch(err => console.error(err));
}

function toggleReplyBlind(replyId) {
    if (!confirm("이 댓글의 블라인드 상태를 변경하시겠습니까?")) return;
    window.authFetch(`/replies/admin/${replyId}/blind`, { method: 'PUT' })
        .then(res => {
            if (res.ok) { alert("댓글 블라인드 처리가 정상 반영되었습니다."); printReplies(currentReplyPage); }
            else alert("권한이 없거나 실패했습니다.");
        }).catch(err => console.error(err));
}

function likeReply(replyId) {
    if (!token) {
        if(confirm('로그인 후 이용 가능합니다.\n로그인 페이지로 이동하시겠습니까?')) location.href='/login';
        return;
    }
    window.authFetch(`/replies/like`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ replyId: replyId })
    }).then(async res => {
        if (res.ok) printReplies(currentReplyPage);
        else {
            const msg = await res.text();
            alert(msg || '오류가 발생했습니다.');
        }
    }).catch(err => console.error(err));
}

function reportReply(replyId) {
    if (!token) {
        if(confirm('로그인 후 이용 가능합니다.\n로그인 페이지로 이동하시겠습니까?')) location.href='/login';
        return;
    }
    if(!confirm('이 댓글을 신고하시겠습니까?')) return;
    window.authFetch(`/replies/report`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ replyId: replyId })
    }).then(async res => {
        if (res.ok) { alert("댓글 신고가 접수되었습니다."); printReplies(currentReplyPage); }
        else {
            const msg = await res.text();
            alert(msg || '오류가 발생했습니다.');
        }
    }).catch(err => console.error(err));
}

function chgReplySort(sortType) {
    currentReplySort = sortType;
    const latestBtn = document.getElementById('replySortLatest');
    const likeBtn = document.getElementById('replySortLike');

    if (sortType === 'latest') {
        if(latestBtn) latestBtn.className = "text-xs font-bold text-blue-600 cursor-pointer";
        if(likeBtn) likeBtn.className = "text-xs font-semibold text-slate-500 cursor-pointer";
    } else {
        if(latestBtn) latestBtn.className = "text-xs font-semibold text-slate-500 cursor-pointer";
        if(likeBtn) likeBtn.className = "text-xs font-bold text-blue-600 cursor-pointer";
    }
    printReplies(1);
}

// 📌 DOM 로딩이 끝난 후, 버튼 이벤트 리스너 부착 및 최초 댓글 로딩
document.addEventListener("DOMContentLoaded", function() {
    const currentBoardIdEl = document.getElementById('currentBoardId');
    if (currentBoardIdEl) {
        const boardId = currentBoardIdEl.value;

        // 게시글 좋아요 이벤트
        const btnLike = document.getElementById('btnLike');
        if (btnLike) {
            btnLike.addEventListener('click', function() {
                if (!token) {
                    if(confirm('로그인 후 이용 가능합니다.\n로그인 페이지로 이동하시겠습니까?')) location.href='/login';
                    return;
                }
                const countSpan = btnLike.querySelector('.like-count-text');
                const sidebarCountSpan = document.getElementById('sidebarLikeCount');
                window.authFetch('/board/like', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ boardId: boardId }).toString()
                }).then(async res => {
                    const msg = await res.text();
                    if (res.ok) {
                        let currentCount = parseInt(countSpan.innerText);
                        if(btnLike.classList.contains('is-active')){
                            btnLike.classList.remove('is-active');
                            countSpan.innerText = currentCount - 1;
                            if(sidebarCountSpan) sidebarCountSpan.innerText = currentCount - 1;
                        } else {
                            btnLike.classList.add('is-active');
                            countSpan.innerText = currentCount + 1;
                            if(sidebarCountSpan) sidebarCountSpan.innerText = currentCount + 1;
                        }
                    } else alert(msg || '오류가 발생했습니다.');
                }).catch(err => console.error(err));
            });
        }

        // 게시글 신고 이벤트
        const btnReport = document.getElementById('btnReport');
        if (btnReport) {
            btnReport.addEventListener('click', function() {
                if (!token) {
                    if(confirm('로그인 후 이용 가능합니다.\n로그인 페이지로 이동하시겠습니까?')) location.href='/login';
                    return;
                }
                if (btnReport.classList.contains('is-active')) { alert('이미 신고가 접수된 게시글입니다.'); return; }
                if(!confirm('이 게시글을 정말로 신고하시겠습니까?')) return;

                window.authFetch('/board/report', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: new URLSearchParams({ boardId: boardId }).toString()
                }).then(async res => {
                    const msg = await res.text();
                    if (res.ok) { alert(msg); btnReport.classList.add('is-active'); }
                    else alert(msg || '오류가 발생했습니다.');
                }).catch(err => console.error(err));
            });
        }

        // 댓글 등록 이벤트
        const btnRegisterReply = document.getElementById('btnRegisterReply');
        const replyInput = document.getElementById('replyInput');
        if (btnRegisterReply && replyInput) {
            btnRegisterReply.addEventListener('click', function() {
                const loggedInUserId = document.getElementById('loggedInUserId')?.value;
                if (!token || !loggedInUserId) {
                    if(confirm('로그인 후 이용 가능합니다.\n로그인 페이지로 이동하시겠습니까?')) location.href='/login';
                    return;
                }
                const text = replyInput.value.trim();
                if (text === '') { alert('댓글 내용을 입력해 주세요.'); replyInput.focus(); return; }

                window.authFetch('/replies/', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ boardId: boardId, replyText: text, userId: loggedInUserId })
                }).then(async res => {
                    if(res.ok) {
                        replyInput.value = '';
                        const mainCountEl = document.getElementById('replyCountMain');
                        const totalCount = mainCountEl ? parseInt(mainCountEl.innerText) + 1 : 1;
                        printReplies(Math.ceil(totalCount / 10));
                    } else {
                        const msg = await res.text();
                        alert(msg || '댓글 등록에 실패했습니다.');
                    }
                }).catch(err => console.error('댓글 등록 에러:', err));
            });
        }

        // 초기 댓글 로드
        printReplies(1);
    }
});//