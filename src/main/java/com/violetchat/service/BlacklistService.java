package com.violetchat.service;

import com.violetchat.dto.response.BlacklistResponse;
import com.violetchat.entity.Blacklist;
import com.violetchat.entity.User;
import com.violetchat.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final UserService userService;

    @Transactional
    public BlacklistResponse blockUser(Long userId, Long blockedUserId) {
        User user = userService.findById(userId);
        User blocked = userService.findById(blockedUserId);

        if (blacklistRepository.existsByUserAndBlockedUser(user, blocked)) {
            throw new IllegalArgumentException("Already blocked");
        }

        Blacklist blacklist = Blacklist.builder()
                .user(user)
                .blockedUser(blocked)
                .reason("Blocked")
                .build();

        Blacklist saved = blacklistRepository.save(blacklist);
        log.info("🚫 User {} blocked {}", userId, blockedUserId);
        return toResponse(saved);
    }

    @Transactional
    public void unblockUser(Long userId, Long blockedUserId) {
        User user = userService.findById(userId);
        User blocked = userService.findById(blockedUserId);
        blacklistRepository.deleteByUserAndBlockedUser(user, blocked);
        log.info("🔓 User {} unblocked {}", userId, blockedUserId);
    }

    @Transactional(readOnly = true)
    public List<BlacklistResponse> getBlockedUsers(Long userId) {
        User user = userService.findById(userId);
        return blacklistRepository.findByUser(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(Long userId, Long blockedUserId) {
        User user = userService.findById(userId);
        User blocked = userService.findById(blockedUserId);
        return blacklistRepository.existsByUserAndBlockedUser(user, blocked);
    }

    private BlacklistResponse toResponse(Blacklist blacklist) {
        return BlacklistResponse.builder()
                .id(blacklist.getId())
                .blockedUser(userService.toResponse(blacklist.getBlockedUser()))
                .reason(blacklist.getReason())
                .createdAt(blacklist.getCreatedAt().toString())
                .build();
    }
}