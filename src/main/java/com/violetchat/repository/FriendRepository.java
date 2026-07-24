package com.violetchat.repository;

import com.violetchat.entity.Friend;
import com.violetchat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    // ===== BASIC =====

    Optional<Friend> findByUserAndFriend(User user, User friend);

    List<Friend> findByUserAndStatus(User user, Friend.FriendStatus status);

    List<Friend> findByFriendAndStatus(User friend, Friend.FriendStatus status);

    // ===== ACCEPTED FRIENDS =====

    @Query("SELECT f FROM Friend f WHERE (f.user = :user OR f.friend = :user) AND f.status = 'ACCEPTED'")
    List<Friend> findAcceptedFriends(@Param("user") User user);

    @Query("SELECT COUNT(f) FROM Friend f WHERE (f.user = :user OR f.friend = :user) AND f.status = 'ACCEPTED'")
    long countAcceptedFriends(@Param("user") User user);

    // ===== PENDING REQUESTS =====

    @Query("SELECT f FROM Friend f WHERE f.friend = :user AND f.status = 'PENDING'")
    List<Friend> findPendingRequests(@Param("user") User user);

    @Query("SELECT COUNT(f) FROM Friend f WHERE f.friend = :user AND f.status = 'PENDING'")
    long countPendingRequests(@Param("user") User user);

    @Query("SELECT f FROM Friend f WHERE f.user = :user AND f.status = 'PENDING'")
    List<Friend> findSentRequests(@Param("user") User user);

    // ===== CHECK EXISTENCE =====

    boolean existsByUserAndFriendAndStatus(User user, User friend, Friend.FriendStatus status);

    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE " +
            "(f.user = :user1 AND f.friend = :user2 OR f.user = :user2 AND f.friend = :user1) " +
            "AND f.status = 'ACCEPTED'")
    boolean areFriends(@Param("user1") User user1, @Param("user2") User user2);

    // ===== DELETE =====

    void deleteByUserAndFriend(User user, User friend);

    @Query("DELETE FROM Friend f WHERE (f.user = :user AND f.friend = :friend) OR (f.user = :friend AND f.friend = :user)")
    void deleteFriendship(@Param("user") User user, @Param("friend") User friend);

    // ===== BULK =====

    @Query("SELECT f FROM Friend f WHERE f.user = :user AND f.status = :status")
    List<Friend> findByUserAndStatusWithUser(@Param("user") User user, @Param("status") Friend.FriendStatus status);

    @Query("SELECT f FROM Friend f WHERE f.friend = :user AND f.status = :status")
    List<Friend> findByFriendAndStatusWithUser(@Param("user") User user, @Param("status") Friend.FriendStatus status);

    // ===== STATISTICS =====

    @Query("SELECT COUNT(f) FROM Friend f WHERE f.status = 'PENDING'")
    long countTotalPendingRequests();

    @Query("SELECT COUNT(f) FROM Friend f WHERE f.status = 'ACCEPTED'")
    long countTotalFriendships();

    // ===== RECOMMENDATIONS =====

    @Query("SELECT f.friend FROM Friend f WHERE f.user = :user AND f.status = 'ACCEPTED'")
    List<User> findAcceptedFriendUsers(@Param("user") User user);

    @Query("SELECT f.user FROM Friend f WHERE f.friend = :user AND f.status = 'ACCEPTED'")
    List<User> findUsersWhoAccepted(@Param("user") User user);

    // ===== IDS =====

    @Query("SELECT f.id FROM Friend f WHERE f.user = :user AND f.status = 'PENDING'")
    List<Long> findPendingRequestIds(@Param("user") User user);

    @Query("SELECT f.id FROM Friend f WHERE f.friend = :user AND f.status = 'PENDING'")
    List<Long> findReceivedRequestIds(@Param("user") User user);
}