package com.violetchat.service;

import com.violetchat.dto.request.RegisterRequest;
import com.violetchat.dto.response.UserResponse;
import com.violetchat.entity.User;
import com.violetchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // ===== REGISTRATION =====

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
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

        User saved = userRepository.save(user);
        log.info("✅ User registered: {}", saved.getUsername());
        return saved;
    }

    // ===== FIND METHODS =====

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    // ===== UPDATE =====

    @Transactional
    public User updateUser(User user) {
        user.setLastActive(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Transactional
    public void updateLastActive(Long userId) {
        userRepository.updateLastActive(userId, LocalDateTime.now());
    }

    @Transactional
    public void updateActiveStatus(Long userId, boolean active) {
        userRepository.updateActiveStatus(userId, active);
    }

    // ===== SEARCH =====

    @Transactional(readOnly = true)
    public List<User> searchUsers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return userRepository.findAll();
        }
        return userRepository.searchUsers(query.trim());
    }

    @Transactional(readOnly = true)
    public List<User> searchUsersExcluding(String query, Long excludeId) {
        if (query == null || query.trim().isEmpty()) {
            return userRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(excludeId))
                    .collect(Collectors.toList());
        }
        return userRepository.searchUsersExcluding(query.trim(), excludeId);
    }

    // ===== GET ALL USERS =====

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<User> getActiveUsers() {
        return userRepository.findActiveRegularUsers();
    }

    // ===== ONLINE USERS =====

    @Transactional(readOnly = true)
    public List<User> getOnlineUsers() {
        // Пользователи, которые были активны в последние 5 минут
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        return userRepository.findByLastActiveAfter(fiveMinutesAgo);
    }

    @Transactional(readOnly = true)
    public long countOnlineUsers() {
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        return userRepository.countOnlineUsers(fiveMinutesAgo);
    }

    // ===== STATISTICS =====

    @Transactional(readOnly = true)
    public long getTotalUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long getActiveUsersCount() {
        return userRepository.countActiveUsers();
    }

    @Transactional(readOnly = true)
    public long getNewUsersSince(LocalDateTime date) {
        return userRepository.countNewUsersSince(date);
    }

    // ===== RECOMMENDATIONS =====

    @Transactional(readOnly = true)
    public List<User> getRecommendedUsers(Long userId, int limit) {
        // Получаем друзей пользователя
        List<Long> friendIds = getFriendIds(userId);

        // Получаем заблокированных пользователей
        List<Long> blockedIds = getBlockedUserIds(userId);

        List<Long> excludeIds = new java.util.ArrayList<>();
        excludeIds.add(userId);
        excludeIds.addAll(friendIds);
        excludeIds.addAll(blockedIds);

        Pageable pageable = PageRequest.of(0, limit);
        return userRepository.findRecommendedUsers(excludeIds, pageable);
    }

    @Transactional(readOnly = true)
    public List<User> getActiveRecommendedUsers(Long userId, int limit) {
        List<Long> friendIds = getFriendIds(userId);
        List<Long> blockedIds = getBlockedUserIds(userId);

        List<Long> excludeIds = new java.util.ArrayList<>();
        excludeIds.add(userId);
        excludeIds.addAll(friendIds);
        excludeIds.addAll(blockedIds);

        Pageable pageable = PageRequest.of(0, limit);
        return userRepository.findActiveUsersExcluding(excludeIds, pageable);
    }

    // ===== HELPER METHODS =====

    private List<Long> getFriendIds(Long userId) {
        // Этот метод должен использовать FriendService
        // Но чтобы избежать циклической зависимости, используем прямое обращение
        try {
            // Временное решение - получаем всех пользователей и фильтруем
            return userRepository.findAll().stream()
                    .filter(u -> !u.getId().equals(userId))
                    .map(User::getId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Could not get friend IDs: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Long> getBlockedUserIds(Long userId) {
        // Аналогично, временное решение
        return List.of();
    }

    // ===== CONVERT TO RESPONSE =====

    public UserResponse toResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .lastActive(user.getLastActive())
                .active(user.isActive())
                .build();
    }

    public List<UserResponse> toResponseList(List<User> users) {
        return users.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}