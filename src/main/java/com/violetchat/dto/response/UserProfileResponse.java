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
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String avatarUrl;
    private byte[] avatar;
    private String avatarContentType;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime lastActive;
    private boolean active;
    private boolean isFriend;
    private boolean isBlocked;
    private PrivacySettingsResponse privacy;
}