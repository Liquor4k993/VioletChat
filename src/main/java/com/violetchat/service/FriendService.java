package com.violetchat.service;

import com.violetchat.dto.response.FriendResponse;
import com.violetchat.dto.response.UserResponse;
import com.violetchat.entity.Friend;
import com.violetchat.entity.User;
import com.violetchat.repository.FriendRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserService userService;
    private final BlacklistService blacklistService;

    // ===== SEND FRIEND REQUEST =====

    @Transactional
    public FriendResponse sendFriendRequest(Long userId, String friendUsername) {
        User user = userService.findById(userId);
        User friend = userService.findByUsername(friendUsername);

        if (user.getId().equals(friend.getId())) {
            throw new IllegalArgumentException("Нельзя добавить себя в друзья");
        }

        // Проверяем, не заблокирован ли пользователь
        if (blacklistService.isBlocked(friend.getId(), user.getId())) {
            throw new IllegalArgumentException("Вы не можете отправить заявку этому пользователю");
        }

        if (blacklistService.isBlocked(user.getId(), friend.getId())) {
            throw new IllegalArgumentException("Пользователь заблокирован вами");
        }

        // Проверяем существующую дружбу в обе стороны
        if (friendRepository.findByUserAndFriend(user, friend).isPresent() ||
                friendRepository.findByUserAndFriend(friend, user).isPresent()) {
            throw new IllegalArgumentException("Запрос уже отправлен или вы уже друзья");
        }

        Friend friendEntity = Friend.builder()
                .user(user)
                .friend(friend)
                .status(Friend.FriendStatus.PENDING)
                .build();

        Friend saved = friendRepository.save(friendEntity);
        log.info("👫 Запрос в друзья: {} -> {}", user.getUsername(), friend.getUsername());
        return toResponse(saved);
    }

    // ===== ACCEPT FRIEND REQUEST =====

    @Transactional
    public FriendResponse acceptFriendRequest(Long userId, Long requestId) {
        Friend friend = friendRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Запрос не найден"));

        // Проверяем, что пользователь является получателем запроса
        if (!friend.getFriend().getId().equals(userId)) {
            throw new IllegalArgumentException("Вы не можете принять этот запрос");
        }

        // Проверяем, что запрос ещё в статусе PENDING
        if (friend.getStatus() != Friend.FriendStatus.PENDING) {
            throw new IllegalArgumentException("Этот запрос уже обработан");
        }

        friend.setStatus(Friend.FriendStatus.ACCEPTED);
        friend.setUpdatedAt(LocalDateTime.now());

        Friend saved = friendRepository.save(friend);
        log.info("✅ Запрос в друзья принят: {} -> {}",
                saved.getUser().getUsername(),
                saved.getFriend().getUsername());
        return toResponse(saved);
    }

    // ===== REJECT FRIEND REQUEST =====

    @Transactional
    public void rejectFriendRequest(Long userId, Long requestId) {
        Friend friend = friendRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Запрос не найден"));

        if (!friend.getFriend().getId().equals(userId)) {
            throw new IllegalArgumentException("Вы не можете отклонить этот запрос");
        }

        if (friend.getStatus() != Friend.FriendStatus.PENDING) {
            throw new IllegalArgumentException("Этот запрос уже обработан");
        }

        friend.setStatus(Friend.FriendStatus.REJECTED);
        friend.setUpdatedAt(LocalDateTime.now());
        friendRepository.save(friend);
        log.info("❌ Запрос в друзья отклонён: {} -> {}",
                friend.getUser().getUsername(),
                friend.getFriend().getUsername());
    }

    // ===== REMOVE FRIEND =====

    @Transactional
    public void removeFriend(Long userId, Long friendId) {
        User user = userService.findById(userId);
        User friend = userService.findById(friendId);

        // Проверяем, что пользователи действительно друзья
        if (!areFriends(userId, friendId)) {
            throw new IllegalArgumentException("Вы не являетесь друзьями");
        }

        // Удаляем дружбу в обе стороны
        friendRepository.findByUserAndFriend(user, friend)
                .ifPresent(friendRepository::delete);
        friendRepository.findByUserAndFriend(friend, user)
                .ifPresent(friendRepository::delete);

        log.info("👋 Users {} and {} are no longer friends", userId, friendId);
    }

    // ===== GET FRIENDS =====

    @Transactional(readOnly = true)
    public List<UserResponse> getFriends(Long userId) {
        User user = userService.findById(userId);
        List<Friend> friendships = friendRepository.findAcceptedFriends(user);

        List<UserResponse> friends = new ArrayList<>();
        for (Friend f : friendships) {
            if (f.getUser().getId().equals(userId)) {
                friends.add(userService.toResponse(f.getFriend()));
            } else {
                friends.add(userService.toResponse(f.getUser()));
            }
        }
        return friends;
    }

    @Transactional(readOnly = true)
    public List<User> getFriendEntities(Long userId) {
        User user = userService.findById(userId);
        List<Friend> friendships = friendRepository.findAcceptedFriends(user);

        List<User> friends = new ArrayList<>();
        for (Friend f : friendships) {
            if (f.getUser().getId().equals(userId)) {
                friends.add(f.getFriend());
            } else {
                friends.add(f.getUser());
            }
        }
        return friends;
    }

    // ===== GET PENDING REQUESTS =====

    @Transactional(readOnly = true)
    public List<FriendResponse> getPendingRequests(Long userId) {
        User user = userService.findById(userId);
        return friendRepository.findPendingRequests(user).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getSentRequests(Long userId) {
        User user = userService.findById(userId);
        return friendRepository.findByUserAndStatus(user, Friend.FriendStatus.PENDING).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ===== CHECK FRIENDSHIP =====

    @Transactional(readOnly = true)
    public boolean areFriends(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) return false;
        User user1 = userService.findById(userId1);
        User user2 = userService.findById(userId2);

        return friendRepository.existsByUserAndFriendAndStatus(user1, user2, Friend.FriendStatus.ACCEPTED) ||
                friendRepository.existsByUserAndFriendAndStatus(user2, user1, Friend.FriendStatus.ACCEPTED);
    }

    @Transactional(readOnly = true)
    public Friend.FriendStatus getFriendshipStatus(Long userId1, Long userId2) {
        if (userId1.equals(userId2)) return null;

        User user1 = userService.findById(userId1);
        User user2 = userService.findById(userId2);

        // Проверяем статус в обе стороны
        var friend1 = friendRepository.findByUserAndFriend(user1, user2);
        if (friend1.isPresent()) {
            return friend1.get().getStatus();
        }

        var friend2 = friendRepository.findByUserAndFriend(user2, user1);
        if (friend2.isPresent()) {
            return friend2.get().getStatus();
        }

        return null;
    }

    @Transactional(readOnly = true)
    public boolean hasPendingRequest(Long userId, String friendUsername) {
        User user = userService.findById(userId);
        User friend = userService.findByUsername(friendUsername);

        return friendRepository.existsByUserAndFriendAndStatus(user, friend, Friend.FriendStatus.PENDING) ||
                friendRepository.existsByUserAndFriendAndStatus(friend, user, Friend.FriendStatus.PENDING);
    }

    // ===== STATISTICS =====

    @Transactional(readOnly = true)
    public long countFriends(Long userId) {
        User user = userService.findById(userId);
        return friendRepository.countAcceptedFriends(user);
    }

    @Transactional(readOnly = true)
    public long countPendingRequests(Long userId) {
        User user = userService.findById(userId);
        return friendRepository.countPendingRequests(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getMutualFriends(Long userId1, Long userId2) {
        List<User> friends1 = getFriendEntities(userId1);
        List<User> friends2 = getFriendEntities(userId2);

        List<User> mutual = friends1.stream()
                .filter(friends2::contains)
                .collect(Collectors.toList());

        return mutual.stream()
                .map(userService::toResponse)
                .collect(Collectors.toList());
    }

    // ===== BULK OPERATIONS =====

    @Transactional
    public void acceptAllPendingRequests(Long userId) {
        User user = userService.findById(userId);
        List<Friend> pending = friendRepository.findPendingRequests(user);

        for (Friend friend : pending) {
            if (friend.getStatus() == Friend.FriendStatus.PENDING) {
                friend.setStatus(Friend.FriendStatus.ACCEPTED);
                friend.setUpdatedAt(LocalDateTime.now());
            }
        }

        friendRepository.saveAll(pending);
        log.info("✅ Accepted {} pending requests for user {}", pending.size(), userId);
    }

    @Transactional
    public void rejectAllPendingRequests(Long userId) {
        User user = userService.findById(userId);
        List<Friend> pending = friendRepository.findPendingRequests(user);

        for (Friend friend : pending) {
            if (friend.getStatus() == Friend.FriendStatus.PENDING) {
                friend.setStatus(Friend.FriendStatus.REJECTED);
                friend.setUpdatedAt(LocalDateTime.now());
            }
        }

        friendRepository.saveAll(pending);
        log.info("❌ Rejected {} pending requests for user {}", pending.size(), userId);
    }

    // ===== PRIVATE METHODS =====

    private FriendResponse toResponse(Friend friend) {
        return FriendResponse.builder()
                .id(friend.getId())
                .user(userService.toResponse(friend.getUser()))
                .friend(userService.toResponse(friend.getFriend()))
                .status(friend.getStatus().name())
                .createdAt(friend.getCreatedAt().toString())
                .build();
    }
}