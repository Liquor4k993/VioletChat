package com.violetchat.service;

import com.violetchat.dto.request.MessageRequest;
import com.violetchat.dto.response.MessageResponse;
import com.violetchat.entity.Message;
import com.violetchat.entity.User;
import com.violetchat.mapper.MessageMapper;
import com.violetchat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final MessageMapper messageMapper;

    /**
     * Находит сообщение по ID.
     *
     * @param messageId ID сообщения
     * @return найденное сообщение
     * @throws IllegalArgumentException если сообщение не найдено
     */
    @Transactional(readOnly = true)
    public Message findById(Long messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));
    }

    @Transactional
    public MessageResponse sendMessage(Long senderId, MessageRequest request) {
        User sender = userService.findById(senderId);
        User receiver = null;

        if (request.getReceiverId() != null) {
            receiver = userService.findById(request.getReceiverId());
        }

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .groupMessage(request.isGroupMessage())
                .build();

        Message savedMessage = messageRepository.save(message);
        log.info("Message sent from {} to {}", sender.getUsername(),
                receiver != null ? receiver.getUsername() : "group");

        return messageMapper.toResponse(savedMessage);
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> getMessagesBetweenUsers(Long senderId, Long receiverId, int page, int size) {
        User sender = userService.findById(senderId);
        User receiver = userService.findById(receiverId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Message> messages = messageRepository.findBySenderAndReceiverOrderByCreatedAtDesc(
                sender, receiver, pageable
        );

        return messages.map(messageMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getRecentGroupMessages(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        List<Message> messages = messageRepository.findRecentGroupMessages(pageable);
        return messages.stream()
                .map(messageMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (!message.getSender().getId().equals(userId)) {
            throw new IllegalArgumentException("You can only delete your own messages");
        }

        messageRepository.delete(message);
        log.info("Message {} deleted by user {}", messageId, userId);
    }
}