package com.violetchat.controller;

import com.violetchat.dto.request.CommentRequest;
import com.violetchat.dto.request.PostRequest;
import com.violetchat.dto.response.CommentResponse;
import com.violetchat.dto.response.PostResponse;
import com.violetchat.service.PostService;
import com.violetchat.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final SecurityUtils securityUtils;

    // ===== GET ALL POSTS =====

    @GetMapping
    public ResponseEntity<Page<PostResponse>> getAllPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Long currentUserId = null;
            try {
                currentUserId = securityUtils.getCurrentUserId();
            } catch (Exception e) {
                // User not authenticated
            }
            return ResponseEntity.ok(postService.getAllPosts(page, size, currentUserId));
        } catch (Exception e) {
            log.error("Error getting posts: ", e);
            throw e;
        }
    }

    // ===== GET POST BY ID =====

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long postId) {
        try {
            Long currentUserId = null;
            try {
                currentUserId = securityUtils.getCurrentUserId();
            } catch (Exception e) {
                // User not authenticated
            }
            return ResponseEntity.ok(postService.getPostById(postId, currentUserId));
        } catch (Exception e) {
            log.error("Error getting post: ", e);
            throw e;
        }
    }

    // ===== CREATE POST =====

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPost(
            @ModelAttribute PostRequest request) {
        try {
            log.info("Creating post with content: {}", request.getContent());
            PostResponse response = postService.createPost(securityUtils.getCurrentUserId(), request);
            log.info("Post created: {}", response.getId());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error creating post: ", e);
            throw new RuntimeException("Failed to create post: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error creating post: ", e);
            throw e;
        }
    }

    // ===== DELETE POST =====

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        try {
            postService.deletePost(postId, securityUtils.getCurrentUserId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting post: ", e);
            throw e;
        }
    }

    // ===== ADD COMMENT =====

    @PostMapping(value = "/{postId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long postId,
            @ModelAttribute CommentRequest request) {
        try {
            CommentResponse response = postService.addComment(
                    postId,
                    securityUtils.getCurrentUserId(),
                    request
            );
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error adding comment: ", e);
            throw new RuntimeException("Failed to add comment: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error adding comment: ", e);
            throw e;
        }
    }

    // ===== GET COMMENTS =====

    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long postId) {
        try {
            return ResponseEntity.ok(postService.getCommentsByPost(postId));
        } catch (Exception e) {
            log.error("Error getting comments: ", e);
            throw e;
        }
    }

    // ===== DELETE COMMENT =====

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        try {
            postService.deleteComment(commentId, securityUtils.getCurrentUserId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error deleting comment: ", e);
            throw e;
        }
    }

    // ===== LIKE / UNLIKE =====

    @PostMapping("/{postId}/like")
    public ResponseEntity<Void> toggleLike(@PathVariable Long postId) {
        try {
            postService.toggleLike(postId, securityUtils.getCurrentUserId());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error toggling like: ", e);
            throw e;
        }
    }

    // ===== CHECK IF LIKED =====

    @GetMapping("/{postId}/liked")
    public ResponseEntity<Boolean> isLiked(@PathVariable Long postId) {
        try {
            Long currentUserId = null;
            try {
                currentUserId = securityUtils.getCurrentUserId();
            } catch (Exception e) {
                return ResponseEntity.ok(false);
            }
            return ResponseEntity.ok(postService.isLikedByUser(postId, currentUserId));
        } catch (Exception e) {
            log.error("Error checking like: ", e);
            return ResponseEntity.ok(false);
        }
    }
}