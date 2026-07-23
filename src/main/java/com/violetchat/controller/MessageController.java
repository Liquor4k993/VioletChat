package com.violetchat.controller;

import com.violetchat.dto.request.MessageRequest;
import com.violetchat.dto.response.MessageResponse;
import com.violetchat.service.MessageService;
import com.violetchat.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        MessageResponse response = messageService.sendMessage(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/with/{receiverId}")
    public ResponseEntity<Page<MessageResponse>> getMessagesWithUser(
            @PathVariable Long receiverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = securityUtils.getCurrentUserId();
        Page<MessageResponse> messages = messageService.getMessagesBetweenUsers(userId, receiverId, page, size);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/group/recent")
    public ResponseEntity<List<MessageResponse>> getRecentGroupMessages(
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<MessageResponse> messages = messageService.getRecentGroupMessages(limit);
        return ResponseEntity.ok(messages);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long messageId) {
        Long userId = securityUtils.getCurrentUserId();
        messageService.deleteMessage(messageId, userId);
        return ResponseEntity.ok().build();
    }
}