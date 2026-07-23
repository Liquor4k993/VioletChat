package com.violetchat.controller;

import com.violetchat.dto.request.ChangePasswordRequest;
import com.violetchat.dto.response.UserResponse;
import com.violetchat.entity.User;
import com.violetchat.service.UserService;
import com.violetchat.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Long userId = securityUtils.getCurrentUserId();
        User user = userService.findById(userId);
        return ResponseEntity.ok(userService.convertToResponse(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(@RequestBody User update) {
        Long userId = securityUtils.getCurrentUserId();
        User updatedUser = userService.updateUser(userId, update);
        return ResponseEntity.ok(userService.convertToResponse(updatedUser));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        userService.updatePassword(userId, request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(userService.convertToResponse(user));
    }
}