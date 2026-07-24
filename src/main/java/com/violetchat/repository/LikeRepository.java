package com.violetchat.repository;

import com.violetchat.entity.Like;
import com.violetchat.entity.Post;
import com.violetchat.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    // ===== BASIC QUERIES =====

    Optional<Like> findByPostAndUser(Post post, User user);

    Optional<Like> findByPostIdAndUserId(Long postId, Long userId);

    // ===== COUNT =====

    long countByPost(Post post);

    long countByPostId(Long postId);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.post.id = :postId")
    long countLikesByPostId(@Param("postId") Long postId);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.user.id = :userId")
    long countLikesByUserId(@Param("userId") Long userId);

    // ===== EXISTS =====

    boolean existsByPostAndUser(Post post, User user);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT COUNT(l) > 0 FROM Like l WHERE l.post.id = :postId AND l.user.id = :userId")
    boolean isPostLikedByUser(@Param("postId") Long postId, @Param("userId") Long userId);

    // ===== DELETE =====

    void deleteByPostAndUser(Post post, User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM Like l WHERE l.post.id = :postId AND l.user.id = :userId")
    void deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Modifying
    @Transactional
    void deleteByPost(Post post);

    @Modifying
    @Transactional
    @Query("DELETE FROM Like l WHERE l.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Like l WHERE l.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    // ===== FIND BY POST =====

    List<Like> findByPost(Post post);

    List<Like> findByPostId(Long postId);

    Page<Like> findByPostId(Long postId, Pageable pageable);

    // ===== FIND BY USER =====

    List<Like> findByUser(User user);

    List<Like> findByUserId(Long userId);

    Page<Like> findByUserId(Long userId, Pageable pageable);

    // ===== FIND LIKED POSTS BY USER =====

    @Query("SELECT l.post FROM Like l WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    List<Post> findLikedPostsByUserId(@Param("userId") Long userId);

    @Query("SELECT l.post FROM Like l WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    Page<Post> findLikedPostsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT l.post.id FROM Like l WHERE l.user.id = :userId")
    List<Long> findLikedPostIdsByUserId(@Param("userId") Long userId);

    // ===== DATE RANGE =====

    @Query("SELECT l FROM Like l WHERE l.createdAt BETWEEN :start AND :end ORDER BY l.createdAt DESC")
    List<Like> findLikesBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT l FROM Like l WHERE l.post.id = :postId AND l.createdAt BETWEEN :start AND :end ORDER BY l.createdAt DESC")
    List<Like> findLikesByPostBetweenDates(@Param("postId") Long postId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT l FROM Like l WHERE l.user.id = :userId AND l.createdAt BETWEEN :start AND :end ORDER BY l.createdAt DESC")
    List<Like> findLikesByUserBetweenDates(@Param("userId") Long userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ===== STATISTICS =====

    @Query("SELECT COUNT(l) FROM Like l WHERE l.createdAt > :date")
    long countLikesSince(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.post.id = :postId AND l.createdAt > :date")
    long countLikesByPostSince(@Param("postId") Long postId, @Param("date") LocalDateTime date);

    @Query("SELECT COUNT(l) FROM Like l WHERE l.user.id = :userId AND l.createdAt > :date")
    long countLikesByUserSince(@Param("userId") Long userId, @Param("date") LocalDateTime date);

    // ===== TOP LIKED =====

    @Query("SELECT l.post.id, COUNT(l) as cnt FROM Like l GROUP BY l.post.id ORDER BY cnt DESC")
    List<Object[]> findTopLikedPosts(Pageable pageable);

    @Query("SELECT l.post.id, COUNT(l) as cnt FROM Like l WHERE l.createdAt > :date GROUP BY l.post.id ORDER BY cnt DESC")
    List<Object[]> findTopLikedPostsSince(@Param("date") LocalDateTime date, Pageable pageable);

    @Query("SELECT l.user.id, COUNT(l) as cnt FROM Like l GROUP BY l.user.id ORDER BY cnt DESC")
    List<Object[]> findTopLikers(Pageable pageable);

    // ===== BULK OPERATIONS =====

    @Modifying
    @Transactional
    @Query("DELETE FROM Like l WHERE l.post.id = :postId AND l.user.id IN :userIds")
    void deleteLikesByPostAndUsers(@Param("postId") Long postId, @Param("userIds") List<Long> userIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM Like l WHERE l.post.id IN :postIds AND l.user.id = :userId")
    void deleteLikesByPostsAndUser(@Param("postIds") List<Long> postIds, @Param("userId") Long userId);

    // ===== EXISTS FOR MULTIPLE =====

    @Query("SELECT l.post.id FROM Like l WHERE l.post.id IN :postIds AND l.user.id = :userId")
    List<Long> findLikedPostIdsInList(@Param("postIds") List<Long> postIds, @Param("userId") Long userId);

    @Query("SELECT COUNT(l) > 0 FROM Like l WHERE l.post.id IN :postIds AND l.user.id = :userId")
    boolean hasLikedAny(@Param("postIds") List<Long> postIds, @Param("userId") Long userId);

    // ===== DAILY STATS =====

    @Query("SELECT DATE(l.createdAt) as date, COUNT(l) as count FROM Like l GROUP BY DATE(l.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyLikeStats(Pageable pageable);

    @Query("SELECT DATE(l.createdAt) as date, COUNT(l) as count FROM Like l WHERE l.post.id = :postId GROUP BY DATE(l.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyLikeStatsByPost(@Param("postId") Long postId, Pageable pageable);

    @Query("SELECT DATE(l.createdAt) as date, COUNT(l) as count FROM Like l WHERE l.user.id = :userId GROUP BY DATE(l.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyLikeStatsByUser(@Param("userId") Long userId, Pageable pageable);

    // ===== BATCH DELETE =====

    @Modifying
    @Transactional
    @Query("DELETE FROM Like l WHERE l.id IN :likeIds")
    void deleteAllByIds(@Param("likeIds") List<Long> likeIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM Like l WHERE l.post.id IN :postIds")
    void deleteAllByPostIds(@Param("postIds") List<Long> postIds);
}