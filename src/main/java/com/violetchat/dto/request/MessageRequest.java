package com.violetchat.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    @NotBlank
    private String content;

    private Long receiverId;

    @Builder.Default
    private boolean groupMessage = true;

    @Builder.Default
    private String messageType = "TEXT";

    private String mediaUrl;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public boolean isGroupMessage() { return groupMessage; }
    public void setGroupMessage(boolean groupMessage) { this.groupMessage = groupMessage; }

    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }

    public String getMediaUrl() { return mediaUrl; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
}