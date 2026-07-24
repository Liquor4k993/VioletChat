package com.violetchat.controller;

import com.violetchat.dto.request.ChangePasswordRequest;
import com.violetchat.dto.request.UpdateProfileRequest;
import com.violetchat.dto.response.BlacklistResponse;
import com.violetchat.dto.response.FriendResponse;
import com.violetchat.dto.response.UserResponse;
import com.violetchat.entity.User;
import com.violetchat.service.BlacklistService;
import com.violetchat.service.FriendService;
import com.violetchat.service.UserService;
import com.violetchat.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FriendService friendService;
    private final BlacklistService blacklistService;
    private final SecurityUtils securityUtils;
    private final PasswordEncoder passwordEncoder;

    // ===== PROFILE =====

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        User user = userService.findById(securityUtils.getCurrentUserId());
        return ResponseEntity.ok(userService.toResponse(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.findById(securityUtils.getCurrentUserId());

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getUsername() != null) {
            user.setUsername(request.getUsername());
        }

        User updated = userService.updateUser(user);
        log.info("👤 User {} updated profile", updated.getUsername());
        return ResponseEntity.ok(userService.toResponse(updated));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            User user = userService.findById(securityUtils.getCurrentUserId());

            if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                return ResponseEntity.status(400)
                        .body(Map.of("error", "Неверный текущий пароль"));
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userService.updateUser(user);

            log.info("🔑 Password changed for user: {}", user.getUsername());
            return ResponseEntity.ok(Map.of("message", "Пароль успешно изменён"));
        } catch (Exception e) {
            log.error("Password change error: ", e);
            return ResponseEntity.status(400)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<UserResponse> updateAvatar(
            @RequestParam("avatar") String avatarUrl) {
        User user = userService.findById(securityUtils.getCurrentUserId());
        user.setAvatarUrl(avatarUrl);
        User updated = userService.updateUser(user);
        log.info("🖼️ Avatar updated for user: {}", user.getUsername());
        return ResponseEntity.ok(userService.toResponse(updated));
    }

    // ===== RECOMMENDATIONS =====

    @GetMapping("/recommendations")
    public ResponseEntity<List<UserResponse>> getRecommendations() {
        try {
            Long currentId = securityUtils.getCurrentUserId();
            List<User> recommended = userService.getRecommendedUsers(currentId, 6);
            return ResponseEntity.ok(recommended.stream()
                    .map(userService::toResponse)
                    .collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            log.error("Recommendations error: ", e);
            return ResponseEntity.ok(List.of());
        }
    }

    // ===== SEARCH =====

    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(@RequestParam String query) {
        try {
            Long currentId = securityUtils.getCurrentUserId();
            List<User> users = userService.searchUsers(query);
            return ResponseEntity.ok(users.stream()
                    .filter(u -> !u.getId().equals(currentId))
                    .map(userService::toResponse)
                    .collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            log.error("Search error: ", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long userId) {
        User user = userService.findById(userId);
        return ResponseEntity.ok(userService.toResponse(user));
    }

    @GetMapping("/by-username/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        User user = userService.findByUsername(username);
        return ResponseEntity.ok(userService.toResponse(user));
    }

    // ===== FRIENDS =====

    @PostMapping("/friends/request")
    public ResponseEntity<?> sendFriendRequest(@RequestParam String username) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.status(400)
                        .body(Map.of("error", "Username is required"));
            }

            FriendResponse response = friendService.sendFriendRequest(
                    securityUtils.getCurrentUserId(),
                    username.trim()
            );
            log.info("👫 Friend request sent to: {}", username);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Friend request error: ", e);
            return ResponseEntity.status(400)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/friends/accept/{requestId}")
    public ResponseEntity<FriendResponse> acceptFriendRequest(@PathVariable Long requestId) {
        try {
            FriendResponse response = friendService.acceptFriendRequest(
                    securityUtils.getCurrentUserId(),
                    requestId
            );
            log.info("✅ Friend request accepted: {}", requestId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Accept friend error: ", e);
            throw e;
        }
    }

    @PostMapping("/friends/reject/{requestId}")
    public ResponseEntity<Void> rejectFriendRequest(@PathVariable Long requestId) {
        try {
            friendService.rejectFriendRequest(securityUtils.getCurrentUserId(), requestId);
            log.info("❌ Friend request rejected: {}", requestId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Reject friend error: ", e);
            throw e;
        }
    }

    @DeleteMapping("/friends/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable Long friendId) {
        try {
            friendService.removeFriend(securityUtils.getCurrentUserId(), friendId);
            log.info("👋 Friend removed: {}", friendId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Remove friend error: ", e);
            throw e;
        }
    }

    @GetMapping("/friends")
    public ResponseEntity<List<UserResponse>> getFriends() {
        try {
            return ResponseEntity.ok(friendService.getFriends(securityUtils.getCurrentUserId()));
        } catch (Exception e) {
            log.error("Get friends error: ", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/friends/pending")
    public ResponseEntity<List<FriendResponse>> getPendingRequests() {
        try {
            return ResponseEntity.ok(
                    friendService.getPendingRequests(securityUtils.getCurrentUserId())
            );
        } catch (Exception e) {
            log.error("Get pending requests error: ", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/friends/check/{userId}")
    public ResponseEntity<Boolean> areFriends(@PathVariable Long userId) {
        try {
            boolean areFriends = friendService.areFriends(
                    securityUtils.getCurrentUserId(),
                    userId
            );
            return ResponseEntity.ok(areFriends);
        } catch (Exception e) {
            log.error("Check friends error: ", e);
            return ResponseEntity.ok(false);
        }
    }

    // ===== BLACKLIST =====

    @PostMapping("/blacklist/{blockedUserId}")
    public ResponseEntity<BlacklistResponse> blockUser(@PathVariable Long blockedUserId) {
        try {
            BlacklistResponse response = blacklistService.blockUser(
                    securityUtils.getCurrentUserId(),
                    blockedUserId
            );
            log.info("🚫 User blocked: {}", blockedUserId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Block user error: ", e);
            throw e;
        }
    }

    @DeleteMapping("/blacklist/{blockedUserId}")
    public ResponseEntity<Void> unblockUser(@PathVariable Long blockedUserId) {
        try {
            blacklistService.unblockUser(securityUtils.getCurrentUserId(), blockedUserId);
            log.info("🔓 User unblocked: {}", blockedUserId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Unblock user error: ", e);
            throw e;
        }
    }

    @GetMapping("/blacklist")
    public ResponseEntity<List<BlacklistResponse>> getBlockedUsers() {
        try {
            return ResponseEntity.ok(
                    blacklistService.getBlockedUsers(securityUtils.getCurrentUserId())
            );
        } catch (Exception e) {
            log.error("Get blocked users error: ", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/blacklist/check/{userId}")
    public ResponseEntity<Boolean> isBlocked(@PathVariable Long userId) {
        try {
            boolean isBlocked = blacklistService.isBlocked(
                    securityUtils.getCurrentUserId(),
                    userId
            );
            return ResponseEntity.ok(isBlocked);
        } catch (Exception e) {
            log.error("Check blocked error: ", e);
            return ResponseEntity.ok(false);
        }
    }

    // ===== BULK OPERATIONS =====

    @GetMapping("/all")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        try {
            Long currentId = securityUtils.getCurrentUserId();
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users.stream()
                    .filter(u -> !u.getId().equals(currentId))
                    .map(userService::toResponse)
                    .collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            log.error("Get all users error: ", e);
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/online")
    public ResponseEntity<List<UserResponse>> getOnlineUsers() {
        try {
            Long currentId = securityUtils.getCurrentUserId();
            List<User> onlineUsers = userService.getOnlineUsers();
            return ResponseEntity.ok(onlineUsers.stream()
                    .filter(u -> !u.getId().equals(currentId))
                    .map(userService::toResponse)
                    .collect(java.util.stream.Collectors.toList()));
        } catch (Exception e) {
            log.error("Get online users error: ", e);
            return ResponseEntity.ok(List.of());
        }
    }

    // ===== STATISTICS =====

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        try {
            Long currentId = securityUtils.getCurrentUserId();
            Map<String, Object> stats = Map.of(
                    "totalUsers", userService.getTotalUsers(),
                    "totalFriends", friendService.getFriends(currentId).size(),
                    "totalBlocked", blacklistService.getBlockedUsers(currentId).size(),
                    "onlineUsers", userService.getOnlineUsers().size()
            );
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Get stats error: ", e);
            return ResponseEntity.ok(Map.of());
        }
    }
}