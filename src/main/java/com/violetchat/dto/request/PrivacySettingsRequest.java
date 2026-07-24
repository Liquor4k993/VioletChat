package com.violetchat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrivacySettingsRequest {
    private boolean allowPrivateMessages;
    private boolean showOnlineStatus;
    private boolean showLastSeen;
    private boolean allowFriendRequests;
}