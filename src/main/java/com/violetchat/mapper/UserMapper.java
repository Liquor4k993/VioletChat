package com.violetchat.mapper;

import com.violetchat.dto.response.UserResponse;
import com.violetchat.entity.User;
import org.springframework.stereotype.Component;

/**
 * Маппер для преобразования User в UserResponse.
 * Использует ручное преобразование.
 */
@Component
public class UserMapper {

    /**
     * Преобразует сущность User в DTO UserResponse.
     *
     * @param user сущность пользователя
     * @return DTO пользователя
     */
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().name() : "USER")
                .createdAt(user.getCreatedAt())
                .lastActive(user.getLastActive())
                .active(user.isActive())
                .build();
    }
}