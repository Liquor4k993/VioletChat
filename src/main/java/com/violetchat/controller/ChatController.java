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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    // ===== SEND MESSAGE =====

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = (String) headerAccessor.getSessionAttributes().get("username");
            if (username == null) {
                log.warn("Username not found in session");
                return;
            }

            log.info("📨 Sending message from: {}", username);
            var user = userService.findByUsername(username);
            MessageResponse response = messageService.sendMessage(user.getId(), request);

            // Отправляем в общий чат
            messagingTemplate.convertAndSend("/topic/public", response);

            // Если есть получатель, отправляем личное сообщение
            if (request.getReceiverId() != null) {
                messagingTemplate.convertAndSendToUser(
                        request.getReceiverId().toString(),
                        "/queue/private",
                        response
                );
            }

            log.info("📨 Message sent: {}", response.getId());
        } catch (Exception e) {
            log.error("Send error: ", e);
        }
    }

    // ===== EDIT MESSAGE =====

    @MessageMapping("/chat.edit")
    public void editMessage(@Payload Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = (String) headerAccessor.getSessionAttributes().get("username");
            if (username == null) {
                log.warn("Username not found in session");
                return;
            }

            Long messageId = Long.valueOf(payload.get("messageId").toString());
            String content = payload.get("content").toString();

            log.info("✏️ Editing message: {}", messageId);

            MessageRequest request = MessageRequest.builder()
                    .content(content)
                    .build();

            var user = userService.findByUsername(username);
            MessageResponse response = messageService.editMessage(messageId, user.getId(), request);

            // Отправляем обновление всем
            messagingTemplate.convertAndSend("/topic/public", response);
            messagingTemplate.convertAndSendToUser(
                    user.getId().toString(),
                    "/queue/private",
                    response
            );

            log.info("✏️ Message edited: {}", messageId);
        } catch (Exception e) {
            log.error("Edit error: ", e);
        }
    }

    // ===== DELETE MESSAGE =====

    @MessageMapping("/chat.delete")
    public void deleteMessage(@Payload Map<String, Long> payload, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = (String) headerAccessor.getSessionAttributes().get("username");
            if (username == null) {
                log.warn("Username not found in session");
                return;
            }

            Long messageId = payload.get("messageId");
            log.info("🗑️ Deleting message: {}", messageId);

            var user = userService.findByUsername(username);
            messageService.deleteMessage(messageId, user.getId());

            // Отправляем уведомление об удалении
            Map<String, Object> deleteNotification = Map.of(
                    "type", "MESSAGE_DELETED",
                    "messageId", messageId
            );
            messagingTemplate.convertAndSend("/topic/public", deleteNotification);

            log.info("🗑️ Message deleted: {}", messageId);
        } catch (Exception e) {
            log.error("Delete error: ", e);
        }
    }

    // ===== ADD USER =====

    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public Map<String, Object> addUser(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = payload.get("username");
            if (username == null) {
                log.warn("Username is null in addUser");
                return Map.of("error", "Username required");
            }

            headerAccessor.getSessionAttributes().put("username", username);
            log.info("👤 User joined: {}", username);

            return Map.of(
                    "type", "JOIN",
                    "username", username,
                    "message", username + " присоединился! 💜"
            );
        } catch (Exception e) {
            log.error("Add user error: ", e);
            return Map.of("error", e.getMessage());
        }
    }

    // ===== TYPING INDICATOR =====

    @MessageMapping("/chat.typing")
    public void typing(@Payload Map<String, String> payload, SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = (String) headerAccessor.getSessionAttributes().get("username");
            if (username == null) return;

            String receiverId = payload.get("receiverId");
            if (receiverId != null) {
                messagingTemplate.convertAndSendToUser(
                        receiverId,
                        "/queue/typing",
                        Map.of("username", username, "typing", true)
                );
            }
        } catch (Exception e) {
            log.error("Typing error: ", e);
        }
    }
}