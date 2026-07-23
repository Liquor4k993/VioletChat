package com.violetchat.service;

import com.violetchat.dto.response.MessageResponse;
import com.violetchat.entity.Message;
import com.violetchat.mapper.MessageMapper;
import com.violetchat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final MessageMapper messageMapper;
    private final UserService userService;

    /**
     * Трансляция нового сообщения всем подписчикам
     */
    public void broadcastNewMessage(MessageResponse message) {
        try {
            // Отправляем в общий чат
            messagingTemplate.convertAndSend("/topic/public", message);
            log.info("📡 Сообщение транслировано в /topic/public");
        } catch (Exception e) {
            log.error("Ошибка трансляции: ", e);
        }
    }

    /**
     * Отправка личного сообщения
     */
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

    /**
     * Отправка статуса "печатает"
     */
    public void sendTypingStatus(Long senderId, String username) {
        try {
            Map<String, Object> typingData = new HashMap<>();
            typingData.put("username", username);
            typingData.put("typing", true);

            messagingTemplate.convertAndSend("/topic/typing", typingData);
        } catch (Exception e) {
            log.error("Ошибка отправки статуса печатания: ", e);
        }
    }
}