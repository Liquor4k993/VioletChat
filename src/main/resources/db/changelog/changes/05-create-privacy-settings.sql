CREATE TABLE IF NOT EXISTS privacy_settings (
                                                id BIGSERIAL PRIMARY KEY,
                                                user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    allow_private_messages BOOLEAN DEFAULT TRUE,
    show_online_status BOOLEAN DEFAULT TRUE,
    show_last_seen BOOLEAN DEFAULT TRUE,
    allow_friend_requests BOOLEAN DEFAULT TRUE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS idx_privacy_settings_user_id ON privacy_settings(user_id);