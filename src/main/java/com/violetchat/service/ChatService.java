package com.violetchat.service;

import com.violetchat.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastNewMessage(MessageResponse message) {
        try {
            messagingTemplate.convertAndSend("/topic/public", message);
            log.info("📡 Сообщение транслировано в /topic/public");
        } catch (Exception e) {
            log.error("Ошибка трансляции: ", e);
        }
    }

    public void sendPrivateMessage(MessageResponse message, Long receiverId) {
        try {
            messagingTemplate.convertAndSendToUser(
                    receiverId.toString(),
                    "/queue/private",
                    message
            );
            log.info("💌 Личное сообщение отправлено пользователю {}", receiverId);
        } catch (Exception e) {
            log.error("Ошибка отправки личного сообщения: ", e);
        }
    }

    public void sendTypingStatus(Long senderId, String username) {
        try {
            java.util.Map<String, Object> typingData = new java.util.HashMap<>();
            typingData.put("username", username);
            typingData.put("typing", true);

            messagingTemplate.convertAndSend("/topic/typing", typingData);
        } catch (Exception e) {
            log.error("Ошибка отправки статуса печатания: ", e);
        }
    }
}