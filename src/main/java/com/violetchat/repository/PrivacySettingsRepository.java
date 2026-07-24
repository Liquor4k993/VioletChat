package com.violetchat.repository;

import com.violetchat.entity.PrivacySettings;
import com.violetchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrivacySettingsRepository extends JpaRepository<PrivacySettings, Long> {

    Optional<PrivacySettings> findByUser(User user);

    Optional<PrivacySettings> findByUserId(Long userId);
}