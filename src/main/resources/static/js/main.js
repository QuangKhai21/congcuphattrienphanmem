// Pet Care — main UI scripts
// Avatar URL của user đã đăng nhập (được inject từ server)
var CURRENT_USER_AVATAR = '';
// Avatar URL của Nekomimi AI (lấy từ nút FAB)
var NEKOMIMI_AVATAR = '';

/** Đường dẫn tương đối (uploads/...) trên trang như /vet-qa sẽ thành URL sai — ép về /uploads/... */
function normalizeAvatarSrc(url) {
    if (url == null || url === '') return '';
    var u = String(url).trim();
    if (!u) return '';
    if (/^https?:\/\//i.test(u) || u.indexOf('//') === 0) return u;
    u = u.replace(/\\/g, '/');
    if (/^[A-Za-z]:\//.test(u)) {
        var idx = u.toLowerCase().indexOf('/uploads/');
        return idx >= 0 ? u.substring(idx) : '';
    }
    if (u.toLowerCase().indexOf('file:') === 0) {
        var j = u.toLowerCase().indexOf('/uploads/');
        return j >= 0 ? u.substring(j) : '';
    }
    if (u.charAt(0) !== '/') u = '/' + u;
    var low = u.toLowerCase();
    if (low.indexOf('/uploads/') === 0 || low.indexOf('/images/') === 0) return u;
    return '';
}

document.addEventListener('DOMContentLoaded', function () {
    // Tìm avatar trong navbar nếu có
    var navAvatar = document.querySelector('.navbar .user-avatar img, .nav-avatar img, .avatar-circle img');
    if (navAvatar && navAvatar.src) {
        var navSrc = navAvatar.getAttribute('src') || '';
        CURRENT_USER_AVATAR = /^https?:\/\//i.test(navSrc)
            ? navSrc.trim()
            : (normalizeAvatarSrc(navSrc) || navAvatar.src);
    } else {
        // Thử lấy từ hidden input
        var hiddenAvatar = document.querySelector('input[name="userAvatarUrl"]');
        if (hiddenAvatar) {
            CURRENT_USER_AVATAR = normalizeAvatarSrc(hiddenAvatar.value);
        }
    }

    // Lấy avatar Nekomimi từ nút FAB (hoặc data attribute trên root)
    var fabAvatar = document.querySelector('#nekomimi-chat-fab img.nekomimi-chat-fab-avatar');
    if (fabAvatar && fabAvatar.src) {
        NEKOMIMI_AVATAR = fabAvatar.src;
    } else {
        var nekoRoot = document.getElementById('nekomimi-chat-root');
        var rel = nekoRoot && nekoRoot.getAttribute('data-nekomimi-avatar');
        if (rel) {
            try {
                NEKOMIMI_AVATAR = new URL(rel, window.location.origin).href;
            } catch (e) {
                NEKOMIMI_AVATAR = rel;
            }
        }
    }

    if (typeof bootstrap !== 'undefined' && bootstrap.Alert) {
        document.querySelectorAll('.alert-dismissible').forEach(function (alert) {
            setTimeout(function () {
                var bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
                bsAlert.close();
            }, 5000);
        });
    }

    var topBtn = document.createElement('button');
    topBtn.type = 'button';
    topBtn.className = 'btn-back-top';
    topBtn.setAttribute('aria-label', 'Lên đầu trang');
    topBtn.innerHTML = '\u2191';
    topBtn.style.fontSize = '1.25rem';
    topBtn.style.lineHeight = '1';
    document.body.appendChild(topBtn);

    function toggleTop() {
        if (window.scrollY > 320) {
            topBtn.classList.add('visible');
        } else {
            topBtn.classList.remove('visible');
        }
    }

    topBtn.addEventListener('click', function () {
        window.scrollTo({ top: 0, behavior: 'smooth' });
    });

    window.addEventListener('scroll', toggleTop, { passive: true });
    toggleTop();

    // ══════════════════════════════════════════════════════
    // Nekomimi Chat — multi-conversation (AI + vet threads)
    // ══════════════════════════════════════════════════════
    (function initNekomimiChat() {
        var root       = document.getElementById('nekomimi-chat-root');
        if (!root) return;

        var fab        = document.getElementById('nekomimi-chat-fab');
        var panel      = document.getElementById('nekomimi-chat-panel');
        var messagesEl = document.getElementById('nekomimi-chat-messages');
        var input      = document.getElementById('nekomimi-chat-input');
        var sendBtn    = document.getElementById('nekomimi-chat-send');
        var toggleRight = document.getElementById('nekomimi-chat-toggle-right');
        var iconOpen   = fab ? fab.querySelector('.nekomimi-chat-fab-icon-open') : null;
        var iconClose  = fab ? fab.querySelector('.nekomimi-chat-fab-icon-close') : null;
        var threadList = document.getElementById('chatThreadList');
        var fabBadge  = document.getElementById('nekomimi-fab-badge');

        var totalUnread = 0;  // tổng tin nhắn chưa đọc từ vet/private

        function updateFabBadge() {
            if (!fabBadge) return;
            if (totalUnread <= 0) {
                fabBadge.classList.add('d-none');
                fabBadge.textContent = '0';
            } else {
                fabBadge.classList.remove('d-none');
                fabBadge.textContent = totalUnread > 99 ? '99+' : totalUnread;
            }
        }

        if (!fab || !panel || !messagesEl) {
            console.warn('[NekomimiChat] Missing core DOM elements — fab:', fab, 'panel:', panel, 'messagesEl:', messagesEl);
            return; // Không chạy gì cả nếu thiếu phần tử cốt lõi
        }

        // ── State ────────────────────────────────────────────
        // currentThread: { type: 'ai'|'vet'|'private', convId, vetId, otherUserId, otherUserName, otherUserAvatar }
        var currentThread = null;
        var csrfEl        = document.querySelector('input[name="_csrf"]');
        var csrfToken     = csrfEl ? csrfEl.value : '';
        var csrfParam     = csrfEl ? '_csrf=' + encodeURIComponent(csrfToken) : '';
        var sessions      = {};    // convId → array of rendered msg ids (dedup)
        var aiConvId      = null;  // AI conversation ID (lazy-created on first send/open)
        // Current logged-in user ID (for private chat sender detection)
        var CURRENT_USER_ID = (function () {
            var el = document.getElementById('current-user-id');
            return el && el.value ? parseInt(el.value, 10) : null;
        })();

        // ── Helpers ──────────────────────────────────────────
        function formatTime(d) {
            return d.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit', hour12: false });
        }

        function escHtml(s) {
            if (!s) return '';
            var d = document.createElement('div');
            d.textContent = s;
            return d.innerHTML;
        }

        function msgId(type, id) { return type + '-' + id; }

        function isRendered(type, id) {
            return !!document.getElementById(msgId(type, id));
        }

        function scrollBottom() {
            messagesEl.scrollTop = messagesEl.scrollHeight;
        }

        // ── Thread list management ────────────────────────────
        /**
         * Thêm thread vào sidebar. Loại 'vet' hoặc 'private'.
         */
        function addUserThread(convId, userId, userName, userSpec, userAvatarUrl, threadType) {
            if (document.querySelector('[data-convid="' + convId + '"]')) return;
            userAvatarUrl = userAvatarUrl ? normalizeAvatarSrc(userAvatarUrl) : '';
            var li = document.createElement('li');
            var avatarInner = userAvatarUrl
                ? '<img src="' + escHtml(userAvatarUrl) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                : '<i class="bi bi-person"></i>';
            li.innerHTML =
                '<button type="button" class="nekomimi-chat-thread" data-thread="' + threadType + '"' +
                ' data-convid="' + convId + '" data-userid="' + userId + '">' +
                '  <span class="nekomimi-chat-thread-avatar" aria-hidden="true">' + avatarInner + '</span>' +
                '  <span class="nekomimi-chat-thread-meta">' +
                '    <span class="nekomimi-chat-thread-name">' + escHtml(userName) + '</span>' +
                '    <span class="nekomimi-chat-thread-preview text-muted small">' +
                       escHtml(userSpec || (threadType === 'vet' ? 'Bác sĩ thú y' : 'Người dùng')) + '</span>' +
                '  </span>' +
                '</button>';
            threadList.appendChild(li);
            var btn = li.querySelector('button');
            if (userAvatarUrl) {
                btn.setAttribute('data-avatar-url', userAvatarUrl);
            }
            btn.addEventListener('click', function () {
                var url = btn.getAttribute('data-avatar-url');
                switchThread(threadType, convId, userId, userName, userSpec, url || null);
            });
        }

        function selectThread(btn) {
            document.querySelectorAll('.nekomimi-chat-thread').forEach(function (b) {
                b.classList.remove('is-active');
            });
            if (btn) btn.classList.add('is-active');
        }

        // ── Header / right-panel sync ─────────────────────────
        function updateHeader(type, title, sub, avatarHtml) {
            var t = document.getElementById('chatHeaderTitle');
            var s = document.getElementById('chatHeaderSub');
            var a = document.getElementById('chatHeaderAvatar');
            if (t) t.textContent = title || '';
            if (s) s.textContent = sub || '';
            if (a) a.innerHTML = avatarHtml || getNekomimiAvatarHtml();
        }

        function updateProfile(type, name, bio, verified, vetAvatarUrl) {
            var a = document.getElementById('chatProfileAvatar');
            var n = document.getElementById('chatProfileName');
            var b = document.querySelector('.nekomimi-chat-profile-bio');
            if (a) {
                if (type === 'ai') {
                    a.innerHTML = getNekomimiAvatarHtml();
                } else if (vetAvatarUrl) {
                    a.innerHTML = '<img src="' + escHtml(vetAvatarUrl) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
                } else {
                    a.innerHTML = '<i class="bi bi-person-badge"></i>';
                }
            }
            if (n) {
                n.innerHTML = escHtml(name) + ' ';
                if (verified) n.innerHTML += verified;
            }
            if (b) b.textContent = bio || '';
        }

        /** Nền khung chat = ảnh đại diện người đang chat (AI / bác sĩ) */
        function applyThreadTheme(avatarUrl) {
            var bg = document.getElementById('nekomimiChatMessagesBg');
            if (!bg) return;
            var u = avatarUrl && normalizeAvatarSrc(avatarUrl);
            if (u) {
                bg.style.backgroundImage = 'url(' + JSON.stringify(u) + ')';
            } else {
                bg.style.backgroundImage = 'none';
            }
        }

        // ── Switch thread ────────────────────────────────────
        function switchThread(type, convId, vetId, vetName, vetSpec, vetAvatarUrl) {
            if (type === 'vet' || type === 'private') {
                vetAvatarUrl = normalizeAvatarSrc(vetAvatarUrl) || null;
            }
            currentThread = {
                type: type,
                convId: convId,
                vetId: (type === 'vet' ? vetId : null),
                otherUserId: (type === 'private' ? vetId : null),
                otherUserName: (type === 'private' ? vetName : null),
                otherUserAvatar: (type === 'private' ? vetAvatarUrl : null),
                // Luôn lưu avatar bác sĩ cho thread VET — loadVetMessages / bubble fallback cần field này
                vetAvatarUrl: (type === 'vet' ? vetAvatarUrl : null)
            };

            var btn = type === 'ai'
                ? document.querySelector('[data-thread="ai"]')
                : document.querySelector('[data-convid="' + convId + '"]');
            selectThread(btn);

            if (type === 'ai') {
                var aiUrl = (btn && btn.getAttribute('data-avatar-url')) || NEKOMIMI_AVATAR;
                applyThreadTheme(aiUrl);
            } else {
                applyThreadTheme(vetAvatarUrl || null);
            }

            messagesEl.innerHTML = '';
            sessions = {};

            if (type === 'ai') {
                updateHeader(type, 'Nekomimi AI', 'Trợ lý Pet Care', getNekomimiAvatarHtml());
                updateProfile(type, 'Nekomimi AI',
                    'Hỗ trợ gợi ý chăm sóc thú cưng, gợi ý dùng dịch vụ. Không thay thế bác sĩ thú y.',
                    '<i class="bi bi-patch-check-fill nekomimi-chat-verified" title="Trợ lý chính thức"></i>');
                loadAIMessages(aiConvId);

                if (sendBtn) {
                    sendBtn.innerHTML = '<i class="bi bi-stars"></i>';
                    sendBtn.title = 'Gửi cho AI';
                }
            } else if (type === 'private') {
                // ── Private user chat ──────────────────────────────
                var privAvatarHtml = vetAvatarUrl
                    ? '<img src="' + escHtml(vetAvatarUrl) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                    : '<i class="bi bi-person"></i>';
                updateHeader('private', vetName || 'Người dùng', 'Chat riêng tư', privAvatarHtml);
                updateProfile('private', vetName || 'Người dùng',
                    'Cuộc trò chuyện riêng tư giữa hai tài khoản.',
                    '', vetAvatarUrl || null);
                loadPrivateMessages(convId);

                if (sendBtn) {
                    sendBtn.innerHTML = '<i class="bi bi-send"></i>';
                    sendBtn.title = 'Gửi tin nhắn';
                }
            } else {
                // ── Vet chat ─────────────────────────────────────
                var headerAv = vetAvatarUrl
                    ? '<img src="' + escHtml(vetAvatarUrl) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                    : '<i class="bi bi-person-badge"></i>';
                updateHeader(type, vetName || 'Bác sĩ',
                    vetSpec || 'Bác sĩ thú y',
                    headerAv);
                updateProfile(type, vetName || 'Bác sĩ',
                    'Tư vấn riêng tư giữa khách hàng và bác sĩ thú y.',
                    '<i class="bi bi-patch-check-fill nekomimi-chat-verified" title="Cuộc trò chuyện tư vấn"></i>',
                    vetAvatarUrl || null);
                // Đánh dấu đã đọc + trừ badge
                if (totalUnread > 0) { totalUnread -= 1; updateFabBadge(); }
                fetch('/api/conversations/' + convId + '/read?' + csrfParam, {
                    method: 'POST',
                    credentials: 'same-origin'
                }).catch(function() {});
                loadVetMessages(convId);

                if (sendBtn) {
                    sendBtn.innerHTML = '<i class="bi bi-send"></i>';
                    sendBtn.title = 'Gửi tin nhắn';
                }
            }
        }

        // ── Bubble rendering ─────────────────────────────────
        // ── Avatar helpers ───────────────────────────────
        function getNekomimiAvatarHtml() {
            if (NEKOMIMI_AVATAR) {
                return '<img src="' + escHtml(NEKOMIMI_AVATAR) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
            }
            return '<i class="bi bi-stars"></i>';
        }

        function getUserAvatarHtml() {
            if (CURRENT_USER_AVATAR) {
                return '<img src="' + escHtml(CURRENT_USER_AVATAR) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
            }
            return '<i class="bi bi-person"></i>';
        }

        function getAvatarHtml(avatarUrl, fallbackIcon, isCurrentUser) {
            if (isCurrentUser) {
                return getUserAvatarHtml();
            }
            var n = normalizeAvatarSrc(avatarUrl);
            if (n) {
                return '<img src="' + escHtml(n) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
            }
            return fallbackIcon || '<i class="bi bi-person"></i>';
        }

        function appendBubble(role, text, msgIdStr, opts) {
            opts = opts || {};
            var row = document.createElement('div');
            row.className = 'nekomimi-chat-row ' + (role === 'user' ? 'outgoing' : 'incoming');
            row.id = msgIdStr || ('gen-' + Date.now());

            // Hiển thị avatar cho tất cả mọi người (kể cả user)
            if (role !== 'user' || CURRENT_USER_AVATAR) {
                var av = document.createElement('span');
                av.className = 'nekomimi-chat-bubble-avatar';
                av.setAttribute('aria-hidden', 'true');
                if (role === 'user') {
                    av.innerHTML = getAvatarHtml(CURRENT_USER_AVATAR, '<i class="bi bi-person"></i>', true);
                } else if (opts.avatarHtml) {
                    av.innerHTML = opts.avatarHtml;
                } else {
                    av.innerHTML = getAvatarHtml(opts.senderAvatar, '<i class="bi bi-person-badge"></i>', false);
                }
                row.appendChild(av);
            }

            var bubble = document.createElement('div');
            bubble.className = 'nekomimi-chat-bubble';
            if (opts.isHtml) {
                bubble.innerHTML = text;
            } else {
                bubble.textContent = text;
            }
            row.appendChild(bubble);

            if (role !== 'user' && opts.withFeedback) {
                var fb = document.createElement('div');
                fb.className = 'nekomimi-chat-feedback';
                fb.innerHTML =
                    '<button type="button" title="Hữu ích" aria-label="Hữu ích"><i class="bi bi-hand-thumbs-up"></i></button>' +
                    '<button type="button" title="Chưa tốt" aria-label="Chưa tốt"><i class="bi bi-hand-thumbs-down"></i></button>';
                bubble.appendChild(fb);
            }

            messagesEl.appendChild(row);
            scrollBottom();
        }

        function appendTimeRow(text) {
            var div = document.createElement('div');
            div.className = 'nekomimi-chat-time';
            div.textContent = text || formatTime(new Date());
            messagesEl.appendChild(div);
        }

        // ── Load AI messages from API ──────────────────────────
        function loadAIMessages(convId) {
            appendTimeRow('Đang tải...');
            var url = '/api/ai/messages?' + csrfParam;
            if (convId) url += '&conversationId=' + convId;
            fetch(url, {
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json' }
            }).then(function (r) {
                if (!r.ok) throw new Error(r.status);
                return r.json();
            }).then(function (msgs) {
                messagesEl.innerHTML = '';
            if (!msgs.length) {
                // Welcome when no history
                appendTimeRow();
                appendBubble('ai',
                    'Xin chào! Mình là <strong>Nekomimi AI</strong>, trợ lý thú cưng của Pet Care.',
                    null, { isHtml: true, withFeedback: true,
                            avatarHtml: getNekomimiAvatarHtml() });
                appendBubble('ai',
                    '<p class="mb-2">Mình có thể gợi ý nhanh về:</p>' +
                    '<ol class="mb-0 ps-3 small"><li>Chăm sóc &amp; theo dõi sức khỏe</li>' +
                    '<li>Điều hướng tới Lịch hẹn / Tìm thú lạc / Bác sĩ</li>' +
                    '<li>Gợi ý dùng dịch vụ quanh bạn</li></ol>',
                    null, { isHtml: true, avatarHtml: getNekomimiAvatarHtml() });
                    return;
                }
                // Save conversation ID from first message
                if (msgs[0] && msgs[0].conversationId) aiConvId = msgs[0].conversationId;
                appendTimeRow(formatTime(new Date(msgs[0].sentAt)));
                msgs.forEach(function (m) {
                    var role = m.senderType === 'USER' ? 'user' : 'incoming';
                    var avatarHtml;
                    if (normalizeAvatarSrc(m.senderAvatar)) {
                        avatarHtml = '<img src="' + escHtml(normalizeAvatarSrc(m.senderAvatar)) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
                    } else if (m.senderType === 'AI') {
                        avatarHtml = getNekomimiAvatarHtml();
                    } else {
                        avatarHtml = getUserAvatarHtml();
                    }
                    appendBubble(role, m.content, msgId('ai', m.id),
                        { isHtml: m.senderType === 'AI', withFeedback: false, avatarHtml: avatarHtml, senderAvatar: m.senderAvatar });
                });
            }).catch(function () {
                messagesEl.innerHTML = '';
                appendTimeRow();
                appendBubble('ai',
                    'Không tải được tin nhắn. Vui lòng thử lại.',
                    null, { isHtml: true, withFeedback: false,
                            avatarHtml: getNekomimiAvatarHtml() });
            });
        }

        // ── Load vet messages from API ────────────────────────
        function loadVetMessages(convId) {
            appendTimeRow('Đang tải tin nhắn...');
            fetch('/api/conversations/' + convId + '/messages?' + csrfParam, {
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json' }
            }).then(function (r) {
                if (!r.ok) throw new Error(r.status);
                return r.json();
            }).then(function (msgs) {
                messagesEl.innerHTML = '';
                if (!msgs.length) {
                    appendTimeRow();
                    var vetEmptyAv = currentThread.vetAvatarUrl
                        ? '<img src="' + escHtml(currentThread.vetAvatarUrl) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                        : '<i class="bi bi-person-badge"></i>';
                    appendBubble('ai',
                        'Chưa có tin nhắn nào. Hãy gửi lời chào để bắt đầu trò chuyện nhé!',
                        null, { isHtml: true, withFeedback: false, avatarHtml: vetEmptyAv });
                    return;
                }
                appendTimeRow(formatTime(new Date(msgs[0].sentAt)));
                msgs.forEach(function (m) {
                    // Căn trái/phải theo người gửi (bác sĩ đăng nhập: tin của mình bên phải, khách bên trái)
                    var mine = CURRENT_USER_ID != null && m.senderId === CURRENT_USER_ID;
                    var role = mine ? 'user' : 'incoming';
                    var avatarHtml;
                    if (normalizeAvatarSrc(m.senderAvatar)) {
                        avatarHtml = '<img src="' + escHtml(normalizeAvatarSrc(m.senderAvatar)) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
                    } else if (m.senderType === 'VET') {
                        avatarHtml = currentThread.vetAvatarUrl
                            ? '<img src="' + escHtml(currentThread.vetAvatarUrl) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                            : '<i class="bi bi-person-badge"></i>';
                    } else if (m.senderType === 'AI') {
                        avatarHtml = '<i class="bi bi-stars"></i>';
                    } else if (currentThread.vetAvatarUrl) {
                        avatarHtml = '<img src="' + escHtml(currentThread.vetAvatarUrl) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
                    } else {
                        avatarHtml = '<i class="bi bi-person"></i>';
                    }
                    appendBubble(role, m.content, msgId('vet', m.id),
                        { isHtml: false, withFeedback: false, avatarHtml: avatarHtml, senderAvatar: m.senderAvatar });
                });
            }).catch(function () {
                messagesEl.innerHTML = '';
                appendTimeRow();
                appendBubble('ai',
                    'Không tải được tin nhắn. Vui lòng thử lại.',
                    null, { isHtml: true, withFeedback: false,
                            avatarHtml: '<i class="bi bi-person-badge"></i>' });
            });
        }

        // ── Send vet message ─────────────────────────────────
        function sendVetMessage(convId, text) {
            appendTimeRow();
            // Hiển thị với avatar user thực
            appendBubble('user', text, null, {
                isHtml: false, withFeedback: false,
                avatarHtml: CURRENT_USER_AVATAR
                    ? '<img src="' + escHtml(CURRENT_USER_AVATAR) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                    : '<i class="bi bi-person"></i>'
            });
            sendBtn.disabled = true;

            fetch('/api/conversations/' + convId + '/messages?' + csrfParam, {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json'
                },
                body: 'content=' + encodeURIComponent(text)
            }).then(function (r) {
                if (!r.ok) throw new Error(r.status);
                return r.json();
            }).then(function (msg) {
                // Tin của chính mình đã append lúc gửi — không append lại (USER hay VET role đều có thể là mình)
                var mine = CURRENT_USER_ID != null && msg.senderId === CURRENT_USER_ID;
                if (mine) {
                    var rows = messagesEl.querySelectorAll('.nekomimi-chat-row.outgoing');
                    var lastOutgoing = rows[rows.length - 1];
                    if (lastOutgoing && !lastOutgoing.id) {
                        lastOutgoing.id = msgId('vet', msg.id);
                    }
                    sendBtn.disabled = false;
                    if (input) input.focus();
                    return;
                }
                // Tin từ đối tác
                var avatarHtml;
                if (normalizeAvatarSrc(msg.senderAvatar)) {
                    avatarHtml = '<img src="' + escHtml(normalizeAvatarSrc(msg.senderAvatar)) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
                } else if (currentThread.vetAvatarUrl) {
                    avatarHtml = '<img src="' + escHtml(currentThread.vetAvatarUrl) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
                } else {
                    avatarHtml = '<i class="bi bi-person-badge"></i>';
                }
                // Tăng badge nếu panel đang đóng hoặc không phải thread đang mở
                var isActive = isPanelOpen() && currentThread && currentThread.type === 'vet' && currentThread.convId == convId;
                if (!isActive) {
                    totalUnread += 1;
                    updateFabBadge();
                }
                appendTimeRow();
                appendBubble('incoming', msg.content, msgId('vet', msg.id),
                    { isHtml: false, withFeedback: true, avatarHtml: avatarHtml, senderAvatar: msg.senderAvatar });
                sendBtn.disabled = false;
                if (input) input.focus();
            }).catch(function () {
                appendTimeRow();
                appendBubble('ai',
                    'Gửi thất bại. Vui lòng thử lại.',
                    null, { isHtml: true, withFeedback: false,
                            avatarHtml: '<i class="bi bi-person-badge"></i>' });
                sendBtn.disabled = false;
            });
        }

        // ── Send private user message ───────────────────────────
        function sendPrivateMessage(convId, text) {
            appendTimeRow();
            appendBubble('user', text, null, {
                isHtml: false, withFeedback: false,
                avatarHtml: CURRENT_USER_AVATAR
                    ? '<img src="' + escHtml(CURRENT_USER_AVATAR) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                    : '<i class="bi bi-person"></i>'
            });
            sendBtn.disabled = true;

            fetch('/api/conversations/' + convId + '/messages?' + csrfParam, {
                method: 'POST',
                credentials: 'same-origin',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Accept': 'application/json'
                },
                body: 'content=' + encodeURIComponent(text)
            }).then(function (r) {
                if (!r.ok) throw new Error(r.status);
                return r.json();
            }).then(function (msg) {
                // USER đã được append lúc gửi — không append lại (tránh nhân đôi)
                if (msg.senderType === 'USER') {
                    var rows = messagesEl.querySelectorAll('.nekomimi-chat-row.outgoing');
                    var lastOutgoing = rows[rows.length - 1];
                    if (lastOutgoing && !lastOutgoing.id) {
                        lastOutgoing.id = msgId('pvt', msg.id);
                    }
                    sendBtn.disabled = false;
                    if (input) input.focus();
                    return;
                }
                // Tin nhắn từ người kia
                var avatarHtml;
                if (normalizeAvatarSrc(msg.senderAvatar)) {
                    avatarHtml = '<img src="' + escHtml(normalizeAvatarSrc(msg.senderAvatar)) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
                } else if (currentThread && currentThread.otherUserAvatar) {
                    avatarHtml = '<img src="' + escHtml(currentThread.otherUserAvatar) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
                } else {
                    avatarHtml = '<i class="bi bi-person"></i>';
                }
                var isActive = isPanelOpen() && currentThread && currentThread.type === 'private' && currentThread.convId == convId;
                if (!isActive) {
                    totalUnread += 1;
                    updateFabBadge();
                }
                appendTimeRow();
                appendBubble('incoming', msg.content, msgId('pvt', msg.id),
                    { isHtml: false, withFeedback: false, avatarHtml: avatarHtml, senderAvatar: msg.senderAvatar });
                sendBtn.disabled = false;
                if (input) input.focus();
            }).catch(function () {
                appendTimeRow();
                appendBubble('ai',
                    'Gửi thất bại. Vui lòng thử lại.',
                    null, { isHtml: true, withFeedback: false,
                            avatarHtml: '<i class="bi bi-person"></i>' });
                sendBtn.disabled = false;
            });
        }

        // ── Load private user messages ─────────────────────────
        function loadPrivateMessages(convId) {
            appendTimeRow('Đang tải tin nhắn...');
            fetch('/api/conversations/' + convId + '/messages?' + csrfParam, {
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json' }
            }).then(function (r) {
                if (!r.ok) throw new Error(r.status);
                return r.json();
            }).then(function (msgs) {
                messagesEl.innerHTML = '';
                if (!msgs.length) {
                    appendTimeRow();
                    var avHtml = currentThread && currentThread.otherUserAvatar
                        ? '<img src="' + escHtml(currentThread.otherUserAvatar) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                        : '<i class="bi bi-person"></i>';
                    appendBubble('ai',
                        'Chưa có tin nhắn nào. Hãy gửi lời chào để bắt đầu trò chuyện nhé!',
                        null, { isHtml: true, withFeedback: false, avatarHtml: avHtml });
                    return;
                }
                appendTimeRow(formatTime(new Date(msgs[0].sentAt)));
                msgs.forEach(function (m) {
                    var role = m.senderId === CURRENT_USER_ID ? 'user' : 'incoming';
                    var avatarHtml;
                    if (role === 'user') {
                        avatarHtml = CURRENT_USER_AVATAR
                            ? '<img src="' + escHtml(CURRENT_USER_AVATAR) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                            : '<i class="bi bi-person"></i>';
                    } else {
                        var pav = normalizeAvatarSrc(m.senderAvatar);
                        avatarHtml = pav
                            ? '<img src="' + escHtml(pav) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                            : (currentThread && currentThread.otherUserAvatar
                                ? '<img src="' + escHtml(currentThread.otherUserAvatar) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                                : '<i class="bi bi-person"></i>');
                    }
                    appendBubble(role, m.content, msgId('pvt', m.id),
                        { isHtml: false, withFeedback: false, avatarHtml: avatarHtml, senderAvatar: m.senderAvatar });
                });
            }).catch(function () {
                messagesEl.innerHTML = '';
                appendTimeRow();
                appendBubble('ai',
                    'Không tải được tin nhắn. Vui lòng thử lại.',
                    null, { isHtml: true, withFeedback: false,
                            avatarHtml: '<i class="bi bi-person"></i>' });
            });
        }

        // ── Load user's conversations from API ───────────────
        function loadUserConversations() {
            fetch('/api/conversations?' + csrfParam, {
                credentials: 'same-origin',
                headers: { 'Accept': 'application/json' }
            }).then(function (r) {
                if (!r.ok || r.status === 401) return [];
                return r.json();
            }).then(function (convs) {
                totalUnread = 0;
                convs.forEach(function (c) {
                    totalUnread += (c.unreadCount > 0 ? c.unreadCount : 0);
                    if (c.type === 'VET') {
                        var oid = c.otherUserId != null ? c.otherUserId : c.vetId;
                        var oname = c.otherUserName != null ? c.otherUserName : c.vetName;
                        var ospec = c.otherUserSpec != null ? c.otherUserSpec : c.vetSpec;
                        var oav = c.otherUserAvatar !== undefined ? c.otherUserAvatar : c.vetAvatar;
                        addUserThread(c.id, oid, oname, ospec, oav, 'vet');
                    } else if (c.type === 'PRIVATE') {
                        addUserThread(c.id, c.otherUserId, c.otherUserName, null, c.otherUserAvatar, 'private');
                    }
                });
                updateFabBadge();
            }).catch(function () { /* silent */ });
        }

        // ── Send user message (dispatch AI vs Vet) ────────────
        function sendUserMessage() {
            var text = (input.value || '').trim();
            if (!text) return;
            input.value = '';

            if (!currentThread || currentThread.type === 'ai') {
                appendTimeRow();
                appendBubble('user', text, null, {
                    isHtml: false, withFeedback: false,
                    avatarHtml: CURRENT_USER_AVATAR
                        ? '<img src="' + escHtml(CURRENT_USER_AVATAR) + '" alt="" class="chat-avatar-img" loading="lazy"/>'
                        : '<i class="bi bi-person"></i>'
                });
                sendBtn.disabled = true;

                fetch('/api/ai/chat?' + csrfParam, {
                    method: 'POST',
                    credentials: 'same-origin',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                        'Accept': 'application/json'
                    },
                    body: 'message=' + encodeURIComponent(text)
                }).then(function (r) {
                    if (!r.ok) throw new Error(r.status);
                    return r.json();
                }).then(function (resp) {
                    // Save AI conversation ID for future loads
                    if (resp.conversationId) aiConvId = resp.conversationId;
                    appendTimeRow();
                    var avatarHtml = getNekomimiAvatarHtml();
                    if (normalizeAvatarSrc(resp.senderAvatar)) {
                        avatarHtml = '<img src="' + escHtml(normalizeAvatarSrc(resp.senderAvatar)) + '" alt="" class="chat-avatar-img" loading="lazy"/>';
                    }
                    appendBubble('ai', resp.content, msgId('ai', resp.id), { isHtml: true, withFeedback: true, avatarHtml: avatarHtml });
                    sendBtn.disabled = false;
                    if (input) input.focus();
                }).catch(function () {
                    appendTimeRow();
                    appendBubble('ai',
                        'Xin lỗi, mình chưa trả lời được. Bạn thử lại hoặc hỏi bác sĩ tại <a href="/vet-qa">đây</a> nhé!',
                        null, { isHtml: true, withFeedback: false, avatarHtml: getNekomimiAvatarHtml() });
                    sendBtn.disabled = false;
                });
            } else if (currentThread.type === 'vet') {
                // ─ Vet chat (API)
                sendVetMessage(currentThread.convId, text);
            } else if (currentThread.type === 'private') {
                // ─ Private user chat (API)
                sendPrivateMessage(currentThread.convId, text);
            }
        }

        // ── Panel open/close ─────────────────────────────────
        // Luôn toggle bằng class 'is-open', KHÔNG phụ thuộc attribute 'hidden' trên panel
        // (attribute có thể bị browser/extension thay đổi → gây lỗi click).
        function isPanelOpen() {
            // Rõ ràng: class 'is-open' trên root là nguồn truth duy nhất.
            return root && root.classList.contains('is-open');
        }

        function setOpen(open) {
            if (!fab || !panel) {
                if (typeof console !== 'undefined') {
                    console.warn('[NekomimiChat] fab or panel not found in DOM. fab=', fab, 'panel=', panel);
                }
                if (fab) fab.onclick = function () { setOpen(!isPanelOpen()); };
                return;
            }
            root.classList.toggle('is-open', open);
            fab.setAttribute('aria-expanded', open ? 'true' : 'false');
            if (open) {
                panel.removeAttribute('hidden');
                if (iconOpen)  iconOpen.classList.add('d-none');
                if (iconClose) iconClose.classList.remove('d-none');
                if (input) input.focus();
                if (csrfToken) loadUserConversations();
            } else {
                panel.setAttribute('hidden', 'hidden');
                if (iconOpen)  iconOpen.classList.remove('d-none');
                if (iconClose) iconClose.classList.add('d-none');
                root.classList.remove('is-right-open');
                if (window.innerWidth >= 900) {
                    root.classList.remove('is-right-collapsed');
                }
            }
        }

        // ── Right sidebar toggle ──────────────────────────────
        function syncRightToggleAria() {
            if (!toggleRight) return;
            if (window.innerWidth >= 900) {
                var collapsed = root.classList.contains('is-right-collapsed');
                toggleRight.setAttribute('aria-expanded', collapsed ? 'false' : 'true');
            } else {
                toggleRight.setAttribute('aria-expanded',
                    root.classList.contains('is-right-open') ? 'true' : 'false');
            }
        }

        // ── Event listeners ──────────────────────────────────
        // Primary click — stopPropagation ngăn conflict với document listener
        fab.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            console.log('[NekomimiChat] FAB clicked — was open:', isPanelOpen());
            // Toggle state
            var willBeOpen = !isPanelOpen();
            setOpen(willBeOpen);
        });

        // Expose toggle globally so it can be called programmatically
        window.__nekofabToggle = function () { setOpen(!isPanelOpen()); };

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && isPanelOpen()) {
                setOpen(false);
            }
        });

        // Click outside panel → đóng panel (không chặn khi click vào FAB hay panel)
        document.addEventListener('click', function (e) {
            if (!isPanelOpen()) return;
            // Đóng nếu click ra ngoài cả FAB và panel
            if (panel && !panel.contains(e.target) && !fab.contains(e.target)) {
                setOpen(false);
            }
        });

        if (toggleRight) {
            toggleRight.addEventListener('click', function () {
                if (window.innerWidth >= 900) {
                    root.classList.toggle('is-right-collapsed');
                } else {
                    root.classList.toggle('is-right-open');
                }
                syncRightToggleAria();
            });
        }

        if (sendBtn) {
            sendBtn.addEventListener('click', sendUserMessage);
        } else {
            console.warn('[NekomimiChat] sendBtn not found — send button will not work');
        }
        if (input) {
            input.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    sendUserMessage();
                }
            });
        } else {
            console.warn('[NekomimiChat] input not found — Enter-to-send will not work');
        }

        // AI thread click
        document.querySelector('[data-thread="ai"]')?.addEventListener('click', function () {
            switchThread('ai', null, null, null, null, null);
        });

        // External event: open vet chat (from profile page)
        window.addEventListener('nekomimi:open-vet', function (e) {
            var vetId   = e.detail.vetId;
            var convId  = e.detail.conversationId;
            var vetName = e.detail.vetName || 'Bác sĩ';
            var vetSpec = e.detail.vetSpec || '';
            var vetAvatar = e.detail.vetAvatar || null;

            addUserThread(convId, vetId, vetName, vetSpec, vetAvatar, 'vet');
            switchThread('vet', convId, vetId, vetName, vetSpec, vetAvatar);
            setOpen(true);
        });

        // External event: open private chat (from user search / profile)
        window.addEventListener('nekomimi:open-private', function (e) {
            var userId    = e.detail.userId;
            var convId    = e.detail.conversationId;
            var userName  = e.detail.userName || 'Người dùng';
            var userAvatar = e.detail.userAvatar || null;

            addUserThread(convId, userId, userName, null, userAvatar, 'private');
            switchThread('private', convId, userId, userName, null, userAvatar);
            setOpen(true);
        });

        // ── Compose: user search & start private chat ──────────
        var composeBtn = document.getElementById('chat-compose-btn');
        var userSearchInput = document.getElementById('nekomimi-chat-search');
        var userSearchResults = document.getElementById('user-search-results');
        var searchTimeout = null;

        if (composeBtn) {
            composeBtn.addEventListener('click', function () {
                userSearchInput.focus();
            });
        }

        if (userSearchInput && userSearchResults) {
            userSearchInput.addEventListener('input', function () {
                var q = userSearchInput.value.trim();
                clearTimeout(searchTimeout);
                if (q.length < 2) {
                    userSearchResults.style.display = 'none';
                    return;
                }
                searchTimeout = setTimeout(function () {
                    fetch('/api/users/search?q=' + encodeURIComponent(q) + '&' + csrfParam, {
                        credentials: 'same-origin',
                        headers: { 'Accept': 'application/json' }
                    }).then(function (r) {
                        if (!r.ok) return [];
                        return r.json();
                    }).then(function (users) {
                        if (!users.length) {
                            userSearchResults.innerHTML =
                                '<div class="px-3 py-2 small text-muted">Không tìm thấy người dùng</div>';
                            userSearchResults.style.display = 'block';
                            return;
                        }
                        userSearchResults.innerHTML = users.map(function (u) {
                            var uAv = normalizeAvatarSrc(u.avatarUrl);
                            var av = uAv
                                ? '<img src="' + escHtml(uAv) + '" alt="" class="chat-avatar-img" style="width:32px;height:32px;" loading="lazy"/>'
                                : '<i class="bi bi-person" style="font-size:1.25rem;"></i>';
                            return '<button type="button" class="user-search-item" ' +
                                'data-userid="' + u.id + '" data-username="' + escHtml(u.fullName) + '" ' +
                                'data-useravatar="' + escHtml(uAv) + '">' +
                                '<span class="me-2 flex-shrink-0">' + av + '</span>' +
                                '<span class="flex-grow-1 text-start small">' + escHtml(u.fullName) + '</span>' +
                                '</button>';
                        }).join('');
                        userSearchResults.style.display = 'block';
                        // Click on result → start private conversation
                        userSearchResults.querySelectorAll('.user-search-item').forEach(function (btn) {
                            btn.addEventListener('click', function () {
                                var uid = parseInt(btn.getAttribute('data-userid'), 10);
                                var uname = btn.getAttribute('data-username');
                                var uavatar = btn.getAttribute('data-useravatar') || null;
                                userSearchResults.style.display = 'none';
                                userSearchInput.value = '';
                                fetch('/api/conversations/private/' + uid + '?' + csrfParam, {
                                    credentials: 'same-origin',
                                    headers: { 'Accept': 'application/json' }
                                }).then(function (r) {
                                    if (!r.ok) throw new Error(r.status);
                                    return r.json();
                                }).then(function (data) {
                                    window.dispatchEvent(new CustomEvent('nekomimi:open-private', {
                                        detail: {
                                            userId: uid,
                                            conversationId: data.conversationId,
                                            userName: data.otherUserName || uname,
                                            userAvatar: data.otherUserAvatar || uavatar
                                        }
                                    }));
                                }).catch(function () {
                                    alert('Không thể mở cuộc trò chuyện.');
                                });
                            });
                        });
                    }).catch(function () {
                        userSearchResults.style.display = 'none';
                    });
                }, 300);
            });

            // Hide results when clicking outside
            document.addEventListener('click', function (e) {
                if (!userSearchInput.contains(e.target) && !userSearchResults.contains(e.target)) {
                    userSearchResults.style.display = 'none';
                }
            });
        }

        // ── Init ─────────────────────────────────────────────
        if (window.innerWidth >= 900) syncRightToggleAria();
        switchThread('ai', null, null, null, null, null);
    })();
});




