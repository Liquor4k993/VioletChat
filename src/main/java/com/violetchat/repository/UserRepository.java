package com.violetchat.repository;

import com.violetchat.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ===== BASIC QUERIES =====

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // ===== SEARCH =====

    List<User> findByUsernameContainingIgnoreCaseOrFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String username, String firstName, String lastName, String email
    );

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<User> searchUsers(@Param("query") String query);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) AND u.id != :excludeId")
    List<User> searchUsersExcluding(@Param("query") String query, @Param("excludeId") Long excludeId);

    // ===== RECOMMENDATIONS =====

    @Query("SELECT u FROM User u WHERE u.id NOT IN :excludeIds AND u.active = true ORDER BY u.createdAt DESC")
    List<User> findRecommendedUsers(@Param("excludeIds") List<Long> excludeIds, Pageable pageable);

    @Query(value = "SELECT * FROM users u WHERE u.id NOT IN :excludeIds AND u.active = true ORDER BY u.created_at DESC LIMIT :limit",
            nativeQuery = true)
    List<User> findRecommendedUsersNative(@Param("excludeIds") List<Long> excludeIds, @Param("limit") int limit);

    @Query("SELECT u FROM User u WHERE u.id NOT IN :excludeIds AND u.active = true ORDER BY u.lastActive DESC")
    List<User> findActiveUsersExcluding(@Param("excludeIds") List<Long> excludeIds, Pageable pageable);

    // ===== ONLINE STATUS =====

    @Query("SELECT u FROM User u WHERE u.lastActive > :time AND u.active = true")
    List<User> findByLastActiveAfter(@Param("time") LocalDateTime time);

    @Query("SELECT COUNT(u) FROM User u WHERE u.lastActive > :time AND u.active = true")
    long countOnlineUsers(@Param("time") LocalDateTime time);

    // ===== STATISTICS =====

    @Query("SELECT COUNT(u) FROM User u WHERE u.active = true")
    long countActiveUsers();

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt > :date")
    long countNewUsersSince(@Param("date") LocalDateTime date);

    // ===== BULK OPERATIONS =====

    @Query("SELECT u FROM User u WHERE u.id IN :userIds AND u.active = true")
    List<User> findActiveUsersByIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT u FROM User u WHERE u.active = true ORDER BY u.lastActive DESC")
    List<User> findRecentlyActiveUsers(Pageable pageable);

    // ===== ADMIN QUERIES =====

    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN'")
    List<User> findAdmins();

    @Query("SELECT u FROM User u WHERE u.role = 'USER' AND u.active = true")
    List<User> findActiveRegularUsers();

    // ===== AUTH =====

    @Query("SELECT u FROM User u WHERE u.username = :username AND u.active = true")
    Optional<User> findActiveByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.active = true")
    Optional<User> findActiveByEmail(@Param("email") String email);

    // ===== CHECK EXISTENCE =====

    boolean existsByUsernameAndActiveTrue(String username);

    boolean existsByEmailAndActiveTrue(String email);

    // ===== UPDATE =====

    @Query("UPDATE User u SET u.lastActive = :time WHERE u.id = :userId")
    void updateLastActive(@Param("userId") Long userId, @Param("time") LocalDateTime time);

    @Query("UPDATE User u SET u.active = :active WHERE u.id = :userId")
    void updateActiveStatus(@Param("userId") Long userId, @Param("active") boolean active);
}