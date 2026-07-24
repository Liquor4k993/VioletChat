package com.violetchat.service;

import com.violetchat.dto.request.MessageImageRequest;
import com.violetchat.dto.request.MessageRequest;
import com.violetchat.dto.response.MessageResponse;
import com.violetchat.entity.Message;
import com.violetchat.entity.User;
import com.violetchat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final BlacklistService blacklistService;
    private final ChatService chatService;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    // ===== SEND MESSAGE =====

    @Transactional
    public MessageResponse sendMessage(Long senderId, MessageRequest request) {
        User sender = userService.findById(senderId);
        User receiver = request.getReceiverId() != null ?
                userService.findById(request.getReceiverId()) : null;

        if (receiver != null) {
            if (blacklistService.isBlocked(receiver.getId(), sender.getId())) {
                throw new IllegalArgumentException("You are blocked by this user");
            }
        }

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .groupMessage(request.isGroupMessage())
                .messageType(request.getMessageType() != null ? request.getMessageType() : "TEXT")
                .mediaUrl(request.getMediaUrl())
                .build();

        Message saved = messageRepository.save(message);
        log.info("📨 Message saved: {}", saved.getId());

        MessageResponse response = toResponse(saved);

        if (receiver != null) {
            chatService.sendPrivateMessage(response, receiver.getId());
        } else {
            chatService.broadcastNewMessage(response);
        }

        return response;
    }

    // ===== SEND IMAGE MESSAGE =====

    @Transactional
    public MessageResponse sendImageMessage(Long senderId, MessageImageRequest request) throws IOException {
        User sender = userService.findById(senderId);
        User receiver = request.getReceiverId() != null ?
                userService.findById(request.getReceiverId()) : null;

        if (receiver != null) {
            if (blacklistService.isBlocked(receiver.getId(), sender.getId())) {
                throw new IllegalArgumentException("You are blocked by this user");
            }
        }

        String imageUrl = saveImage(request.getImage());

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content("📷 Image")
                .groupMessage(request.isGroupMessage())
                .messageType("IMAGE")
                .mediaUrl(imageUrl)
                .build();

        Message saved = messageRepository.save(message);
        MessageResponse response = toResponse(saved);

        if (receiver != null) {
            chatService.sendPrivateMessage(response, receiver.getId());
        } else {
            chatService.broadcastNewMessage(response);
        }

        return response;
    }

    // ===== EDIT MESSAGE =====

    @Transactional
    public MessageResponse editMessage(Long messageId, Long userId, MessageRequest request) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot edit this message");
        }

        if (message.isDeleted()) {
            throw new IllegalArgumentException("Message is deleted");
        }

        message.setContent(request.getContent());
        message.setEditedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());

        Message saved = messageRepository.save(message);
        MessageResponse response = toResponse(saved);

        if (message.getReceiver() != null) {
            chatService.sendPrivateMessage(response, message.getReceiver().getId());
        } else {
            chatService.broadcastNewMessage(response);
        }

        return response;
    }

    // ===== DELETE MESSAGE (soft) =====

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot delete this message");
        }

        message.setDeleted(true);
        message.setContent("Message deleted");
        message.setUpdatedAt(LocalDateTime.now());

        messageRepository.save(message);
        log.info("🗑️ Message {} marked as deleted", messageId);

        MessageResponse response = toResponse(message);
        if (message.getReceiver() != null) {
            chatService.sendPrivateMessage(response, message.getReceiver().getId());
        } else {
            chatService.broadcastNewMessage(response);
        }
    }

    // ===== HARD DELETE MESSAGE =====

    @Transactional
    public void hardDeleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot delete this message");
        }

        messageRepository.delete(message);
        log.info("🗑️ Message {} permanently deleted", messageId);
    }

    // ===== GET MESSAGES BETWEEN USERS (with pagination) =====

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesBetweenUsers(Long senderId, Long receiverId, int page, int size) {
        User sender = userService.findById(senderId);
        User receiver = userService.findById(receiverId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Message> messages = messageRepository.findBySenderAndReceiverOrderByCreatedAtDesc(sender, receiver, pageable);
        return messages.map(this::toResponse);
    }

    // ===== GET CHAT HISTORY =====

    @Transactional(readOnly = true)
    public List<MessageResponse> getChatHistory(Long userId, Long receiverId, int limit) {
        User user = userService.findById(userId);
        User receiver = userService.findById(receiverId);
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        Page<Message> messages = messageRepository.findBySenderAndReceiverOrderByCreatedAtDesc(user, receiver, pageable);
        return messages.getContent().stream()
                .filter(m -> !m.isDeleted())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ===== GET RECENT GROUP MESSAGES =====

    @Transactional(readOnly = true)
    public List<MessageResponse> getRecentGroupMessages(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        List<Message> messages = messageRepository.findRecentGroupMessages(pageable);
        return messages.stream()
                .filter(m -> !m.isDeleted())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ===== MARK AS READ =====

    @Transactional
    public void markMessageAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (message.getReceiver() != null &&
                message.getReceiver().getId().equals(userId)) {
            message.setRead(true);
            messageRepository.save(message);
            log.info("📖 Message {} marked as read", messageId);
        }
    }

    // ===== GET UNREAD COUNT =====

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        User user = userService.findById(userId);
        return messageRepository.countUnreadMessages(user);
    }

    // ===== GET MESSAGES WITH USER (without pagination) =====

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessagesWithUser(Long userId, Long otherUserId) {
        User user = userService.findById(userId);
        User other = userService.findById(otherUserId);
        Pageable pageable = PageRequest.of(0, 100, Sort.by("createdAt").descending());
        Page<Message> messages = messageRepository.findBySenderAndReceiverOrderByCreatedAtDesc(user, other, pageable);
        return messages.getContent().stream()
                .filter(m -> !m.isDeleted())
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ===== PRIVATE METHODS =====

    private String saveImage(MultipartFile file) throws IOException {
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        Path filePath = uploadDir.resolve(filename);
        Files.write(filePath, file.getBytes());

        return "/uploads/" + filename;
    }

    private MessageResponse toResponse(Message message) {
        if (message == null) return null;
        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .sender(userService.toResponse(message.getSender()))
                .receiver(message.getReceiver() != null ?
                        userService.toResponse(message.getReceiver()) : null)
                .groupMessage(message.isGroupMessage())
                .messageType(message.getMessageType())
                .mediaUrl(message.getMediaUrl())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .deleted(message.isDeleted())
                .read(message.isRead())
                .editedAt(message.getEditedAt())
                .build();
    }
}