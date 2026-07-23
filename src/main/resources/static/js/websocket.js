/**
 * WebSocket модуль для VioletChat
 * Управляет подключением и обменом сообщениями в реальном времени
 */

const WS_URL = `ws://${window.location.host}/api/ws`;

let ws = null;
let reconnectAttempts = 0;
const MAX_RECONNECT_ATTEMPTS = 5;
const RECONNECT_DELAY = 3000;

/**
 * Подключается к WebSocket серверу
 */
function connectWebSocket() {
    if (ws && ws.readyState === WebSocket.OPEN) {
        return;
    }

    try {
        ws = new WebSocket(WS_URL);

        ws.onopen = () => {
            console.log('✅ WebSocket connected');
            reconnectAttempts = 0;
            // Отправляем токен для аутентификации
            const token = localStorage.getItem('violetchat_token');
            if (token) {
                ws.send(JSON.stringify({
                    type: 'AUTH',
                    token: token
                }));
            }
        };

        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                handleWebSocketMessage(data);
            } catch (e) {
                console.error('WebSocket parse error:', e);
            }
        };

        ws.onerror = (error) => {
            console.error('WebSocket error:', error);
        };

        ws.onclose = () => {
            console.warn('⚠️ WebSocket disconnected');
            attemptReconnect();
        };
    } catch (error) {
        console.error('WebSocket connection error:', error);
        attemptReconnect();
    }
}

/**
 * Попытка переподключения
 */
function attemptReconnect() {
    if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
        console.error('❌ Max reconnection attempts reached');
        return;
    }

    reconnectAttempts++;
    console.log(`🔄 Reconnecting... Attempt ${reconnectAttempts}/${MAX_RECONNECT_ATTEMPTS}`);

    setTimeout(() => {
        connectWebSocket();
    }, RECONNECT_DELAY * reconnectAttempts);
}

/**
 * Отправляет сообщение через WebSocket
 */
function sendWebSocketMessage(data) {
    if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(data));
        return true;
    } else {
        console.error('WebSocket is not connected');
        return false;
    }
}

/**
 * Обрабатывает входящие WebSocket сообщения
 */
function handleWebSocketMessage(data) {
    switch (data.type) {
        case 'AUTH_SUCCESS':
            console.log('✅ WebSocket authenticated');
            break;

        case 'AUTH_ERROR':
            console.error('❌ WebSocket authentication failed');
            break;

        case 'NEW_MESSAGE':
            // Добавляем новое сообщение в чат
            if (data.message && data.message.sender.id !== (window.state?.user?.id)) {
                addMessageToChat(data.message);
            }
            break;

        case 'USER_ONLINE':
        case 'USER_OFFLINE':
            updateOnlineUsers(data.users);
            break;

        case 'MESSAGE_DELETED':
            // Удаляем сообщение из чата
            removeMessageFromChat(data.messageId);
            break;

        case 'TYPING':
            showTypingIndicator(data.username);
            break;

        default:
            console.log('Unknown message type:', data.type);
    }
}

/**
 * Добавляет сообщение в интерфейс чата
 */
function addMessageToChat(message) {
    const messagesList = document.getElementById('messagesList');
    if (!messagesList) return;

    const isSelf = message.sender.id === (window.state?.user?.id);
    const html = `
        <div class="message ${isSelf ? 'self' : 'other'}" data-msgid="${message.id}">
            ${!isSelf ? `<div class="msg-sender">${message.sender.username}</div>` : ''}
            <div class="msg-content">${escapeHtml(message.content)}</div>
            <div class="msg-time">${formatTime(message.createdAt)}</div>
        </div>
    `;

    messagesList.insertAdjacentHTML('beforeend', html);
    const container = document.getElementById('messagesContainer');
    if (container) {
        container.scrollTop = container.scrollHeight;
    }
}

/**
 * Удаляет сообщение из интерфейса
 */
function removeMessageFromChat(messageId) {
    const msgElement = document.querySelector(`.message[data-msgid="${messageId}"]`);
    if (msgElement) {
        msgElement.style.opacity = '0';
        msgElement.style.transition = 'opacity 0.3s';
        setTimeout(() => msgElement.remove(), 300);
    }
}

/**
 * Показывает индикатор набора текста
 */
let typingTimeout = null;

function showTypingIndicator(username) {
    const indicator = document.getElementById('typingIndicator');
    if (!indicator) return;

    indicator.textContent = `${username} печатает...`;
    indicator.style.display = 'block';

    clearTimeout(typingTimeout);
    typingTimeout = setTimeout(() => {
        indicator.style.display = 'none';
    }, 3000);
}

// ===== Эскпорт функций =====
window.connectWebSocket = connectWebSocket;
window.sendWebSocketMessage = sendWebSocketMessage;