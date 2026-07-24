package com.violetchat.service;

import com.violetchat.dto.request.PrivacySettingsRequest;
import com.violetchat.dto.response.PrivacySettingsResponse;
import com.violetchat.entity.PrivacySettings;
import com.violetchat.entity.User;
import com.violetchat.repository.PrivacySettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrivacyService {

    private final PrivacySettingsRepository privacySettingsRepository;

    @Transactional
    public PrivacySettings getOrCreateSettings(User user) {
        return privacySettingsRepository.findByUser(user)
                .orElseGet(() -> {
                    PrivacySettings settings = PrivacySettings.builder()
                            .user(user)
                            .allowPrivateMessages(true)
                            .showOnlineStatus(true)
                            .showLastSeen(true)
                            .allowFriendRequests(true)
                            .build();
                    return privacySettingsRepository.save(settings);
                });
    }

    @Transactional
    public PrivacySettingsResponse updateSettings(Long userId, PrivacySettingsRequest request) {
        PrivacySettings settings = privacySettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Settings not found"));

        settings.setAllowPrivateMessages(request.isAllowPrivateMessages());
        settings.setShowOnlineStatus(request.isShowOnlineStatus());
        settings.setShowLastSeen(request.isShowLastSeen());
        settings.setAllowFriendRequests(request.isAllowFriendRequests());
        settings.setUpdatedAt(LocalDateTime.now());

        PrivacySettings saved = privacySettingsRepository.save(settings);
        log.info("Privacy settings updated for user: {}", userId);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PrivacySettingsResponse getSettings(Long userId) {
        PrivacySettings settings = privacySettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Settings not found"));
        return toResponse(settings);
    }

    @Transactional(readOnly = true)
    public boolean canSendPrivateMessage(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) return false;

        PrivacySettings settings = privacySettingsRepository.findByUserId(receiverId)
                .orElse(null);

        if (settings == null) return true;
        return settings.isAllowPrivateMessages();
    }

    private PrivacySettingsResponse toResponse(PrivacySettings settings) {
        return PrivacySettingsResponse.builder()
                .id(settings.getId())
                .allowPrivateMessages(settings.isAllowPrivateMessages())
                .showOnlineStatus(settings.isShowOnlineStatus())
                .showLastSeen(settings.isShowLastSeen())
                .allowFriendRequests(settings.isAllowFriendRequests())
                .updatedAt(settings.getUpdatedAt().toString())
                .build();
    }
}