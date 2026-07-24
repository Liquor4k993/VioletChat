package com.violetchat.repository;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    // ===== BASIC QUERIES =====

    Page<Post> findAll(Pageable pageable);

    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Post> findAllByOrderByCreatedAtDesc();

    // ===== FIND BY AUTHOR =====

    Page<Post> findByAuthorOrderByCreatedAtDesc(User author, Pageable pageable);

    List<Post> findByAuthorOrderByCreatedAtDesc(User author);

    Page<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    // ===== FIND BY ID WITH AUTHOR =====

    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id = :postId")
    Optional<Post> findByIdWithAuthor(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.id IN :postIds")
    List<Post> findByIdsWithAuthor(@Param("postIds") List<Long> postIds);

    // ===== FIND WITH COMMENTS AND LIKES =====

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.comments LEFT JOIN FETCH p.likes WHERE p.id = :postId")
    Optional<Post> findByIdWithCommentsAndLikes(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.comments LEFT JOIN FETCH p.likes WHERE p.id IN :postIds")
    List<Post> findByIdsWithCommentsAndLikes(@Param("postIds") List<Long> postIds);

    // ===== DATE RANGE =====

    @Query("SELECT p FROM Post p WHERE p.createdAt BETWEEN :start AND :end ORDER BY p.createdAt DESC")
    List<Post> findPostsBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId AND p.createdAt BETWEEN :start AND :end ORDER BY p.createdAt DESC")
    List<Post> findPostsByAuthorBetweenDates(@Param("authorId") Long authorId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ===== WITH IMAGE =====

    @Query("SELECT p FROM Post p WHERE p.imageUrl IS NOT NULL AND p.imageUrl != '' ORDER BY p.createdAt DESC")
    Page<Post> findPostsWithImages(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.imageUrl IS NOT NULL AND p.imageUrl != '' AND p.author.id = :authorId ORDER BY p.createdAt DESC")
    Page<Post> findPostsWithImagesByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    // ===== SEARCH =====

    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY p.createdAt DESC")
    Page<Post> searchPosts(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE LOWER(p.content) LIKE LOWER(CONCAT('%', :keyword, '%')) AND p.author.id = :authorId ORDER BY p.createdAt DESC")
    Page<Post> searchPostsByAuthor(@Param("keyword") String keyword, @Param("authorId") Long authorId, Pageable pageable);

    // ===== COUNT =====

    long countByAuthor(User author);

    long countByAuthorId(Long authorId);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.createdAt > :date")
    long countPostsSince(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.author.id = :authorId AND p.createdAt > :date")
    long countPostsByAuthorSince(@Param("authorId") Long authorId, @Param("date") LocalDateTime date);

    // ===== DELETE =====

    @Modifying
    @Transactional
    @Query("DELETE FROM Post p WHERE p.author.id = :authorId")
    void deleteByAuthorId(@Param("authorId") Long authorId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Post p WHERE p.createdAt < :date")
    void deletePostsOlderThan(@Param("date") LocalDateTime date);

    // ===== UPDATE =====

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.content = :content, p.updatedAt = :time WHERE p.id = :postId")
    void updatePostContent(@Param("postId") Long postId, @Param("content") String content, @Param("time") LocalDateTime time);

    @Modifying
    @Transactional
    @Query("UPDATE Post p SET p.imageUrl = :imageUrl, p.updatedAt = :time WHERE p.id = :postId")
    void updatePostImage(@Param("postId") Long postId, @Param("imageUrl") String imageUrl, @Param("time") LocalDateTime time);

    // ===== RECENT POSTS =====

    @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
    List<Post> findRecentPosts(Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.author.id = :authorId ORDER BY p.createdAt DESC")
    List<Post> findRecentPostsByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    // ===== STATISTICS =====

    @Query("SELECT COUNT(p) FROM Post p")
    long getTotalPostsCount();

    @Query("SELECT COUNT(p) FROM Post p WHERE p.author.id = :authorId")
    long getPostsCountByAuthor(@Param("authorId") Long authorId);

    @Query("SELECT DATE(p.createdAt) as date, COUNT(p) as count FROM Post p GROUP BY DATE(p.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyPostStats(Pageable pageable);

    @Query("SELECT DATE(p.createdAt) as date, COUNT(p) as count FROM Post p WHERE p.author.id = :authorId GROUP BY DATE(p.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyPostStatsByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    // ===== TOP AUTHORS =====

    @Query("SELECT p.author.id, COUNT(p) as cnt FROM Post p GROUP BY p.author.id ORDER BY cnt DESC")
    List<Object[]> findTopAuthors(Pageable pageable);

    @Query("SELECT p.author.id, COUNT(p) as cnt FROM Post p WHERE p.createdAt > :date GROUP BY p.author.id ORDER BY cnt DESC")
    List<Object[]> findTopAuthorsSince(@Param("date") LocalDateTime date, Pageable pageable);

    // ===== BULK OPERATIONS =====

    @Query("SELECT p.id FROM Post p WHERE p.author.id = :authorId")
    List<Long> findIdsByAuthorId(@Param("authorId") Long authorId);

    @Query("SELECT p.id FROM Post p WHERE p.createdAt < :date")
    List<Long> findIdsOlderThan(@Param("date") LocalDateTime date);

    @Modifying
    @Transactional
    @Query("DELETE FROM Post p WHERE p.id IN :postIds")
    void deleteAllByIds(@Param("postIds") List<Long> postIds);

    // ===== EXISTS =====

    boolean existsByAuthorId(Long authorId);

    @Query("SELECT COUNT(p) > 0 FROM Post p WHERE p.id = :postId AND p.author.id = :authorId")
    boolean isPostByAuthor(@Param("postId") Long postId, @Param("authorId") Long authorId);

    // ===== WITH LIKES COUNT =====

    @Query("SELECT p, SIZE(p.likes) as likesCount FROM Post p ORDER BY likesCount DESC")
    Page<Object[]> findPostsOrderByLikesCount(Pageable pageable);

    @Query("SELECT p, SIZE(p.likes) as likesCount FROM Post p WHERE p.author.id = :authorId ORDER BY likesCount DESC")
    Page<Object[]> findPostsByAuthorOrderByLikesCount(@Param("authorId") Long authorId, Pageable pageable);

    // ===== WITH COMMENTS COUNT =====

    @Query("SELECT p, SIZE(p.comments) as commentsCount FROM Post p ORDER BY commentsCount DESC")
    Page<Object[]> findPostsOrderByCommentsCount(Pageable pageable);

    @Query("SELECT p, SIZE(p.comments) as commentsCount FROM Post p WHERE p.author.id = :authorId ORDER BY commentsCount DESC")
    Page<Object[]> findPostsByAuthorOrderByCommentsCount(@Param("authorId") Long authorId, Pageable pageable);

    // ===== COMPLEX QUERIES =====

    @Query("SELECT p FROM Post p WHERE SIZE(p.likes) > :minLikes ORDER BY p.createdAt DESC")
    Page<Post> findPostsWithMinLikes(@Param("minLikes") int minLikes, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE SIZE(p.comments) > :minComments ORDER BY p.createdAt DESC")
    Page<Post> findPostsWithMinComments(@Param("minComments") int minComments, Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.content LIKE %:keyword% AND SIZE(p.likes) > :minLikes ORDER BY p.createdAt DESC")
    Page<Post> searchPostsWithMinLikes(@Param("keyword") String keyword, @Param("minLikes") int minLikes, Pageable pageable);

    // ===== FEED QUERIES =====

    @Query("SELECT p FROM Post p JOIN FETCH p.author ORDER BY p.createdAt DESC")
    Page<Post> findFeedPosts(Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.author WHERE p.author.id IN :authorIds ORDER BY p.createdAt DESC")
    Page<Post> findFeedPostsByAuthors(@Param("authorIds") List<Long> authorIds, Pageable pageable);
}