package com.violetchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {

    @NotBlank(message = "Содержимое сообщения обязательно")
    @Size(max = 1000, message = "Сообщение не должно превышать 1000 символов")
    private String content;

    private Long receiverId;

    @Builder.Default
    private boolean groupMessage = false;
}