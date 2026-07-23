// ===== Конфигурация =====
const API_URL = '';

// ===== Состояние приложения =====
let state = {
    token: localStorage.getItem('violetchat_token'),
    user: null,
    currentChat: 'group',
    activeUserId: null,
    stompClient: null,
    connected: false
};

// ===== DOM-элементы =====
const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);

const authScreen = $('#authScreen');
const chatScreen = $('#chatScreen');
const loginForm = $('#loginForm');
const registerForm = $('#registerForm');
const authTabs = $$('.tab-btn');
const authMessage = $('#authMessage');
const logoutBtn = $('#logoutBtn');
const messagesList = $('#messagesList');
const messageInput = $('#messageInput');
const sendBtn = $('#sendMessageBtn');
const sidebarUsername = $('#sidebarUsername');
const chatTitle = $('#chatTitle');

// ===== АУТЕНТИФИКАЦИЯ =====
function showAuthMessage(text, type = 'error') {
    authMessage.textContent = text;
    authMessage.className = `auth-message ${type}`;
    authMessage.style.display = 'block';
    setTimeout(() => {
        authMessage.style.display = 'none';
        authMessage.className = 'auth-message';
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
        initChat();
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
        switchAuthTab('login');
        $('#loginUsername').value = userData.username;
        $('#loginPassword').value = '';
    } catch (error) {
        showAuthMessage(error.message);
        console.error('Register error:', error);
    }
}

function switchAuthTab(tab) {
    authTabs.forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tab);
    });
    loginForm.classList.toggle('active', tab === 'login');
    registerForm.classList.toggle('active', tab === 'register');
    authMessage.style.display = 'none';
    authMessage.className = 'auth-message';
}

// ===== ИНИЦИАЛИЗАЦИЯ ЧАТА =====
function initChat() {
    if (!state.user) return;

    authScreen.style.display = 'none';
    chatScreen.style.display = 'block';

    sidebarUsername.textContent = state.user.username || state.user.email;
    $('#userAvatar').textContent = (state.user.firstName?.[0] || state.user.username?.[0] || '👤').toUpperCase();

    loadGroupMessages();
    connectWebSocket();
}

function logout() {
    localStorage.removeItem('violetchat_token');
    state.token = null;
    state.user = null;
    if (state.stompClient) {
        try {
            state.stompClient.disconnect();
        } catch(e) {}
        state.stompClient = null;
    }
    state.connected = false;
    chatScreen.style.display = 'none';
    authScreen.style.display = 'flex';
    $('#loginPassword').value = '';
}

// ===== WEBSOCKET =====
function connectWebSocket() {
    if (state.stompClient && state.connected) {
        return;
    }

    try {
        const socket = new SockJS('/ws');
        state.stompClient = Stomp.over(socket);

        state.stompClient.connect(
            {},
            function onConnected() {
                console.log('✅ WebSocket connected');
                state.connected = true;
                updateConnectionStatus(true);

                // Подписываемся на общий чат
                state.stompClient.subscribe('/topic/public', function(payload) {
                    try {
                        const message = JSON.parse(payload.body);
                        console.log('📩 Получено сообщение:', message);

                        // Проверяем, что сообщение не от нас (если есть sender)
                        if (message.sender && message.sender.id !== state.user.id) {
                            addMessageToChat(message, false);
                        } else if (message.type === 'JOIN' || message.type === 'LEAVE') {
                            // Системное сообщение
                            addSystemMessage(message.message);
                        }
                    } catch(e) {
                        console.error('Parse error:', e);
                    }
                });

                // Отправляем уведомление о подключении с именем пользователя
                setTimeout(() => {
                    try {
                        state.stompClient.send('/app/chat.addUser', {}, JSON.stringify({
                            username: state.user.username
                        }));
                    } catch(e) {
                        console.error('Send join error:', e);
                    }
                }, 500);
            },
            function onError() {
                console.log('❌ WebSocket disconnected');
                state.connected = false;
                updateConnectionStatus(false);
                setTimeout(connectWebSocket, 5000);
            }
        );
    } catch (error) {
        console.error('WebSocket connection error:', error);
        state.connected = false;
        updateConnectionStatus(false);
        setTimeout(connectWebSocket, 5000);
    }
}

function updateConnectionStatus(connected) {
    const status = document.getElementById('connectionStatus');
    if (status) {
        status.innerHTML = connected
            ? '🟢 Подключено'
            : '🔴 Переподключение...';
    }
}

// ===== ЗАГРУЗКА СООБЩЕНИЙ =====
async function loadGroupMessages() {
    try {
        const response = await fetch(`${API_URL}/messages/group/recent?limit=50`, {
            headers: { 'Authorization': `Bearer ${state.token}` }
        });

        if (!response.ok) throw new Error('Ошибка загрузки сообщений');

        const messages = await response.json();
        messagesList.innerHTML = '';
        if (messages.length === 0) {
            messagesList.innerHTML = `
                <div class="message system">
                    <div class="msg-content" style="text-align:center;color:var(--text-muted);">
                        💜 Добро пожаловать в VioletChat!<br>
                        Начните общение прямо сейчас!
                    </div>
                </div>
            `;
        }
        messages.reverse().forEach(msg => {
            const isSelf = msg.sender.id === state.user.id;
            addMessageToChat(msg, isSelf);
        });
    } catch (error) {
        console.error('Load messages error:', error);
    }
}

// ===== ДОБАВЛЕНИЕ СООБЩЕНИЯ =====
function addMessageToChat(message, isSelf = false) {
    const html = `
        <div class="message ${isSelf ? 'self' : 'other'}">
            ${!isSelf ? `<div class="msg-sender">${escapeHtml(message.sender.username)}</div>` : ''}
            <div class="msg-content">${escapeHtml(message.content)}</div>
            <div class="msg-time">${formatTime(message.createdAt)}</div>
        </div>
    `;
    messagesList.insertAdjacentHTML('beforeend', html);
    scrollToBottom();
}

function addSystemMessage(text) {
    const html = `
        <div class="message system">
            <div class="msg-content" style="text-align:center;color:var(--text-muted);font-style:italic;">
                ${escapeHtml(text)}
            </div>
        </div>
    `;
    messagesList.insertAdjacentHTML('beforeend', html);
    scrollToBottom();
}

function scrollToBottom() {
    const container = document.getElementById('messagesContainer');
    setTimeout(() => {
        container.scrollTop = container.scrollHeight;
    }, 50);
}

// ===== ОТПРАВКА СООБЩЕНИЯ =====
async function sendMessage() {
    const content = messageInput.value.trim();
    if (!content) return;

    sendBtn.disabled = true;

    const payload = {
        content: content,
        groupMessage: state.currentChat === 'group',
        receiverId: state.currentChat === 'private' ? state.activeUserId : null
    };

    try {
        // Пробуем отправить через WebSocket
        if (state.stompClient && state.connected) {
            state.stompClient.send('/app/chat.send', {}, JSON.stringify(payload));
            messageInput.value = '';

            // Показываем сообщение сразу (оптимистичное обновление)
            const tempMessage = {
                content: content,
                sender: state.user,
                createdAt: new Date().toISOString()
            };
            addMessageToChat(tempMessage, true);
        } else {
            // Если WebSocket не работает, отправляем через REST
            const response = await fetch(`${API_URL}/messages`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${state.token}`
                },
                body: JSON.stringify(payload)
            });

            if (!response.ok) throw new Error('Ошибка отправки');

            const msg = await response.json();
            addMessageToChat(msg, true);
            messageInput.value = '';
        }
    } catch (error) {
        console.error('Send error:', error);
        showToast('❌ Не удалось отправить сообщение', 'error');
    } finally {
        sendBtn.disabled = false;
    }
}

// ===== УТИЛИТЫ =====
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function formatTime(dateStr) {
    if (!dateStr) return '';
    try {
        const date = new Date(dateStr);
        return date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' });
    } catch(e) {
        return '';
    }
}

function showToast(text, type = 'info') {
    const existing = document.querySelector('.toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = text;
    document.body.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transition = 'opacity 0.3s';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// ===== ОБРАБОТЧИКИ =====

// Переключение вкладок
authTabs.forEach(btn => {
    btn.addEventListener('click', () => {
        switchAuthTab(btn.dataset.tab);
    });
});

// Вход
loginForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const username = $('#loginUsername').value.trim();
    const password = $('#loginPassword').value;
    if (username && password) {
        login(username, password);
    }
});

// Регистрация
registerForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const data = {
        username: $('#regUsername').value.trim(),
        email: $('#regEmail').value.trim(),
        password: $('#regPassword').value,
        firstName: $('#regFirstName').value.trim(),
        lastName: $('#regLastName').value.trim()
    };
    if (data.username && data.email && data.password) {
        register(data);
    }
});

// Выход
logoutBtn.addEventListener('click', logout);

// Отправка
sendBtn.addEventListener('click', sendMessage);
messageInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

// Навигация
document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const chat = btn.dataset.chat;
        if (chat === 'group') {
            state.currentChat = 'group';
            state.activeUserId = null;
            chatTitle.textContent = '💬 Общий чат';
            document.querySelectorAll('.nav-btn').forEach(b => {
                b.classList.toggle('active', b.dataset.chat === 'group');
            });
            loadGroupMessages();
        }
    });
});

// ===== ПРОВЕРКА АВТОРИЗАЦИИ =====
if (state.token && state.user) {
    initChat();
} else {
    authScreen.style.display = 'flex';
    chatScreen.style.display = 'none';
}

console.log('💜 VioletChat loaded!');