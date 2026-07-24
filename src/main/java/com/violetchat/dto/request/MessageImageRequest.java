package com.violetchat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageImageRequest {
    private Long receiverId;
    @Builder.Default
    private boolean groupMessage = false;
    private MultipartFile image;

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public boolean isGroupMessage() { return groupMessage; }
    public void setGroupMessage(boolean groupMessage) { this.groupMessage = groupMessage; }

    public MultipartFile getImage() { return image; }
    public void setImage(MultipartFile image) { this.image = image; }
}