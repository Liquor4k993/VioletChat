// ===== CONFIGURATION =====
const API_URL = '';

// ===== STATE =====
let state = {
    token: localStorage.getItem('violetchat_token'),
    user: null,
    currentChat: 'group',
    activeUserId: null,
    activeUsername: null,
    stompClient: null,
    connected: false,
    friends: [],
    pendingRequests: [],
    blockedUsers: [],
    privateChats: {},
    dialogs: [],
    groupMessages: [],
    posts: [],
    currentPostId: null,
    isAuthenticated: !!localStorage.getItem('violetchat_token'),
    editingMessageId: null,
    editingMessageContent: ''
};

// ===== DOM HELPERS =====
const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

// ===== LOAD RECOMMENDATIONS =====
async function loadRecommendations() {
    const container = document.getElementById('recommendationsList');
    if (!container) return;

    try {
        const response = await fetch(`${API_URL}/api/users/recommendations`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Failed to load recommendations');

        const users = await response.json();

        if (!users || users.length === 0) {
            container.innerHTML = `
                <div style="color:var(--text-muted);font-size:13px;padding:8px;">
                    🌟 Нет рекомендаций
                </div>
            `;
            return;
        }

        container.innerHTML = users.map(u => `
            <div class="recommendation-item" onclick="if(!state.isAuthenticated){showAuthMessage('Войдите чтобы добавить друзей','error')}else{addFriend('${u.username}')}">
                <div class="recommendation-avatar">
                    ${u.firstName?.[0] || u.username?.[0] || '👤'}
                </div>
                <div class="recommendation-info">
                    <div class="recommendation-name">@${u.username}</div>
                    <div class="recommendation-action">${u.firstName || ''} ${u.lastName || ''}</div>
                </div>
                <button onclick="event.stopPropagation();addFriend('${u.username}')" 
                        style="background:var(--primary);border:none;color:white;padding:4px 10px;border-radius:8px;cursor:pointer;font-size:12px;">
                    ➕
                </button>
            </div>
        `).join('');
    } catch (error) {
        console.error('Load recommendations error:', error);
        container.innerHTML = `
            <div style="color:var(--text-muted);font-size:13px;padding:8px;">
                ⚠️ Не удалось загрузить рекомендации
            </div>
        `;
    }
}

// ===== LOAD LANDING FEED =====
async function loadLandingFeed() {
    const container = document.getElementById('landingFeed');
    if (!container) return;

    try {
        const response = await fetch(`${API_URL}/api/posts?page=0&size=10`);

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Server error:', errorText);
            throw new Error(`Ошибка загрузки: ${response.status}`);
        }

        const data = await response.json();
        const posts = data.content || [];

        if (posts.length === 0) {
            container.innerHTML = `
                <div style="text-align:center;padding:40px;color:var(--text-muted);">
                    <div style="font-size:48px;margin-bottom:16px;">📭</div>
                    <p>Пока нет постов</p>
                    <p style="font-size:13px;">Зарегистрируйтесь, чтобы создать первый пост!</p>
                </div>
            `;
            return;
        }

        container.innerHTML = posts.map(post => {
            const hasImage = post.imageUrl;
            let imageHtml = '';
            if (hasImage) {
                imageHtml = `<img src="${post.imageUrl}" style="width:100%;max-height:300px;object-fit:cover;border-radius:12px;margin-top:12px;cursor:pointer;" onclick="window.open('${post.imageUrl}')">`;
            }

            return `
                <div style="background:var(--bg-card);border-radius:16px;padding:20px;margin-bottom:16px;border:1px solid var(--border-color);">
                    <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px;">
                        <div style="width:36px;height:36px;border-radius:50%;background:var(--primary);display:flex;align-items:center;justify-content:center;font-size:16px;">
                            ${post.author.firstName?.[0] || post.author.username?.[0] || '👤'}
                        </div>
                        <div>
                            <div style="font-weight:500;font-size:14px;">@${post.author.username}</div>
                            <div style="font-size:12px;color:var(--text-muted);">${formatTime(post.createdAt)}</div>
                        </div>
                    </div>
                    <div style="font-size:15px;line-height:1.6;">${escapeHtml(post.content)}</div>
                    ${imageHtml}
                    <div style="display:flex;gap:16px;margin-top:12px;padding-top:12px;border-top:1px solid var(--border-color);color:var(--text-muted);font-size:14px;">
                        <span>❤️ ${post.likesCount || 0}</span>
                        <span>💬 ${post.comments?.length || 0}</span>
                        <span style="color:var(--primary-light);margin-left:auto;">🔒 Войдите, чтобы взаимодействовать</span>
                    </div>
                </div>
            `;
        }).join('');
    } catch (error) {
        console.error('Load landing feed error:', error);
        container.innerHTML = `
            <div style="text-align:center;padding:40px;color:var(--text-muted);">
                <div style="font-size:48px;margin-bottom:16px;">⚠️</div>
                <p>Не удалось загрузить ленту</p>
                <p style="font-size:13px;">Попробуйте обновить страницу</p>
                <p style="font-size:12px;color:var(--text-muted);margin-top:8px;">${error.message}</p>
            </div>
        `;
    }
}

// ===== AUTHENTICATION =====
function showAuthMessage(text, type = 'error') {
    const msg = document.getElementById('authMessage');
    if (!msg) return;
    msg.textContent = text;
    msg.className = `auth-message ${type}`;
    msg.style.display = 'block';
    setTimeout(() => {
        msg.style.display = 'none';
        msg.className = 'auth-message';
    }, 5000);
}

async function login(username, password) {
    try {
        const response = await fetch(`${API_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Ошибка входа');
        }

        const data = await response.json();
        localStorage.setItem('violetchat_token', data.token);
        state.token = data.token;
        state.user = data.user;
        state.isAuthenticated = true;

        document.getElementById('landingPage').style.display = 'none';
        document.getElementById('chatScreen').style.display = 'block';

        document.getElementById('sidebarUsername').textContent = state.user.username;
        initChat();
        loadRecommendations();
    } catch (error) {
        showAuthMessage(error.message);
        console.error('Login error:', error);
    }
}

async function register(userData) {
    try {
        const response = await fetch(`${API_URL}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(userData)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Ошибка регистрации');
        }

        showAuthMessage('✅ Регистрация успешна! Теперь войдите.', 'success');

        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === 'login');
        });
        document.getElementById('loginForm').classList.add('active');
        document.getElementById('registerForm').classList.remove('active');

        document.getElementById('loginUsername').value = userData.username;
        document.getElementById('loginPassword').value = '';
    } catch (error) {
        showAuthMessage(error.message);
        console.error('Register error:', error);
    }
}

// ===== INIT CHAT =====
function initChat() {
    if (!state.user) return;

    loadDialogs();
    loadFriends();
    loadPendingRequests();
    loadBlockedUsers();
    connectWebSocket();
    setupNavigation();
    setupImageUpload();
    openGroupChat();
}

// ===== NAVIGATION =====
function setupNavigation() {
    document.getElementById('chatTab').addEventListener('click', () => {
        showTab('chat');
        loadDialogs();
    });

    document.getElementById('feedTab').addEventListener('click', () => {
        showTab('feed');
        loadPosts();
    });

    document.getElementById('friendsTab').addEventListener('click', () => {
        showTab('friends');
        loadFriends();
        loadPendingRequests();
    });

    document.getElementById('settingsTab').addEventListener('click', () => {
        showTab('settings');
        loadSettings();
    });

    document.getElementById('createPostBtn')?.addEventListener('click', openCreatePost);
}

function showTab(tab) {
    document.querySelectorAll('#chatContent, #friendsContent, #settingsContent, #feedContent')
        .forEach(el => el.style.display = 'none');

    if (tab === 'chat') {
        document.getElementById('chatContent').style.display = 'flex';
    } else if (tab === 'friends') {
        document.getElementById('friendsContent').style.display = 'block';
    } else if (tab === 'settings') {
        document.getElementById('settingsContent').style.display = 'block';
    } else if (tab === 'feed') {
        document.getElementById('feedContent').style.display = 'block';
    }

    document.querySelectorAll('.nav-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.id === tab + 'Tab') {
            btn.classList.add('active');
        }
    });
}

// ===== DIALOGS =====
async function loadDialogs() {
    try {
        const friendsResponse = await fetch(`${API_URL}/api/users/friends`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (friendsResponse.ok) {
            const friends = await friendsResponse.json();

            const dialogs = [];
            for (const friend of friends) {
                try {
                    const historyResponse = await fetch(`${API_URL}/api/messages/history/${friend.id}?limit=1`, {
                        headers: { 'Authorization': `Bearer ${state.token}` }
                    });

                    let lastMessage = null;
                    let unread = 0;

                    if (historyResponse.ok) {
                        const history = await historyResponse.json();
                        if (history && history.length > 0) {
                            lastMessage = history[0];
                        }
                    }

                    if (state.privateChats[friend.id]) {
                        const msgs = state.privateChats[friend.id].messages || [];
                        unread = msgs.filter(m => m.sender && m.sender.id !== state.user.id && !m.read).length;
                    }

                    dialogs.push({
                        user: friend,
                        lastMessage: lastMessage,
                        unread: unread
                    });
                } catch (e) {
                    console.error('Error loading dialog for friend:', friend.username, e);
                }
            }

            state.dialogs = dialogs;
            renderDialogs();
        }
    } catch (error) {
        console.error('Load dialogs error:', error);
    }
}

function renderDialogs() {
    const container = document.getElementById('chatList');
    if (!container) return;

    if (!state.dialogs || state.dialogs.length === 0) {
        container.innerHTML = `
            <div style="padding:20px;color:var(--text-muted);text-align:center;font-size:14px;">
                💜 У вас пока нет диалогов<br>
                <span style="font-size:12px;">Добавьте друзей, чтобы начать общение</span>
            </div>
        `;
        return;
    }

    const sorted = [...state.dialogs].sort((a, b) => {
        const timeA = a.lastMessage ? new Date(a.lastMessage.createdAt).getTime() : 0;
        const timeB = b.lastMessage ? new Date(b.lastMessage.createdAt).getTime() : 0;
        return timeB - timeA;
    });

    container.innerHTML = sorted.map(dialog => {
        const friend = dialog.user;
        const lastMsg = dialog.lastMessage;
        const isActive = state.currentChat === 'private' && state.activeUserId === friend.id;

        let lastMsgText = 'Нет сообщений';
        let lastMsgTime = '';

        if (lastMsg) {
            const isImage = lastMsg.messageType === 'IMAGE';
            lastMsgText = isImage ? '📷 Изображение' : (lastMsg.content || '');
            if (lastMsg.sender && lastMsg.sender.id === state.user.id) {
                lastMsgText = 'Вы: ' + lastMsgText;
            }
            lastMsgTime = formatTime(lastMsg.createdAt);
        }

        const unreadBadge = dialog.unread > 0 ?
            `<span style="background:#7c3aed;color:white;border-radius:50%;padding:2px 8px;font-size:11px;margin-left:8px;">${dialog.unread}</span>` : '';

        return `
            <div class="dialog-item" onclick="openPrivateChat(${friend.id}, '${friend.username}')" 
                 style="${isActive ? 'background:var(--bg-input);border-left:3px solid var(--primary);' : ''}">
                <div style="width:44px;height:44px;border-radius:50%;background:var(--primary);display:flex;align-items:center;justify-content:center;font-size:18px;flex-shrink:0;overflow:hidden;margin-right:12px;">
                    ${friend.firstName?.[0] || friend.username?.[0] || '👤'}
                </div>
                <div style="flex:1;min-width:0;">
                    <div style="display:flex;align-items:center;justify-content:space-between;">
                        <div style="font-weight:600;font-size:14px;">@${friend.username}</div>
                        <div style="font-size:11px;color:var(--text-muted);">${lastMsgTime}</div>
                    </div>
                    <div style="display:flex;align-items:center;justify-content:space-between;">
                        <div style="font-size:13px;color:var(--text-muted);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:160px;">
                            ${lastMsgText}
                        </div>
                        ${unreadBadge}
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

// ===== GROUP CHAT =====
function openGroupChat() {
    state.currentChat = 'group';
    state.activeUserId = null;
    state.activeUsername = null;

    document.getElementById('chatTitle').textContent = '💬 Общий чат';
    document.getElementById('chatSubtitle').textContent = 'Все пользователи';
    showTab('chat');
    loadGroupMessages();
    renderDialogs();
}

async function loadGroupMessages() {
    try {
        const response = await fetch(`${API_URL}/api/messages/group/recent?limit=50`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) {
            console.error('Failed to load group messages:', response.status);
            return;
        }

        const messages = await response.json();
        state.groupMessages = messages || [];

        const messagesList = document.getElementById('messagesList');
        if (!messagesList) return;

        messagesList.innerHTML = '';

        if (messages.length === 0) {
            messagesList.innerHTML = `
                <div class="message system">
                    <div class="msg-content" style="text-align:center;color:var(--text-muted);">
                        💜 Добро пожаловать в VioletChat!
                    </div>
                </div>
            `;
        } else {
            messages.reverse().forEach(msg => {
                const isSelf = msg.sender && msg.sender.id === state.user.id;
                addMessageToChat(msg, isSelf);
            });
        }
        scrollToBottom();
    } catch (error) {
        console.error('Load messages error:', error);
    }
}

// ===== PRIVATE CHAT =====
function openPrivateChat(userId, username) {
    state.currentChat = 'private';
    state.activeUserId = userId;
    state.activeUsername = username;

    document.getElementById('chatTitle').textContent = `✉️ @${username}`;
    document.getElementById('chatSubtitle').textContent = 'Личный чат';
    showTab('chat');
    renderDialogs();
    loadPrivateMessages(userId);
}

async function loadPrivateMessages(userId) {
    try {
        const response = await fetch(`${API_URL}/api/messages/history/${userId}?limit=50`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) {
            console.error('Failed to load private messages:', response.status);
            return;
        }

        const messages = await response.json();

        if (!state.privateChats[userId]) {
            state.privateChats[userId] = { user: null, messages: [], unread: 0 };
        }
        state.privateChats[userId].messages = messages || [];

        markMessagesAsRead(userId);
        renderMessages(messages || []);
        scrollToBottom();
    } catch (error) {
        console.error('Load private messages error:', error);
    }
}

// ===== MARK AS READ =====
function markMessagesAsRead(userId) {
    if (!state.privateChats[userId]) return;

    const messages = state.privateChats[userId].messages || [];
    const unreadFromOther = messages.filter(m => {
        return m.sender && m.sender.id !== state.user.id && !m.read && !m.deleted;
    });

    if (unreadFromOther.length > 0) {
        unreadFromOther.forEach(m => m.read = true);
        state.privateChats[userId].unread = 0;
        loadDialogs();

        unreadFromOther.forEach(m => {
            if (m.id) {
                markMessageAsReadOnServer(m.id);
            }
        });
    }
}

async function markMessageAsReadOnServer(messageId) {
    try {
        await fetch(`${API_URL}/api/messages/${messageId}/read`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });
    } catch (e) {
        console.error('Error marking message as read:', e);
    }
}

function renderMessages(messages) {
    const messagesList = document.getElementById('messagesList');
    if (!messagesList) return;

    messagesList.innerHTML = '';

    if (!messages || messages.length === 0) {
        messagesList.innerHTML = `
            <div class="message system">
                <div class="msg-content" style="text-align:center;color:var(--text-muted);">
                    💜 Начните общение с @${state.activeUsername}!
                </div>
            </div>
        `;
        return;
    }

    const sorted = [...messages].sort((a, b) => {
        return new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime();
    });

    sorted.forEach(msg => {
        const isSelf = msg.sender && msg.sender.id === state.user.id;
        addMessageToChat(msg, isSelf);
    });
}

// ===== ADD MESSAGE =====
function addMessageToChat(message, isSelf = false) {
    const messagesList = document.getElementById('messagesList');
    if (!messagesList) return;

    // Проверяем, есть ли уже такое сообщение
    if (document.querySelector(`.message[data-msgid="${message.id}"]`)) {
        return;
    }

    if (message.deleted) {
        const html = `
            <div class="message deleted" data-msgid="${message.id}" style="opacity:0.6;">
                <div class="msg-content" style="font-style:italic;color:var(--text-muted);">Сообщение удалено</div>
                <div class="msg-time">${formatTime(message.createdAt)}</div>
            </div>
        `;
        messagesList.insertAdjacentHTML('beforeend', html);
        scrollToBottom();
        return;
    }

    const senderName = message.sender?.username || 'Unknown';
    const content = message.content || '';
    const isImage = message.messageType === 'IMAGE' && message.mediaUrl;
    const isEdited = message.editedAt;

    let contentHtml = '';
    if (isImage) {
        // Формируем правильный URL для изображения
        let imgUrl = message.mediaUrl;
        // Если URL не начинается с / или http, добавляем /
        if (!imgUrl.startsWith('/') && !imgUrl.startsWith('http')) {
            imgUrl = '/' + imgUrl;
        }
        // Если URL начинается с //, добавляем http:
        if (imgUrl.startsWith('//')) {
            imgUrl = 'http:' + imgUrl;
        }
        contentHtml = `
            <div style="margin:4px 0;">
                <img src="${imgUrl}" 
                     style="max-width:300px;max-height:300px;border-radius:12px;object-fit:cover;cursor:pointer;" 
                     onclick="window.open('${imgUrl}')" 
                     alt="Изображение" 
                     loading="lazy"
                     onerror="this.style.display='none';this.parentElement.innerHTML='<span style=\\'color:var(--text-muted);font-size:13px;\\'>❌ Ошибка загрузки изображения</span>'">
            </div>
        `;
    } else {
        contentHtml = escapeHtml(content);
    }

    const messageClass = isSelf ? 'self' : 'other';
    const editedBadge = isEdited ? `<span style="font-size:10px;color:var(--text-muted);margin-left:4px;">(ред.)</span>` : '';

    let actionButtons = '';
    if (isSelf && !message.deleted) {
        actionButtons = `
            <div style="display:flex;gap:4px;margin-top:4px;">
                <button onclick="editMessage(${message.id}, '${escapeHtml(message.content)}')" style="background:transparent;border:none;color:var(--text-muted);cursor:pointer;font-size:11px;">✏️</button>
                <button onclick="deleteMessage(${message.id})" style="background:transparent;border:none;color:#ff6b6b;cursor:pointer;font-size:11px;">🗑️</button>
            </div>
        `;
    }

    const html = `
        <div class="message ${messageClass}" data-msgid="${message.id}">
            ${!isSelf ? `<div class="msg-sender">@${escapeHtml(senderName)}</div>` : ''}
            <div class="msg-content">${contentHtml} ${editedBadge}</div>
            <div class="msg-time">${formatTime(message.createdAt)}</div>
            ${actionButtons}
            ${isSelf ? `<div class="msg-status" style="font-size:10px;color:var(--text-muted);margin-top:2px;text-align:right;">${message.read ? '✅ Прочитано' : '📤 Отправлено'}</div>` : ''}
        </div>
    `;
    messagesList.insertAdjacentHTML('beforeend', html);
    scrollToBottom();
}

function scrollToBottom() {
    const container = document.getElementById('messagesContainer');
    if (container) {
        setTimeout(() => { container.scrollTop = container.scrollHeight; }, 100);
    }
}

// ===== EDIT & DELETE =====
function editMessage(messageId, currentContent) {
    const newContent = prompt('Редактировать сообщение:', currentContent);
    if (newContent === null) return;
    if (newContent.trim() === '') {
        showToast('❌ Сообщение не может быть пустым', 'error');
        return;
    }

    if (state.stompClient && state.connected) {
        state.stompClient.send('/app/chat.edit', {}, JSON.stringify({
            messageId: messageId,
            content: newContent.trim()
        }));
        showToast('✏️ Сообщение обновляется...', 'info');
    } else {
        fetch(`${API_URL}/api/messages/${messageId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${state.token}`
            },
            body: JSON.stringify({ content: newContent.trim() })
        }).then(() => {
            showToast('✅ Сообщение обновлено', 'success');
            refreshCurrentChat();
        }).catch(() => {
            showToast('❌ Ошибка редактирования', 'error');
        });
    }
}

function deleteMessage(messageId) {
    if (!confirm('Удалить это сообщение?')) return;

    if (state.stompClient && state.connected) {
        state.stompClient.send('/app/chat.delete', {}, JSON.stringify({
            messageId: messageId
        }));
        showToast('🗑️ Сообщение удаляется...', 'info');
    } else {
        fetch(`${API_URL}/api/messages/${messageId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${state.token}`
            }
        }).then(() => {
            showToast('✅ Сообщение удалено', 'success');
            refreshCurrentChat();
        }).catch(() => {
            showToast('❌ Ошибка удаления', 'error');
        });
    }
}

function refreshCurrentChat() {
    if (state.currentChat === 'group') {
        loadGroupMessages();
    } else if (state.currentChat === 'private' && state.activeUserId) {
        loadPrivateMessages(state.activeUserId);
    }
}

// ===== SEND MESSAGE =====
async function sendMessage() {
    const messageInput = document.getElementById('messageInput');
    const sendBtn = document.getElementById('sendMessageBtn');
    if (!messageInput || !sendBtn) return;

    const content = messageInput.value.trim();
    if (!content) return;

    sendBtn.disabled = true;

    const isPrivate = state.currentChat === 'private' && state.activeUserId;
    const payload = {
        content: content,
        groupMessage: !isPrivate,
        receiverId: isPrivate ? state.activeUserId : null,
        messageType: 'TEXT'
    };

    try {
        if (state.stompClient && state.connected) {
            state.stompClient.send('/app/chat.send', {}, JSON.stringify(payload));
            messageInput.value = '';

            // Оптимистичное обновление
            const tempMessage = {
                id: Date.now(),
                content: content,
                sender: state.user,
                receiver: isPrivate ? { id: state.activeUserId } : null,
                createdAt: new Date().toISOString(),
                messageType: 'TEXT',
                mediaUrl: null,
                read: false,
                deleted: false,
                editedAt: null
            };

            if (isPrivate) {
                if (!state.privateChats[state.activeUserId]) {
                    state.privateChats[state.activeUserId] = { user: null, messages: [], unread: 0 };
                }
                state.privateChats[state.activeUserId].messages.push(tempMessage);
                loadDialogs();
            } else {
                state.groupMessages.push(tempMessage);
            }

            addMessageToChat(tempMessage, true);
            scrollToBottom();
        } else {
            const response = await fetch(`${API_URL}/api/messages`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${state.token}`
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) {
                const error = await response.text();
                throw new Error(`Ошибка отправки: ${response.status} - ${error}`);
            }

            const msg = await response.json();

            if (isPrivate) {
                if (!state.privateChats[state.activeUserId]) {
                    state.privateChats[state.activeUserId] = { user: null, messages: [], unread: 0 };
                }
                state.privateChats[state.activeUserId].messages.push(msg);
                loadDialogs();
            } else {
                state.groupMessages.push(msg);
            }

            addMessageToChat(msg, true);
            messageInput.value = '';
            scrollToBottom();
        }
    } catch (error) {
        console.error('Send error:', error);
        showToast('❌ Не удалось отправить сообщение: ' + error.message, 'error');
    } finally {
        sendBtn.disabled = false;
    }
}

// ===== SEND IMAGE =====
async function sendImage(imageFile) {
    const formData = new FormData();
    formData.append('image', imageFile);

    const isPrivate = state.currentChat === 'private' && state.activeUserId;

    if (isPrivate) {
        formData.append('receiverId', state.activeUserId);
        formData.append('groupMessage', 'false');
    } else {
        formData.append('groupMessage', 'true');
    }

    try {
        const response = await fetch(`${API_URL}/api/messages/image`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${state.token}`
            },
            body: formData
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(error || 'Ошибка отправки изображения');
        }

        const message = await response.json();

        if (isPrivate) {
            if (!state.privateChats[state.activeUserId]) {
                state.privateChats[state.activeUserId] = { user: null, messages: [], unread: 0 };
            }
            state.privateChats[state.activeUserId].messages.push(message);
            loadDialogs();
        } else {
            state.groupMessages.push(message);
        }

        addMessageToChat(message, true);
        scrollToBottom();
        showToast('✅ Изображение отправлено', 'success');
    } catch (error) {
        console.error('Send image error:', error);
        showToast('❌ ' + error.message, 'error');
    }
}

function setupImageUpload() {
    const imageBtn = document.getElementById('imageUploadBtn');
    if (!imageBtn) return;

    imageBtn.addEventListener('click', () => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = 'image/*';
        input.onchange = async (e) => {
            const file = e.target.files[0];
            if (!file) return;

            if (file.size > 10 * 1024 * 1024) {
                showToast('❌ Файл слишком большой (макс 10MB)', 'error');
                return;
            }

            await sendImage(file);
        };
        input.click();
    });
}

// ===== POSTS =====
async function loadPosts() {
    try {
        const response = await fetch(`${API_URL}/api/posts?page=0&size=20`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) {
            const errorText = await response.text();
            console.error('Server error:', errorText);
            throw new Error(`Ошибка загрузки постов: ${response.status}`);
        }

        const data = await response.json();
        state.posts = data.content || [];
        renderPosts();
    } catch (error) {
        console.error('Load posts error:', error);
        showToast('❌ Не удалось загрузить ленту: ' + error.message, 'error');
    }
}

function renderPosts() {
    const container = document.getElementById('postsList');
    if (!container) return;

    if (!state.posts || state.posts.length === 0) {
        container.innerHTML = `
            <div style="padding:40px;text-align:center;color:var(--text-muted);">
                📭 Пока нет постов<br>
                <span style="font-size:12px;">Будьте первым, кто создаст пост!</span>
            </div>
        `;
        return;
    }

    container.innerHTML = state.posts.map(post => {
        const isAuthor = state.user && post.author.id === state.user.id;
        const liked = post.likedByCurrentUser || false;
        const hasImage = post.imageUrl;

        let imageHtml = '';
        if (hasImage) {
            imageHtml = `<img src="${post.imageUrl}" style="width:100%;max-height:400px;object-fit:cover;border-radius:12px;margin-top:12px;cursor:pointer;" onclick="window.open('${post.imageUrl}')">`;
        }

        let commentsHtml = '';
        if (post.comments && post.comments.length > 0) {
            commentsHtml = post.comments.slice(0, 3).map(c => {
                const commentImage = c.imageUrl ? `<img src="${c.imageUrl}" style="max-width:60px;max-height:60px;border-radius:8px;object-fit:cover;margin-top:4px;">` : '';
                return `
                    <div style="display:flex;gap:8px;padding:8px 0;border-bottom:1px solid var(--border-color);">
                        <div style="width:28px;height:28px;border-radius:50%;background:var(--primary);display:flex;align-items:center;justify-content:center;font-size:12px;flex-shrink:0;">
                            ${c.author.firstName?.[0] || c.author.username?.[0] || '👤'}
                        </div>
                        <div style="flex:1;">
                            <div style="font-size:12px;font-weight:500;">@${c.author.username}</div>
                            <div style="font-size:13px;">${escapeHtml(c.content)}</div>
                            ${commentImage}
                        </div>
                    </div>
                `;
            }).join('');

            if (post.comments.length > 3) {
                commentsHtml += `<div style="color:var(--text-muted);font-size:12px;padding:4px 0;">Ещё ${post.comments.length - 3} комментариев...</div>`;
            }
        }

        return `
            <div class="post-item">
                <div class="post-header">
                    <div class="post-avatar">
                        ${post.author.firstName?.[0] || post.author.username?.[0] || '👤'}
                    </div>
                    <div>
                        <div class="post-author">@${post.author.username}</div>
                        <div class="post-time">${formatTime(post.createdAt)}</div>
                    </div>
                    ${isAuthor ? `<button onclick="deletePost(${post.id})" style="background:transparent;border:none;color:#ff6b6b;cursor:pointer;margin-left:auto;">🗑️</button>` : ''}
                </div>
                <div class="post-content">${escapeHtml(post.content)}</div>
                ${imageHtml}
                <div class="post-actions">
                    <button onclick="toggleLike(${post.id})" style="background:transparent;border:none;color:${liked ? '#7c3aed' : 'var(--text-muted)'};cursor:pointer;font-size:14px;">
                        ${liked ? '❤️' : '🤍'} ${post.likesCount || 0}
                    </button>
                    <button onclick="openCommentModal(${post.id})" style="background:transparent;border:none;color:var(--text-muted);cursor:pointer;font-size:14px;">
                        💬 ${post.comments?.length || 0}
                    </button>
                </div>
                ${commentsHtml ? `<div style="margin-top:12px;padding-top:12px;border-top:1px solid var(--border-color);">${commentsHtml}</div>` : ''}
            </div>
        `;
    }).join('');
}

// ===== CREATE POST =====
function openCreatePost() {
    document.getElementById('createPostModal').style.display = 'flex';
    document.getElementById('postContent').value = '';
    document.getElementById('postImage').value = '';
}

function closeCreatePost() {
    document.getElementById('createPostModal').style.display = 'none';
}

async function createPost() {
    const content = document.getElementById('postContent').value.trim();
    if (!content) {
        showToast('❌ Введите текст поста', 'error');
        return;
    }

    const imageFile = document.getElementById('postImage').files[0];

    const formData = new FormData();
    formData.append('content', content);
    if (imageFile) {
        formData.append('image', imageFile);
    }

    try {
        const response = await fetch(`${API_URL}/api/posts`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${state.token}`
            },
            body: formData
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(`Ошибка создания поста: ${response.status} - ${error}`);
        }

        const post = await response.json();
        state.posts.unshift(post);
        renderPosts();
        closeCreatePost();
        showToast('✅ Пост опубликован!', 'success');
    } catch (error) {
        console.error('Create post error:', error);
        showToast('❌ Не удалось создать пост: ' + error.message, 'error');
    }
}

// ===== LIKES =====
async function toggleLike(postId) {
    try {
        const response = await fetch(`${API_URL}/api/posts/${postId}/like`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${state.token}`
            }
        });

        if (!response.ok) throw new Error('Ошибка');

        loadPosts();
    } catch (error) {
        console.error('Like error:', error);
        showToast('❌ Не удалось поставить лайк', 'error');
    }
}

// ===== COMMENTS =====
function openCommentModal(postId) {
    state.currentPostId = postId;
    document.getElementById('commentModal').style.display = 'flex';
    document.getElementById('commentContent').value = '';
    document.getElementById('commentImage').value = '';
}

function closeComment() {
    document.getElementById('commentModal').style.display = 'none';
    state.currentPostId = null;
}

async function submitComment() {
    const content = document.getElementById('commentContent').value.trim();
    if (!content) {
        showToast('❌ Введите текст комментария', 'error');
        return;
    }

    const imageFile = document.getElementById('commentImage').files[0];
    const formData = new FormData();
    formData.append('content', content);
    if (imageFile) {
        formData.append('image', imageFile);
    }

    try {
        const response = await fetch(`${API_URL}/api/posts/${state.currentPostId}/comments`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${state.token}`
            },
            body: formData
        });

        if (!response.ok) throw new Error('Ошибка отправки комментария');

        closeComment();
        showToast('✅ Комментарий добавлен!', 'success');
        loadPosts();
    } catch (error) {
        console.error('Comment error:', error);
        showToast('❌ Не удалось отправить комментарий', 'error');
    }
}

async function deletePost(postId) {
    if (!confirm('Удалить этот пост?')) return;

    try {
        const response = await fetch(`${API_URL}/api/posts/${postId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${state.token}`
            }
        });

        if (!response.ok) throw new Error('Ошибка удаления');

        showToast('✅ Пост удален', 'success');
        loadPosts();
    } catch (error) {
        console.error('Delete post error:', error);
        showToast('❌ Не удалось удалить пост', 'error');
    }
}

// ===== FRIENDS =====
async function loadFriends() {
    try {
        const response = await fetch(`${API_URL}/api/users/friends`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Ошибка загрузки друзей');

        state.friends = await response.json();
        renderFriendsList();
    } catch (error) {
        console.error('Load friends error:', error);
    }
}

function renderFriendsList() {
    const container = document.getElementById('friendsList');
    if (!container) return;

    if (!state.friends || state.friends.length === 0) {
        container.innerHTML = '<div style="padding:16px;color:var(--text-muted);text-align:center;">У вас пока нет друзей 😢</div>';
        return;
    }

    container.innerHTML = state.friends.map(friend => `
        <div style="display:flex;align-items:center;justify-content:space-between;padding:12px;border-bottom:1px solid var(--border-color);">
            <div style="display:flex;align-items:center;gap:12px;cursor:pointer;" 
                 onclick="openPrivateChat(${friend.id}, '${friend.username}')">
                <div style="width:40px;height:40px;border-radius:50%;background:var(--primary);display:flex;align-items:center;justify-content:center;font-size:18px;overflow:hidden;">
                    ${friend.firstName?.[0] || friend.username?.[0] || '👤'}
                </div>
                <div>
                    <div style="font-weight:500;">@${friend.username}</div>
                    <div style="font-size:12px;color:var(--text-muted);">${friend.firstName || ''} ${friend.lastName || ''}</div>
                </div>
            </div>
            <div>
                <button onclick="openPrivateChat(${friend.id}, '${friend.username}')" 
                        style="background:var(--primary);border:none;color:white;padding:6px 12px;border-radius:8px;cursor:pointer;">💬</button>
                <button onclick="blockUser(${friend.id})" 
                        style="background:#ff6b6b;border:none;color:white;padding:6px 12px;border-radius:8px;cursor:pointer;margin-left:4px;">🚫</button>
            </div>
        </div>
    `).join('');
}

async function loadPendingRequests() {
    try {
        const response = await fetch(`${API_URL}/api/users/friends/pending`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Ошибка загрузки заявок');

        state.pendingRequests = await response.json();
        renderPendingRequests();
    } catch (error) {
        console.error('Load pending error:', error);
    }
}

function renderPendingRequests() {
    const container = document.getElementById('pendingRequests');
    if (!container) return;

    if (!state.pendingRequests || state.pendingRequests.length === 0) {
        container.innerHTML = '<div style="padding:12px;color:var(--text-muted);font-size:14px;">Нет входящих заявок</div>';
        return;
    }

    container.innerHTML = state.pendingRequests.map(f => {
        const user = f.user;
        if (!user) return '';
        return `
            <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-bottom:1px solid var(--border-color);">
                <div style="display:flex;align-items:center;gap:12px;">
                    <div style="width:32px;height:32px;border-radius:50%;background:var(--primary);display:flex;align-items:center;justify-content:center;font-size:14px;">
                        ${user.firstName?.[0] || user.username?.[0] || '👤'}
                    </div>
                    <div>
                        <div style="font-weight:500;">@${user.username}</div>
                        <div style="font-size:12px;color:var(--text-muted);">${user.firstName || ''}</div>
                    </div>
                </div>
                <div>
                    <button onclick="acceptFriendRequest(${f.id})" 
                            style="background:#51cf66;border:none;color:white;padding:4px 12px;border-radius:8px;cursor:pointer;">✅ Принять</button>
                    <button onclick="rejectFriendRequest(${f.id})" 
                            style="background:#ff6b6b;border:none;color:white;padding:4px 12px;border-radius:8px;cursor:pointer;margin-left:4px;">❌ Отклонить</button>
                </div>
            </div>
        `;
    }).join('');
}

async function acceptFriendRequest(requestId) {
    try {
        const response = await fetch(`${API_URL}/api/users/friends/accept/${requestId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Ошибка принятия заявки');

        showToast('✅ Заявка принята!', 'success');
        loadPendingRequests();
        loadFriends();
        loadDialogs();
        loadRecommendations();
    } catch (error) {
        showToast('❌ ' + error.message, 'error');
    }
}

async function rejectFriendRequest(requestId) {
    try {
        const response = await fetch(`${API_URL}/api/users/friends/reject/${requestId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Ошибка отклонения заявки');

        showToast('❌ Заявка отклонена', 'info');
        loadPendingRequests();
    } catch (error) {
        showToast('❌ ' + error.message, 'error');
    }
}

async function addFriend(username) {
    if (!username) return;

    try {
        const response = await fetch(`${API_URL}/api/users/friends/request?username=${encodeURIComponent(username)}`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Ошибка отправки заявки');
        }

        showToast(`✅ Заявка отправлена @${username}`, 'success');
        document.getElementById('searchInput').value = '';
        document.getElementById('searchResults').innerHTML = '';
        loadFriends();
        loadRecommendations();
    } catch (error) {
        showToast('❌ ' + error.message, 'error');
    }
}

// ===== SEARCH =====
async function searchUsers(query) {
    if (!query || query.length < 2) {
        document.getElementById('searchResults').innerHTML = '';
        return;
    }

    try {
        const response = await fetch(`${API_URL}/api/users/search?query=${encodeURIComponent(query)}`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Ошибка поиска');

        const users = await response.json();
        const results = document.getElementById('searchResults');

        if (users.length === 0) {
            results.innerHTML = '<div style="padding:16px;color:var(--text-muted);">Пользователи не найдены</div>';
            return;
        }

        results.innerHTML = users.map(u => `
            <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-bottom:1px solid var(--border-color);">
                <div style="display:flex;align-items:center;gap:12px;">
                    <div style="width:32px;height:32px;border-radius:50%;background:var(--primary);display:flex;align-items:center;justify-content:center;font-size:14px;overflow:hidden;">
                        ${u.firstName?.[0] || u.username?.[0] || '👤'}
                    </div>
                    <div>
                        <div style="font-weight:500;">@${u.username}</div>
                        <div style="font-size:12px;color:var(--text-muted);">${u.firstName || ''} ${u.lastName || ''}</div>
                    </div>
                </div>
                <div>
                    <button onclick="openPrivateChat(${u.id}, '${u.username}')" 
                            style="background:var(--primary);border:none;color:white;padding:4px 12px;border-radius:8px;cursor:pointer;font-size:12px;">💬</button>
                    <button onclick="addFriend('${u.username}')" 
                            style="background:#51cf66;border:none;color:white;padding:4px 12px;border-radius:8px;cursor:pointer;font-size:12px;margin-left:4px;">➕</button>
                    <button onclick="blockUser(${u.id})" 
                            style="background:#ff6b6b;border:none;color:white;padding:4px 12px;border-radius:8px;cursor:pointer;font-size:12px;margin-left:4px;">🚫</button>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Search error:', error);
    }
}

// ===== BLACKLIST =====
async function blockUser(userId) {
    if (!confirm('Заблокировать этого пользователя?')) return;

    try {
        const response = await fetch(`${API_URL}/api/users/blacklist/${userId}`, {
            method: 'POST',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Ошибка блокировки');

        showToast('✅ Пользователь заблокирован', 'success');
        loadBlockedUsers();
        loadFriends();
        loadDialogs();
        loadRecommendations();
    } catch (error) {
        showToast('❌ ' + error.message, 'error');
    }
}

async function unblockUser(userId) {
    try {
        const response = await fetch(`${API_URL}/api/users/blacklist/${userId}`, {
            method: 'DELETE',
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Ошибка разблокировки');

        showToast('✅ Пользователь разблокирован', 'success');
        loadBlockedUsers();
        loadRecommendations();
    } catch (error) {
        showToast('❌ ' + error.message, 'error');
    }
}

async function loadBlockedUsers() {
    try {
        const response = await fetch(`${API_URL}/api/users/blacklist`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Ошибка загрузки ЧС');

        state.blockedUsers = await response.json();
        renderBlockedUsers();
    } catch (error) {
        console.error('Load blocked error:', error);
    }
}

function renderBlockedUsers() {
    const container = document.getElementById('blockedList');
    if (!container) return;

    if (!state.blockedUsers || state.blockedUsers.length === 0) {
        container.innerHTML = '<div style="padding:12px;color:var(--text-muted);font-size:14px;">Чёрный список пуст</div>';
        return;
    }

    container.innerHTML = state.blockedUsers.map(b => `
        <div style="display:flex;align-items:center;justify-content:space-between;padding:10px 12px;border-bottom:1px solid var(--border-color);">
            <div style="display:flex;align-items:center;gap:12px;">
                <div style="width:32px;height:32px;border-radius:50%;background:var(--primary);display:flex;align-items:center;justify-content:center;font-size:14px;">
                    ${b.blockedUser.firstName?.[0] || b.blockedUser.username?.[0] || '👤'}
                </div>
                <div>
                    <div style="font-weight:500;">@${b.blockedUser.username}</div>
                    <div style="font-size:12px;color:var(--text-muted);">${b.reason || 'Блокировка'}</div>
                </div>
            </div>
            <button onclick="unblockUser(${b.blockedUser.id})" 
                    style="background:#51cf66;border:none;color:white;padding:4px 12px;border-radius:8px;cursor:pointer;font-size:12px;">Разблокировать</button>
        </div>
    `).join('');
}

// ===== SETTINGS =====
async function loadSettings() {
    try {
        const response = await fetch(`${API_URL}/api/users/me`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Ошибка загрузки настроек');

        const user = await response.json();
        document.getElementById('settingsUsername').value = user.username || '';
        document.getElementById('settingsEmail').value = user.email || '';
        document.getElementById('settingsFirstName').value = user.firstName || '';
        document.getElementById('settingsLastName').value = user.lastName || '';
    } catch (error) {
        console.error('Load settings error:', error);
    }
}

async function saveSettings() {
    const data = {
        email: document.getElementById('settingsEmail').value,
        firstName: document.getElementById('settingsFirstName').value,
        lastName: document.getElementById('settingsLastName').value
    };

    try {
        const response = await fetch(`${API_URL}/api/users/me`, {
            method: 'PATCH',
            headers: {
                'Authorization': `Bearer ${state.token}`,
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) throw new Error('Ошибка сохранения');

        const updated = await response.json();
        state.user = { ...state.user, ...updated };
        document.getElementById('sidebarUsername').textContent = state.user.username;
        showToast('✅ Профиль обновлён', 'success');
        loadRecommendations();
    } catch (error) {
        showToast('❌ ' + error.message, 'error');
    }
}

// ===== WEBSOCKET =====
function connectWebSocket() {
    if (state.stompClient && state.connected) return;

    try {
        const socket = new SockJS('/ws');
        state.stompClient = Stomp.over(socket);
        state.stompClient.debug = null;

        state.stompClient.connect(
            {},
            function onConnected() {
                console.log('✅ WebSocket connected');
                state.connected = true;

                // Подписка на общий чат
                state.stompClient.subscribe('/topic/public', function(payload) {
                    try {
                        const data = JSON.parse(payload.body);

                        // Проверка на уведомление об удалении
                        if (data.type === 'MESSAGE_DELETED') {
                            const msgElement = document.querySelector(`.message[data-msgid="${data.messageId}"]`);
                            if (msgElement) {
                                msgElement.style.opacity = '0.3';
                                msgElement.querySelector('.msg-content').textContent = 'Сообщение удалено';
                            }
                            return;
                        }

                        // Проверка на системное сообщение
                        if (data.type === 'JOIN') {
                            showToast(data.message, 'info');
                            return;
                        }

                        const message = data;
                        if (message.sender && message.sender.id !== state.user.id) {
                            if (message.receiver) {
                                // Приватное сообщение в общем топике (если сообщение групповое)
                                const senderId = message.sender.id;
                                if (!state.privateChats[senderId]) {
                                    state.privateChats[senderId] = { user: null, messages: [], unread: 0 };
                                }

                                // Проверяем, нет ли уже такого сообщения
                                const exists = state.privateChats[senderId].messages.some(m => m.id === message.id);
                                if (!exists) {
                                    state.privateChats[senderId].messages.push(message);
                                    state.privateChats[senderId].unread = (state.privateChats[senderId].unread || 0) + 1;
                                    loadDialogs();

                                    if (state.currentChat === 'private' && state.activeUserId === senderId) {
                                        addMessageToChat(message, false);
                                        state.privateChats[senderId].unread = 0;
                                        if (message.id) {
                                            markMessageAsReadOnServer(message.id);
                                        }
                                    }
                                }
                            } else {
                                // Групповое сообщение
                                const exists = state.groupMessages.some(m => m.id === message.id);
                                if (!exists) {
                                    state.groupMessages.push(message);
                                    if (state.currentChat === 'group') {
                                        addMessageToChat(message, false);
                                    }
                                } else {
                                    // Обновление существующего сообщения (редактирование)
                                    const index = state.groupMessages.findIndex(m => m.id === message.id);
                                    if (index !== -1) {
                                        state.groupMessages[index] = message;
                                        const msgElement = document.querySelector(`.message[data-msgid="${message.id}"]`);
                                        if (msgElement) {
                                            msgElement.remove();
                                            addMessageToChat(message, false);
                                        }
                                    }
                                }
                            }
                        }
                    } catch(e) {
                        console.error('Parse error:', e);
                    }
                });

                // Подписка на личные сообщения
                state.stompClient.subscribe('/user/queue/private', function(payload) {
                    try {
                        const message = JSON.parse(payload.body);
                        if (message.sender && message.sender.id !== state.user.id) {
                            const senderId = message.sender.id;

                            if (!state.privateChats[senderId]) {
                                state.privateChats[senderId] = { user: null, messages: [], unread: 0 };
                            }

                            // Проверяем, нет ли уже такого сообщения
                            const exists = state.privateChats[senderId].messages.some(m => m.id === message.id);
                            if (!exists) {
                                state.privateChats[senderId].messages.push(message);
                                state.privateChats[senderId].unread = (state.privateChats[senderId].unread || 0) + 1;
                                loadDialogs();

                                if (state.currentChat === 'private' && state.activeUserId === senderId) {
                                    addMessageToChat(message, false);
                                    state.privateChats[senderId].unread = 0;
                                    if (message.id) {
                                        markMessageAsReadOnServer(message.id);
                                    }
                                }
                            } else {
                                // Обновление существующего сообщения
                                const index = state.privateChats[senderId].messages.findIndex(m => m.id === message.id);
                                if (index !== -1) {
                                    state.privateChats[senderId].messages[index] = message;
                                    if (state.currentChat === 'private' && state.activeUserId === senderId) {
                                        renderMessages(state.privateChats[senderId].messages);
                                    }
                                }
                            }
                        }
                    } catch(e) {
                        console.error('Parse private error:', e);
                    }
                });

                // Отправляем приветствие
                setTimeout(() => {
                    try {
                        state.stompClient.send('/app/chat.addUser', {}, JSON.stringify({
                            username: state.user.username
                        }));
                    } catch(e) {}
                }, 500);
            },
            function onError() {
                console.log('❌ WebSocket disconnected');
                state.connected = false;
                setTimeout(connectWebSocket, 5000);
            }
        );
    } catch (error) {
        console.error('WebSocket error:', error);
        state.connected = false;
        setTimeout(connectWebSocket, 5000);
    }
}

// ===== UTILITIES =====
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatTime(dateStr) {
    if (!dateStr) return '';
    try {
        const date = new Date(dateStr);
        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const msgDate = new Date(date.getFullYear(), date.getMonth(), date.getDate());

        if (msgDate.getTime() === today.getTime()) {
            return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
        } else {
            return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' });
        }
    } catch(e) { return ''; }
}

function showToast(text, type = 'info') {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.style.cssText = `
        position: fixed;
        bottom: 24px;
        left: 50%;
        transform: translateX(-50%);
        background: ${type === 'error' ? '#ff6b6b' : type === 'success' ? '#51cf66' : '#7c3aed'};
        color: white;
        padding: 12px 24px;
        border-radius: 12px;
        font-weight: 500;
        z-index: 9999;
        max-width: 90%;
        text-align: center;
        animation: fadeIn 0.3s ease;
        box-shadow: 0 4px 24px rgba(0,0,0,0.3);
    `;
    toast.textContent = text;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ===== EVENT LISTENERS =====
document.addEventListener('DOMContentLoaded', function() {
    // Auth tabs
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const tab = this.dataset.tab;
            document.querySelectorAll('.tab-btn').forEach(b => {
                b.classList.toggle('active', b.dataset.tab === tab);
            });
            document.getElementById('loginForm').classList.toggle('active', tab === 'login');
            document.getElementById('registerForm').classList.toggle('active', tab === 'register');
            document.getElementById('authMessage').style.display = 'none';
        });
    });

    // Login
    document.getElementById('loginForm').addEventListener('submit', function(e) {
        e.preventDefault();
        const username = document.getElementById('loginUsername').value.trim();
        const password = document.getElementById('loginPassword').value;
        if (username && password) {
            login(username, password);
        }
    });

    // Register
    document.getElementById('registerForm').addEventListener('submit', function(e) {
        e.preventDefault();
        const data = {
            username: document.getElementById('regUsername').value.trim(),
            email: document.getElementById('regEmail').value.trim(),
            password: document.getElementById('regPassword').value,
            firstName: document.getElementById('regFirstName').value.trim(),
            lastName: document.getElementById('regLastName').value.trim()
        };
        if (data.username && data.email && data.password) {
            register(data);
        }
    });

    // Logout
    document.getElementById('logoutBtn')?.addEventListener('click', function() {
        localStorage.removeItem('violetchat_token');
        state.token = null;
        state.user = null;
        state.isAuthenticated = false;
        document.getElementById('chatScreen').style.display = 'none';
        document.getElementById('landingPage').style.display = 'flex';
        document.getElementById('loginPassword').value = '';
        loadLandingFeed();
        loadRecommendations();
    });

    // Send message
    document.getElementById('sendMessageBtn')?.addEventListener('click', sendMessage);
    document.getElementById('messageInput')?.addEventListener('keydown', function(e) {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // Search
    document.getElementById('searchInput')?.addEventListener('input', function() {
        searchUsers(this.value);
    });

    // Save settings
    document.getElementById('saveSettingsBtn')?.addEventListener('click', saveSettings);

    // Modals
    document.getElementById('createPostModal')?.addEventListener('click', function(e) {
        if (e.target === this) closeCreatePost();
    });
    document.getElementById('commentModal')?.addEventListener('click', function(e) {
        if (e.target === this) closeComment();
    });
});

// ===== STARTUP =====
if (state.token && state.user) {
    document.getElementById('landingPage').style.display = 'none';
    document.getElementById('chatScreen').style.display = 'block';
    document.getElementById('sidebarUsername').textContent = state.user.username;
    initChat();
    loadRecommendations();
} else {
    document.getElementById('landingPage').style.display = 'flex';
    document.getElementById('chatScreen').style.display = 'none';
    loadLandingFeed();
    loadRecommendations();
}

console.log('💜 VioletChat loaded!');