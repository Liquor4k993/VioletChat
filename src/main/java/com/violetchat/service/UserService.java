package com.violetchat.service;

import com.violetchat.dto.request.RegisterRequest;
import com.violetchat.dto.response.UserResponse;
import com.violetchat.entity.User;
import com.violetchat.exception.ResourceNotFoundException;
import com.violetchat.mapper.UserMapper;
import com.violetchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(User.Role.USER)
                .active(true)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered: {}", savedUser.getUsername());
        return savedUser;
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    @Transactional
    public User updateUser(Long id, User update) {
        User user = findById(id);
        if (update.getFirstName() != null) user.setFirstName(update.getFirstName());
        if (update.getLastName() != null) user.setLastName(update.getLastName());
        if (update.getEmail() != null && !update.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(update.getEmail())) {
                throw new IllegalArgumentException("Email already in use: " + update.getEmail());
            }
            user.setEmail(update.getEmail());
        }
        if (update.getAvatarUrl() != null) user.setAvatarUrl(update.getAvatarUrl());
        user.setLastActive(LocalDateTime.now());

        User updatedUser = userRepository.save(user);
        log.info("User updated: {}", updatedUser.getUsername());
        return updatedUser;
    }

    @Transactional
    public void updatePassword(Long id, String oldPassword, String newPassword) {
        User user = findById(id);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Invalid old password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password updated for user: {}", user.getUsername());
    }

    @Transactional
    public void updateLastActive(String username) {
        User user = findByUsername(username);
        user.setLastActive(LocalDateTime.now());
        userRepository.save(user);
    }

    public UserResponse convertToResponse(User user) {
        return userMapper.toResponse(user);
    }
}