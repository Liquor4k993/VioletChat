package com.violetchat.controller;

import com.violetchat.dto.request.MessageImageRequest;
import com.violetchat.dto.request.MessageRequest;
import com.violetchat.dto.response.MessageResponse;
import com.violetchat.service.MessageService;
import com.violetchat.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SecurityUtils securityUtils;

    // ===== SEND MESSAGE =====

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request) {
        try {
            log.info("Sending message from user: {}", securityUtils.getCurrentUserId());
            MessageResponse response = messageService.sendMessage(securityUtils.getCurrentUserId(), request);
            log.info("Message sent: {}", response.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error sending message: ", e);
            throw e;
        }
    }

    // ===== SEND IMAGE MESSAGE =====

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageResponse> sendImageMessage(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "receiverId", required = false) Long receiverId,
            @RequestParam(value = "groupMessage", defaultValue = "false") boolean groupMessage) {
        try {
            log.info("Sending image message from user: {}", securityUtils.getCurrentUserId());

            MessageImageRequest request = MessageImageRequest.builder()
                    .receiverId(receiverId)
                    .groupMessage(groupMessage)
                    .image(image)
                    .build();

            MessageResponse response = messageService.sendImageMessage(securityUtils.getCurrentUserId(), request);
            log.info("Image message sent: {}", response.getId());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error sending image message: ", e);
            throw new RuntimeException("Failed to send image: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error sending image message: ", e);
            throw e;
        }
    }

    // ===== GET RECENT GROUP MESSAGES =====

    @GetMapping("/group/recent")
    public ResponseEntity<List<MessageResponse>> getRecentGroupMessages(
            @RequestParam(defaultValue = "50") int limit) {
        try {
            log.info("Getting recent group messages, limit: {}", limit);
            List<MessageResponse> messages = messageService.getRecentGroupMessages(limit);
            log.info("Found {} group messages", messages.size());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting group messages: ", e);
            return ResponseEntity.ok(List.of());
        }
    }

    // ===== GET MESSAGES WITH USER (with pagination) =====

    @GetMapping("/with/{receiverId}")
    public ResponseEntity<Page<MessageResponse>> getMessagesWithUser(
            @PathVariable Long receiverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            log.info("Getting messages with user: {}", receiverId);
            Page<MessageResponse> messages = messageService.getMessagesBetweenUsers(
                    securityUtils.getCurrentUserId(),
                    receiverId,
                    page,
                    size
            );
            log.info("Found {} messages", messages.getTotalElements());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting messages with user: ", e);
            return ResponseEntity.ok(Page.empty());
        }
    }

    // ===== GET CHAT HISTORY =====

    @GetMapping("/history/{receiverId}")
    public ResponseEntity<List<MessageResponse>> getChatHistory(
            @PathVariable Long receiverId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            log.info("Getting chat history with user: {}, limit: {}", receiverId, limit);
            List<MessageResponse> messages = messageService.getChatHistory(
                    securityUtils.getCurrentUserId(),
                    receiverId,
                    limit
            );
            log.info("Found {} messages in history", messages.size());
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            log.error("Error getting chat history: ", e);
            return ResponseEntity.ok(List.of());
        }
    }

    // ===== MARK AS READ =====

    @PostMapping("/{messageId}/read")
    public ResponseEntity<Void> markMessageAsRead(@PathVariable Long messageId) {
        try {
            log.info("Marking message as read: {}", messageId);
            messageService.markMessageAsRead(messageId, securityUtils.getCurrentUserId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking message as read: ", e);
            return ResponseEntity.ok().build();
        }
    }

    // ===== EDIT MESSAGE =====

    @PutMapping("/{messageId}")
    public ResponseEntity<MessageResponse> editMessage(
            @PathVariable Long messageId,
            @RequestBody MessageRequest request) {
        try {
            log.info("Editing message: {}", messageId);
            MessageResponse response = messageService.editMessage(
                    messageId,
                    securityUtils.getCurrentUserId(),
                    request
            );
            log.info("Message edited: {}", messageId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error editing message: ", e);
            throw e;
        }
    }

    // ===== DELETE MESSAGE (soft) =====

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        try {
            log.info("Deleting message: {}", messageId);
            messageService.deleteMessage(messageId, securityUtils.getCurrentUserId());
            log.info("Message deleted: {}", messageId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting message: ", e);
            throw e;
        }
    }

    // ===== HARD DELETE MESSAGE =====

    @DeleteMapping("/{messageId}/hard")
    public ResponseEntity<Void> hardDeleteMessage(@PathVariable Long messageId) {
        try {
            log.info("Hard deleting message: {}", messageId);
            messageService.hardDeleteMessage(messageId, securityUtils.getCurrentUserId());
            log.info("Message permanently deleted: {}", messageId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error hard deleting message: ", e);
            throw e;
        }
    }

    // ===== GET UNREAD COUNT =====

    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount() {
        try {
            long count = messageService.getUnreadCount(securityUtils.getCurrentUserId());
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error getting unread count: ", e);
            return ResponseEntity.ok(0L);
        }
    }
}