package com.violetchat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettingsResponse {
    private Long id;
    private boolean allowPrivateMessages;
    private boolean showOnlineStatus;
    private boolean showLastSeen;
    private boolean allowFriendRequests;
    private String updatedAt;
}