// ==========================================
// 1. 공통 전역 변수 (중복 선언 방지를 위해 최상단에 1번만 선언)
// ==========================================
const token = localStorage.getItem('accessToken');


// ==========================================
// 2. LIST PAGE LOGIC (리스트 페이지 전용)
// ==========================================
function clickWriteBtn() {
    const urlParams = new URLSearchParams(window.location.search);
    const currentBoardType = (urlParams.get('boardType') || '').toLowerCase();

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
    if (!typeSelect || !keywordInput) return;

    switch(typeSelect.value) {
        case 'tc': keywordInput.placeholder = "🔍  제목+내용 검색"; break;
        case 't':  keywordInput.placeholder = "🔍  제목 검색"; break;
        case 'c':  keywordInput.placeholder = "🔍  내용 검색"; break;
        case 'w':  keywordInput.placeholder = "🔍  작성자 검색"; break;
        default:   keywordInput.placeholder = "🔍  검색어 입력"; break;
    }
}

document.addEventListener('DOMContentLoaded', function() {
    const safebotSwitch = document.getElementById('safebotSwitch');
    if(!safebotSwitch) return;

    const isSafebotOn = localStorage.getItem('safebot') !== 'OFF';
    safebotSwitch.checked = isSafebotOn;
    applySafebot(isSafebotOn);

    safebotSwitch.addEventListener('change', function() {
        const isOn = this.checked;
        localStorage.setItem('safebot', isOn ? 'ON' : 'OFF');
        applySafebot(isOn);
    });

    function applySafebot(isOn) {
        document.querySelectorAll('.board-item').forEach(item => {
            const isBlinded = item.getAttribute('data-blinded') === 'true';

            if (isBlinded) {
                const blindTitle = item.querySelector('.blind-title');
                const realTitle = item.querySelector('.real-title');

                if (isOn) {
                    if(blindTitle) blindTitle.style.display = 'inline';
                    if(realTitle) realTitle.style.display = 'none';
                } else {
                    if(blindTitle) blindTitle.style.display = 'none';
                    if(realTitle) realTitle.style.display = 'inline';
                }
            }
        });
    }
});

function clickBoardRow(url, isBlinded, isDeleted) {
    if (isDeleted) return;

    const loggedInUserRole = document.getElementById('loggedInUserRole')?.value || '';

    if (isBlinded) {
        if (loggedInUserRole === 'ROLE_ADMIN' || localStorage.getItem('safebot') === 'OFF') {
            location.href = url;
        } else {
            alert('AI 세이프봇이 작동 중입니다.\n우측 상단의 스위치를 끄면 내용을 볼 수 있습니다.');
        }
    } else {
        location.href = url;
    }
}

document.addEventListener("DOMContentLoaded", function() {
    updatePlaceholder();
});


// ==========================================
// 3. MODIFY PAGE LOGIC (수정 페이지 전용)
// ==========================================
const modifyForm = document.getElementById('modifyForm');
if (modifyForm) {
    if (!token) {
        alert('로그인 후 이용 가능합니다.');
        location.href = '/login';
    }

    let modifyFilesArray = [];
    const modifyFilesInput = document.getElementById('modifyFiles');
    const modifyFilePreview = document.getElementById('modifyFilePreview');

    // 기존 등록된 파일 개수 파악
    function getExistingFileCount() {
        return document.querySelectorAll('.existing-file-item').length;
    }

    // 기존 파일 삭제
    window.removeExistingFile = function(btn) {
        btn.closest('.existing-file-item').remove();
    }

    // 새 파일 추가 (누적)
    if (modifyFilesInput) {
        modifyFilesInput.addEventListener('change', function(e) {
            const newFiles = Array.from(e.target.files);

            if (getExistingFileCount() + modifyFilesArray.length + newFiles.length > 5) {
                alert("기존 이미지를 포함해 최대 5장까지만 첨부할 수 있습니다.");
                e.target.value = '';
                return;
            }

            modifyFilesArray = modifyFilesArray.concat(newFiles);
            e.target.value = '';
            renderModifyFiles();
        });
    }

    // 새 파일 렌더링
    function renderModifyFiles() {
        modifyFilePreview.innerHTML = '';
        modifyFilesArray.forEach((file, index) => {
            const div = document.createElement('div');
            div.className = 'flex justify-between items-center text-sm text-blue-700 bg-blue-50 px-3 py-2 rounded border border-blue-200';
            div.innerHTML = `
                <span class="truncate pr-4">[새 이미지] ${file.name}</span>
                <button type="button" class="text-red-500 font-bold hover:text-red-700 flex-shrink-0" onclick="removeModifyFile(${index})">X</button>
            `;
            modifyFilePreview.appendChild(div);
        });
    }

    // 새 파일 삭제
    window.removeModifyFile = function(index) {
        modifyFilesArray.splice(index, 1);
        renderModifyFiles();
    }

    modifyForm.addEventListener('submit', function(e) {
        e.preventDefault();

        const title = document.getElementById('title').value.trim();
        const content = document.getElementById('content').value.trim();

        if(!title) { alert('제목을 입력해주세요.'); return; }
        if(!content) { alert('내용을 입력해주세요.'); return; }

        const formData = new FormData(modifyForm);

        // 새로 누적한 파일들을 FormData에 수동 추가
        modifyFilesArray.forEach(file => {
            formData.append('files', file);
        });

        window.authFetch('/board/modify', {
            method: 'POST',
            body: formData
        })
            .then(async res => {
                if (res.ok) {
                    alert('게시글이 성공적으로 수정되었습니다.');
                    const boardId = document.getElementById('boardId').value;
                    const boardType = document.getElementById('boardType') ? document.getElementById('boardType').value : '';
                    location.href = '/board/read?boardId=' + boardId + (boardType ? '&boardType=' + boardType : '');
                } else {
                    const errorMessage = await res.text();
                    alert(errorMessage || '수정 중 오류가 발생했습니다.');
                }
            })
            .catch(error => { console.error(error); alert('서버와 통신 중 오류가 발생했습니다.'); });
    });
}


// ==========================================
// 4. REGISTER PAGE LOGIC (등록 페이지 전용)
// ==========================================
const registerForm = document.getElementById('registerForm');
if (registerForm) {
    if (!token) {
        if (confirm('로그인 후 이용 가능합니다.\n로그인 페이지로 이동하시겠습니까?')) {
            location.href = '/login';
        } else {
            location.href = '/board/list';
        }
    }

    let registerFiles = [];
    const fileInput = document.getElementById('files');
    const filePreview = document.getElementById('filePreview');

    // 파일 여러 번 선택하여 누적하기
    if (fileInput) {
        fileInput.addEventListener('change', function(e) {
            const newFiles = Array.from(e.target.files);

            const maxSize = 10 * 1024 * 1024; // 10MB 계산
            for (let file of newFiles) {
                if (file.size > maxSize) {
                    alert("10MB 이하의 이미지만 첨부 가능합니다: " + file.name);
                    e.target.value = '';
                    return;
                }
            }

            registerFiles = registerFiles.concat(newFiles);
            e.target.value = '';
            renderRegisterFiles();
        });
    }

    // 파일 목록 화면 렌더링
    function renderRegisterFiles() {
        if(!filePreview) return;
        filePreview.innerHTML = '';
        registerFiles.forEach((file, index) => {
            const div = document.createElement('div');
            div.className = 'flex justify-between items-center text-sm text-slate-700 bg-slate-100 px-3 py-2 rounded border border-slate-200';
            div.innerHTML = `
                <span class="truncate pr-4">${file.name}</span>
                <button type="button" class="text-red-500 font-bold hover:text-red-700 flex-shrink-0" onclick="removeRegisterFile(${index})">X</button>
            `;
            filePreview.appendChild(div);
        });
    }

    // 목록에서 파일 제거
    window.removeRegisterFile = function(index) {
        registerFiles.splice(index, 1);
        renderRegisterFiles();
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

        const formData = new FormData(registerForm);
        formData.delete('files');

        let boardTypeVal = document.getElementById('boardType') ? document.getElementById('boardType').value : '';
        if(boardTypeVal) {
            boardTypeVal = boardTypeVal.trim().toLowerCase();
            formData.set('boardType', boardTypeVal);
        }

        registerFiles.forEach(file => {
            formData.append('files', file);
        });

        window.authFetch('/board/register', {
            method: 'POST',
            body: formData
        })
            .then(async response => {
                if (response.redirected) {
                    location.href = response.url;
                } else if (response.status === 401 || response.status === 403) {
                    alert('로그인이 만료되었습니다. 다시 로그인해주세요.');
                    location.href = '/login';
                } else if (response.ok) {
                    alert('게시글이 등록되었습니다.');
                    location.href = boardType ? '/board/list?boardType=' + boardType : '/board/list';
                } else {
                    const errorMsg = await response.text();
                    alert(errorMsg || '게시글 등록에 실패했습니다.');
                }
            })
            .catch(error => { console.error(error); alert('서버 통신 오류'); });
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
function removePost(button) {
    const boardId = button.getAttribute("data-id");
    const boardType = button.getAttribute("data-boardtype");

    if (!confirm("정말 삭제하시겠습니까?")) return;

    window.authFetch('/board/remove', {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: new URLSearchParams({ boardId: boardId, boardType: boardType }).toString()
    })
        .then(async res => {
            if (res.ok) {
                alert('게시글이 삭제되었습니다.');
                if (boardType && boardType !== 'null' && boardType.trim() !== '') {
                    location.href = '/board/list?boardType=' + boardType;
                } else {
                    location.href = '/board/list';
                }
            } else {
                try {
                    const errorData = await res.json();
                    alert(errorData.message || '삭제 권한이 없거나 실패했습니다.');
                } catch(e) {
                    const errorMessage = await res.text();
                    alert(errorMessage || '삭제 권한이 없거나 실패했습니다.');
                }
            }
        })
        .catch(err => console.error(err));
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

let currentReplyPage = 1;
let currentReplySort = 'like';

function printReplies(page = 1) {
    const boardIdEl = document.getElementById('currentBoardId');
    const replyList = document.getElementById('replyList');
    if (!boardIdEl || !replyList) return;

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

                    let replyContentStr = '';
                    if (reply.blinded) {
                        replyContentStr = `
                            <p class="blind-text text-slate-400 italic">AI 세이프봇이 가린 부적절한 댓글입니다.</p>
                            <p class="real-text text-slate-600 break-all leading-relaxed" style="display:none;">${reply.replyText}</p>
                        `;
                    } else {
                        replyContentStr = `<p class="real-text text-slate-600 break-all leading-relaxed">${reply.replyText}</p>`;
                    }

                    if (!reply.blinded) {
                        const likeColor = reply.liked ? 'text-blue-600 font-bold' : 'text-slate-500';
                        btnStr += `<button onclick="likeReply(${reply.replyId})" class="text-xs ${likeColor} hover:underline mr-2">♥ ${reply.likeCount || 0}</button>`;

                        if (loggedInUserId) {
                            btnStr += `<button onclick="showReReplyForm(${reply.replyId})" class="text-xs text-blue-500 font-semibold hover:underline mr-2">답글</button>`;
                        }

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
                        <li class="reply-item py-3 border-b border-slate-100" data-blinded="${reply.blinded}">
                            <div class="flex justify-between items-center mb-1">
                                <div>
                                    <span class="font-bold text-sm text-slate-800">${reply.nickname || '익명'}</span>
                                    <span class="text-xs text-slate-400 ml-2">${reply.createdAt}</span>
                                </div>
                                <div>${btnStr}</div>
                            </div>
                            <div class="text-sm">${replyContentStr}</div>
                            
                            <!-- 대댓글 등록 폼 -->
                            <div id="rereply-form-${reply.replyId}" class="mt-3 pl-8 pb-2" style="display:none;">
                                <div class="flex gap-2">
                                    <input id="rereply-input-${reply.replyId}" class="input-field flex-1" type="text" style="font-size:12px; padding: 6px 12px;" placeholder="답글을 입력하세요">
                                    <button onclick="registerReReply(${reply.replyId})" class="btn-primary" style="padding:6px 12px; white-space:nowrap; background:#2563eb; color:white; border-radius:6px; font-weight:700; font-size:12px;">답글 등록</button>
                                </div>
                            </div>
                        </li>
                    `;

                    // 대댓글 렌더링
                    if (reply.children && reply.children.length > 0) {
                        reply.children.forEach(child => {
                            let childBtnStr = '';

                            let childContentStr = '';
                            if (child.blinded) {
                                childContentStr = `
                                    <p class="blind-text text-slate-400 italic">AI 세이프봇이 가린 부적절한 댓글입니다.</p>
                                    <p class="real-text text-slate-600 break-all leading-relaxed" style="display:none;">${child.replyText}</p>
                                `;
                            } else {
                                childContentStr = `<p class="real-text text-slate-600 break-all leading-relaxed">${child.replyText}</p>`;
                            }

                            if (!child.blinded) {
                                const childLikeColor = child.liked ? 'text-blue-600 font-bold' : 'text-slate-500';
                                childBtnStr += `<button onclick="likeReply(${child.replyId})" class="text-xs ${childLikeColor} hover:underline mr-2">♥ ${child.likeCount || 0}</button>`;

                                if (loggedInUserId && loggedInUserId != child.userId && loggedInUserRole !== 'ROLE_ADMIN') {
                                    const childReportColor = child.reported ? 'text-red-600 font-bold' : 'text-slate-500';
                                    childBtnStr += `<button onclick="reportReply(${child.replyId})" class="text-xs ${childReportColor} hover:underline mr-2">신고</button>`;
                                }
                            }

                            if (loggedInUserId == child.userId) {
                                const childSafeText = child.replyText.replace(/'/g, "\\'").replace(/"/g, '\\"');
                                childBtnStr += `<button onclick="modifyReplyPrompt(${child.replyId}, '${childSafeText}')" class="text-xs text-blue-500 font-semibold hover:underline mr-2">수정</button>`;
                            }

                            if (loggedInUserId == child.userId || loggedInUserRole === 'ROLE_ADMIN') {
                                childBtnStr += `<button onclick="removeReply(${child.replyId})" class="text-xs text-red-500 font-semibold hover:underline mr-2">삭제</button>`;
                            }

                            if (loggedInUserRole === 'ROLE_ADMIN') {
                                const childBlindBtnText = child.blinded ? '블라인드 해제' : '블라인드';
                                childBtnStr += `<button onclick="toggleReplyBlind(${child.replyId})" class="text-xs text-amber-600 font-semibold hover:underline">${childBlindBtnText}</button>`;
                            }

                            listStr += `
                                <li class="reply-item py-3 pl-8 border-b border-slate-100 bg-slate-50/50" data-blinded="${child.blinded}">
                                    <div class="flex justify-between items-center mb-1">
                                        <div>
                                            <span class="text-slate-400 mr-1">ㄴ</span>
                                            <span class="font-bold text-sm text-slate-800">${child.nickname || '익명'}</span>
                                            <span class="text-xs text-slate-400 ml-2">${child.createdAt}</span>
                                        </div>
                                        <div>${childBtnStr}</div>
                                    </div>
                                    <div class="text-sm pl-4">${childContentStr}</div>
                                </li>
                            `;
                        });
                    }
                });
            } else {
                listStr = '<div class="text-center py-10 text-slate-400 text-sm">등록된 댓글이 없습니다. 첫 댓글을 남겨보세요!</div>';
            }

            replyList.innerHTML = listStr;

            if (typeof applyReplySafebot === 'function') {
                applyReplySafebot();
            }

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

document.addEventListener("DOMContentLoaded", function() {
    const currentBoardIdEl = document.getElementById('currentBoardId');
    if (currentBoardIdEl) {
        const boardId = currentBoardIdEl.value;

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

        printReplies(1);
    }
});

window.safebotStatus = localStorage.getItem('safebot') !== 'OFF' ? 'ON' : 'OFF';

document.addEventListener('DOMContentLoaded', function() {
    const replySwitch = document.getElementById('replySafebotSwitch');
    if (!replySwitch) return;

    // 1. 초기 로드 시 브라우저에 저장된 상태 반영
    replySwitch.checked = (window.safebotStatus === 'ON');

    // 2. 스위치를 켜고 끌 때마다 상태를 변경하고 화면을 새로 정렬
    replySwitch.addEventListener('change', function() {
        window.safebotStatus = this.checked ? 'ON' : 'OFF';
        localStorage.setItem('safebot', window.safebotStatus);
        applyReplySafebot(); // 화면 가림막 업데이트
    });
});

function applyReplySafebot() {
    const isOn = (window.safebotStatus === 'ON');

    document.querySelectorAll('.reply-item').forEach(item => {
        const isBlinded = item.getAttribute('data-blinded') === 'true';

        if (isBlinded) {
            const blindText = item.querySelector('.blind-text');
            const realText = item.querySelector('.real-text');

            if (isOn) {
                if (blindText) blindText.style.display = 'block';
                if (realText) realText.style.display = 'none';
            } else {
                if (blindText) blindText.style.display = 'none';
                if (realText) realText.style.display = 'block';
            }
        }
    });
}

function showReReplyForm(replyId) {
    const form = document.getElementById(`rereply-form-${replyId}`);
    if (form) {
        form.style.display = form.style.display === 'none' ? 'block' : 'none';
        const input = document.getElementById(`rereply-input-${replyId}`);
        if (input && form.style.display === 'block') {
            input.focus();
        }
    }
}

function registerReReply(parentId) {
    const token = localStorage.getItem('accessToken');
    const loggedInUserId = document.getElementById('loggedInUserId')?.value;
    const boardIdEl = document.getElementById('currentBoardId');
    if (!boardIdEl) return;
    const boardId = boardIdEl.value;

    if (!token || !loggedInUserId) {
        if(confirm('로그인 후 이용 가능합니다.\n로그인 페이지로 이동하시겠습니까?')) location.href='/login';
        return;
    }

    const input = document.getElementById(`rereply-input-${parentId}`);
    if (!input) return;
    const text = input.value.trim();
    if (text === '') { alert('답글 내용을 입력해 주세요.'); input.focus(); return; }

    window.authFetch('/replies/', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ boardId: boardId, replyText: text, userId: loggedInUserId, parentId: parentId })
    }).then(async res => {
        if(res.ok) {
            input.value = '';
            printReplies(currentReplyPage);
        } else {
            const msg = await res.text();
            alert(msg || '답글 등록에 실패했습니다.');
        }
    }).catch(err => console.error('답글 등록 에러:', err));
}