package com.violetchat.repository;

import com.violetchat.entity.Comment;
import com.violetchat.entity.Post;
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

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ===== BASIC QUERIES =====

    Page<Comment> findByPostIdOrderByCreatedAtAsc(Long postId, Pageable pageable);

    Page<Comment> findByPostIdOrderByCreatedAtDesc(Long postId, Pageable pageable);

    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);

    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);

    List<Comment> findByPost(Post post);

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.id = :commentId")
    Comment findByIdWithAuthor(@Param("commentId") Long commentId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.post.id = :postId ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdWithAuthor(@Param("postId") Long postId);

    // ===== COUNT =====

    long countByPost(Post post);

    long countByPostId(Long postId);

    long countByAuthorId(Long authorId);

    // УДАЛЯЕМ ПРОБЛЕМНЫЙ МЕТОД countTodayComments() - он не нужен для базовой функциональности

    // ===== DELETE =====

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.post = :post")
    void deleteByPost(@Param("post") Post post);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.post.id = :postId")
    void deleteByPostId(@Param("postId") Long postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.author.id = :authorId")
    void deleteByAuthorId(@Param("authorId") Long authorId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.createdAt < :date")
    void deleteCommentsOlderThan(@Param("date") LocalDateTime date);

    // ===== FIND BY AUTHOR =====

    Page<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    List<Comment> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    Page<Comment> findByPostIdAndAuthorIdOrderByCreatedAtAsc(Long postId, Long authorId, Pageable pageable);

    List<Comment> findByPostIdAndAuthorIdOrderByCreatedAtAsc(Long postId, Long authorId);

    // ===== RECENT COMMENTS =====

    @Query("SELECT c FROM Comment c ORDER BY c.createdAt DESC")
    Page<Comment> findRecentComments(Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId ORDER BY c.createdAt DESC")
    Page<Comment> findRecentCommentsByPost(@Param("postId") Long postId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.author.id = :authorId ORDER BY c.createdAt DESC")
    Page<Comment> findRecentCommentsByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    // ===== DATE RANGE =====

    @Query("SELECT c FROM Comment c WHERE c.createdAt BETWEEN :start AND :end ORDER BY c.createdAt DESC")
    List<Comment> findCommentsBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.createdAt BETWEEN :start AND :end ORDER BY c.createdAt DESC")
    List<Comment> findCommentsByPostBetweenDates(@Param("postId") Long postId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT c FROM Comment c WHERE c.author.id = :authorId AND c.createdAt BETWEEN :start AND :end ORDER BY c.createdAt DESC")
    List<Comment> findCommentsByAuthorBetweenDates(@Param("authorId") Long authorId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ===== WITH IMAGE =====

    @Query("SELECT c FROM Comment c WHERE c.imageUrl IS NOT NULL AND c.imageUrl != '' ORDER BY c.createdAt DESC")
    Page<Comment> findCommentsWithImages(Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND c.imageUrl IS NOT NULL AND c.imageUrl != '' ORDER BY c.createdAt DESC")
    Page<Comment> findCommentsWithImagesByPost(@Param("postId") Long postId, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.author.id = :authorId AND c.imageUrl IS NOT NULL AND c.imageUrl != '' ORDER BY c.createdAt DESC")
    Page<Comment> findCommentsWithImagesByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    // ===== SEARCH =====

    @Query("SELECT c FROM Comment c WHERE LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.createdAt DESC")
    Page<Comment> searchComments(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.post.id = :postId AND LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.createdAt DESC")
    Page<Comment> searchCommentsByPost(@Param("postId") Long postId, @Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.author.id = :authorId AND LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY c.createdAt DESC")
    Page<Comment> searchCommentsByAuthor(@Param("authorId") Long authorId, @Param("keyword") String keyword, Pageable pageable);

    // ===== STATISTICS =====

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.createdAt > :date")
    long countCommentsSince(@Param("date") LocalDateTime date);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.post.id = :postId AND c.createdAt > :date")
    long countCommentsByPostSince(@Param("postId") Long postId, @Param("date") LocalDateTime date);

    @Query("SELECT COUNT(c) FROM Comment c WHERE c.author.id = :authorId AND c.createdAt > :date")
    long countCommentsByAuthorSince(@Param("authorId") Long authorId, @Param("date") LocalDateTime date);

    // ===== BULK OPERATIONS =====

    @Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.content = :content, c.updatedAt = :time WHERE c.id = :commentId")
    void updateCommentContent(@Param("commentId") Long commentId, @Param("content") String content, @Param("time") LocalDateTime time);

    @Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.imageUrl = :imageUrl, c.updatedAt = :time WHERE c.id = :commentId")
    void updateCommentImage(@Param("commentId") Long commentId, @Param("imageUrl") String imageUrl, @Param("time") LocalDateTime time);

    @Modifying
    @Transactional
    @Query("UPDATE Comment c SET c.updatedAt = :time WHERE c.id = :commentId")
    void updateCommentUpdatedAt(@Param("commentId") Long commentId, @Param("time") LocalDateTime time);

    // ===== EXISTS =====

    boolean existsByIdAndPostId(Long commentId, Long postId);

    boolean existsByIdAndAuthorId(Long commentId, Long authorId);

    boolean existsByPostId(Long postId);

    boolean existsByAuthorId(Long authorId);

    // ===== IDS =====

    @Query("SELECT c.id FROM Comment c WHERE c.post.id = :postId")
    List<Long> findIdsByPostId(@Param("postId") Long postId);

    @Query("SELECT c.id FROM Comment c WHERE c.author.id = :authorId")
    List<Long> findIdsByAuthorId(@Param("authorId") Long authorId);

    @Query("SELECT c.id FROM Comment c WHERE c.createdAt BETWEEN :start AND :end")
    List<Long> findIdsBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // ===== TOP COMMENTS =====

    @Query("SELECT c.post.id, COUNT(c) as cnt FROM Comment c GROUP BY c.post.id ORDER BY cnt DESC")
    List<Object[]> findTopCommentedPosts(Pageable pageable);

    @Query("SELECT c.post.id, COUNT(c) as cnt FROM Comment c WHERE c.createdAt > :date GROUP BY c.post.id ORDER BY cnt DESC")
    List<Object[]> findTopCommentedPostsSince(@Param("date") LocalDateTime date, Pageable pageable);

    @Query("SELECT c.author.id, COUNT(c) as cnt FROM Comment c GROUP BY c.author.id ORDER BY cnt DESC")
    List<Object[]> findTopCommenters(Pageable pageable);

    @Query("SELECT c.author.id, COUNT(c) as cnt FROM Comment c WHERE c.createdAt > :date GROUP BY c.author.id ORDER BY cnt DESC")
    List<Object[]> findTopCommentersSince(@Param("date") LocalDateTime date, Pageable pageable);

    // ===== DAILY STATS =====

    @Query("SELECT DATE(c.createdAt) as date, COUNT(c) as count FROM Comment c GROUP BY DATE(c.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyCommentStats(Pageable pageable);

    @Query("SELECT DATE(c.createdAt) as date, COUNT(c) as count FROM Comment c WHERE c.post.id = :postId GROUP BY DATE(c.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyCommentStatsByPost(@Param("postId") Long postId, Pageable pageable);

    @Query("SELECT DATE(c.createdAt) as date, COUNT(c) as count FROM Comment c WHERE c.author.id = :authorId GROUP BY DATE(c.createdAt) ORDER BY date DESC")
    List<Object[]> getDailyCommentStatsByAuthor(@Param("authorId") Long authorId, Pageable pageable);

    // ===== LATEST COMMENTS FOR FEED =====

    @Query("SELECT c FROM Comment c JOIN FETCH c.author ORDER BY c.createdAt DESC")
    List<Comment> findLatestCommentsWithAuthors(Pageable pageable);

    @Query("SELECT c FROM Comment c JOIN FETCH c.author WHERE c.post.id = :postId ORDER BY c.createdAt DESC")
    List<Comment> findLatestCommentsByPostWithAuthors(@Param("postId") Long postId, Pageable pageable);

    // ===== COMPLEX QUERIES =====

    @Query("SELECT c FROM Comment c WHERE c.content LIKE %:word% ORDER BY c.createdAt DESC")
    Page<Comment> findCommentsContainingWord(@Param("word") String word, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE LENGTH(c.content) > :minLength ORDER BY c.createdAt DESC")
    Page<Comment> findCommentsWithMinLength(@Param("minLength") int minLength, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.imageUrl IS NULL OR c.imageUrl = '' ORDER BY c.createdAt DESC")
    Page<Comment> findCommentsWithoutImages(Pageable pageable);

    // ===== BATCH DELETE =====

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.id IN :commentIds")
    void deleteAllByIds(@Param("commentIds") List<Long> commentIds);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.post.id IN :postIds")
    void deleteAllByPostIds(@Param("postIds") List<Long> postIds);
}