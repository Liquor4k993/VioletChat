package com.violetchat.service;

import com.violetchat.dto.request.CommentRequest;
import com.violetchat.dto.request.PostRequest;
import com.violetchat.dto.response.CommentResponse;
import com.violetchat.dto.response.PostResponse;
import com.violetchat.entity.Comment;
import com.violetchat.entity.Like;
import com.violetchat.entity.Post;
import com.violetchat.entity.User;
import com.violetchat.repository.CommentRepository;
import com.violetchat.repository.LikeRepository;
import com.violetchat.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final UserService userService;

    @Value("${upload.path:uploads}")
    private String uploadPath;

    // ===== CREATE POST =====

    @Transactional
    public PostResponse createPost(Long userId, PostRequest request) throws IOException {
        User author = userService.findById(userId);

        Post post = Post.builder()
                .author(author)
                .content(request.getContent())
                .build();

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = saveImage(request.getImage());
            post.setImageUrl(imageUrl);
        }

        Post saved = postRepository.save(post);
        log.info("📝 Post created by user: {}", author.getUsername());

        return toResponse(saved, userId);
    }

    // ===== GET ALL POSTS =====

    @Transactional(readOnly = true)
    public Page<PostResponse> getAllPosts(int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> posts = postRepository.findAll(pageable);
        return posts.map(post -> toResponse(post, currentUserId));
    }

    // ===== GET POST BY ID =====

    @Transactional(readOnly = true)
    public PostResponse getPostById(Long postId, Long currentUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return toResponse(post, currentUserId);
    }

    // ===== DELETE POST =====

    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getAuthor().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot delete this post");
        }

        // Удаляем связанные лайки и комментарии
        likeRepository.deleteByPost(post);
        commentRepository.deleteByPost(post);

        postRepository.delete(post);
        log.info("🗑️ Post {} deleted", postId);
    }

    // ===== ADD COMMENT =====

    @Transactional
    public CommentResponse addComment(Long postId, Long userId, CommentRequest request) throws IOException {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User author = userService.findById(userId);

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .content(request.getContent())
                .build();

        if (request.getImage() != null && !request.getImage().isEmpty()) {
            String imageUrl = saveImage(request.getImage());
            comment.setImageUrl(imageUrl);
        }

        Comment saved = commentRepository.save(comment);
        log.info("💬 Comment added to post {} by user {}", postId, author.getUsername());

        return toCommentResponse(saved);
    }

    // ===== GET COMMENTS =====

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(Long postId) {
        Pageable pageable = PageRequest.of(0, 50, Sort.by("createdAt").ascending());
        Page<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId, pageable);
        return comments.getContent().stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());
    }

    // ===== DELETE COMMENT =====

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new IllegalArgumentException("You cannot delete this comment");
        }

        commentRepository.delete(comment);
        log.info("🗑️ Comment {} deleted", commentId);
    }

    // ===== TOGGLE LIKE =====

    @Transactional
    public void toggleLike(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User user = userService.findById(userId);

        Optional<Like> existingLike = likeRepository.findByPostAndUser(post, user);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
            log.info("👎 Like removed from post {}", postId);
        } else {
            Like like = Like.builder()
                    .post(post)
                    .user(user)
                    .build();
            likeRepository.save(like);
            log.info("👍 Like added to post {}", postId);
        }
    }

    // ===== CHECK IF LIKED =====

    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long postId, Long userId) {
        if (userId == null) return false;
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        User user = userService.findById(userId);
        return likeRepository.existsByPostAndUser(post, user);
    }

    // ===== GET LIKES COUNT =====

    @Transactional(readOnly = true)
    public long getLikesCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        return likeRepository.countByPost(post);
    }

    // ===== GET POSTS BY USER =====

    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByUser(Long userId, int page, int size, Long currentUserId) {
        User user = userService.findById(userId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Post> posts = postRepository.findByAuthorOrderByCreatedAtDesc(user, pageable);
        return posts.map(post -> toResponse(post, currentUserId));
    }

    // ===== GET RECENT POSTS =====

    @Transactional(readOnly = true)
    public List<PostResponse> getRecentPosts(int limit, Long currentUserId) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        Page<Post> posts = postRepository.findAll(pageable);
        return posts.getContent().stream()
                .map(post -> toResponse(post, currentUserId))
                .collect(Collectors.toList());
    }

    // ===== PRIVATE METHODS =====

    private String saveImage(MultipartFile file) throws IOException {
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "post_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        Path filePath = uploadDir.resolve(filename);
        Files.write(filePath, file.getBytes());

        return "/uploads/" + filename;
    }

    private PostResponse toResponse(Post post, Long currentUserId) {
        boolean liked = false;
        if (currentUserId != null) {
            liked = likeRepository.existsByPostAndUser(post, userService.findById(currentUserId));
        }

        List<CommentResponse> commentResponses = post.getComments().stream()
                .map(this::toCommentResponse)
                .collect(Collectors.toList());

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .author(userService.toResponse(post.getAuthor()))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .likesCount(post.getLikesCount())
                .likedByCurrentUser(liked)
                .comments(commentResponses)
                .build();
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .imageUrl(comment.getImageUrl())
                .author(userService.toResponse(comment.getAuthor()))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}