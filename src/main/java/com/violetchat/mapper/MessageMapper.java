package com.violetchat.mapper;

import com.violetchat.dto.response.MessageResponse;
import com.violetchat.entity.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Маппер для преобразования Message в MessageResponse.
 * Использует ручное преобразование.
 */
@Component
@RequiredArgsConstructor
public class MessageMapper {

    private final UserMapper userMapper;

    /**
     * Преобразует сущность Message в DTO MessageResponse.
     *
     * @param message сущность сообщения
     * @return DTO сообщения
     */
    public MessageResponse toResponse(Message message) {
        if (message == null) {
            return null;
        }

        return MessageResponse.builder()
                .id(message.getId())
                .content(message.getContent())
                .sender(userMapper.toResponse(message.getSender()))
                .receiver(message.getReceiver() != null ?
                        userMapper.toResponse(message.getReceiver()) : null)
                .groupMessage(message.isGroupMessage())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}