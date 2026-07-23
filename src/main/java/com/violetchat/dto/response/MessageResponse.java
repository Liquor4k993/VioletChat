package com.violetchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private String content;
    private UserResponse sender;
    private UserResponse receiver;
    private boolean groupMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}