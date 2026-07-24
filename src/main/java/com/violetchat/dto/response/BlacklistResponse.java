package com.violetchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistResponse {
    private Long id;
    private UserResponse blockedUser;
    private String reason;
    private String createdAt;
}