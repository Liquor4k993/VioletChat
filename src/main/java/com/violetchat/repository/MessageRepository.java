package com.violetchat.repository;

import com.violetchat.entity.Message;
import com.violetchat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findBySenderAndReceiverOrderByCreatedAtDesc(User sender, User receiver, Pageable pageable);

    Page<Message> findByGroupMessageTrueOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.groupMessage = true AND m.deleted = false ORDER BY m.createdAt DESC")
    List<Message> findRecentGroupMessages(Pageable pageable);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver = :user AND m.read = false AND m.deleted = false")
    long countUnreadMessages(@Param("user") User user);
}