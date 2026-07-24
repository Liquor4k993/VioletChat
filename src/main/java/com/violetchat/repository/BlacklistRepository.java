package com.violetchat.repository;

import com.violetchat.entity.Blacklist;
import com.violetchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

    Optional<Blacklist> findByUserAndBlockedUser(User user, User blockedUser);
    List<Blacklist> findByUser(User user);
    boolean existsByUserAndBlockedUser(User user, User blockedUser);
    void deleteByUserAndBlockedUser(User user, User blockedUser);
}