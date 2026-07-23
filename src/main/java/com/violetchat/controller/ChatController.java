package com.violetchat.controller;

import com.violetchat.dto.request.MessageRequest;
import com.violetchat.dto.response.MessageResponse;
import com.violetchat.service.MessageService;
import com.violetchat.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final UserService userService;

    /**
     * Отправка сообщения в общий чат
     */
    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public MessageResponse sendMessage(@Payload MessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        try {
            // Получаем имя пользователя из сессии WebSocket
            String username = (String) headerAccessor.getSessionAttributes().get("username");
            if (username == null) {
                log.error("❌ Username not found in session");
                return null;
            }

            log.info("📨 Сообщение от {}: {}", username, request.getContent());

            // Получаем пользователя
            var user = userService.findByUsername(username);

            // Создаём сообщение
            MessageResponse response = messageService.sendMessage(user.getId(), request);

            return response;
        } catch (Exception e) {
            log.error("❌ Ошибка отправки сообщения: ", e);
            return null;
        }
    }

    /**
     * Подключение пользователя
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public Map<String, Object> addUser(@Payload Map<String, String> payload,
                                       SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = payload.get("username");
            if (username == null) {
                log.error("❌ Username not provided");
                return Map.of("error", "Username required");
            }

            log.info("👤 Пользователь подключился: {}", username);

            // Сохраняем имя в сессии WebSocket
            headerAccessor.getSessionAttributes().put("username", username);

            return Map.of(
                    "type", "JOIN",
                    "username", username,
                    "message", username + " присоединился к чату! 💜",
                    "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("❌ Ошибка подключения: ", e);
            return Map.of("error", e.getMessage());
        }
    }

    /**
     * Отключение пользователя
     */
    @MessageMapping("/chat.removeUser")
    @SendTo("/topic/public")
    public Map<String, Object> removeUser(SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = (String) headerAccessor.getSessionAttributes().get("username");
            if (username == null) {
                return Map.of("error", "User not found");
            }

            log.info("👋 Пользователь отключился: {}", username);

            return Map.of(
                    "type", "LEAVE",
                    "username", username,
                    "message", username + " покинул чат",
                    "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            log.error("❌ Ошибка отключения: ", e);
            return Map.of("error", e.getMessage());
        }
    }
}