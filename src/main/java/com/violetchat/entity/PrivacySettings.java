package com.violetchat.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "privacy_settings")
public class PrivacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "allow_private_messages")
    @Builder.Default
    private boolean allowPrivateMessages = true;

    @Column(name = "show_online_status")
    @Builder.Default
    private boolean showOnlineStatus = true;

    @Column(name = "show_last_seen")
    @Builder.Default
    private boolean showLastSeen = true;

    @Column(name = "allow_friend_requests")
    @Builder.Default
    private boolean allowFriendRequests = true;

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}