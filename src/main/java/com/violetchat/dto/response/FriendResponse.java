package com.violetchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponse {
    private Long id;
    private UserResponse user;
    private UserResponse friend;
    private String status;
    private String createdAt;
}