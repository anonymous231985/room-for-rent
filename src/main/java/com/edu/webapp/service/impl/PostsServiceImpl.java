package com.edu.webapp.service.impl;

import com.edu.webapp.entity.post.Comment;
import com.edu.webapp.entity.post.Image;
import com.edu.webapp.entity.post.LikePost;
import com.edu.webapp.entity.post.Post;
import com.edu.webapp.entity.user.User;
import com.edu.webapp.error.ErrorCodes;
import com.edu.webapp.error.ValidateException;
import com.edu.webapp.mapper.CommentMapper;
import com.edu.webapp.mapper.PostMapper;
import com.edu.webapp.model.enums.ActiveStatus;
import com.edu.webapp.model.page.CustomPage;
import com.edu.webapp.model.request.*;
import com.edu.webapp.model.response.CommentRes;
import com.edu.webapp.model.response.PostRes;
import com.edu.webapp.model.response.PostUserRes;
import com.edu.webapp.repository.*;
import com.edu.webapp.security.JwtCommon;
import com.edu.webapp.service.PostService;
import com.edu.webapp.utils.TimeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostsServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final ImageRepository imageRepository;
    private final CommentRepository commentRepository;
    private final JwtCommon jwtCommon;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final LikePostRepository likePostRepository;

    @Transactional
    @Override
    public void createPost(PostCreateReq postCreateReq) {
        String username = jwtCommon.extractUsername();
        Post post = postMapper.postReqToPost(postCreateReq);
        post.setActive(ActiveStatus.ACTIVE);
        post.setCreatedBy(username);
        post.setUpdatedBy(username);
        postRepository.save(post);
        for (String file : postCreateReq.getImages()) {
            Image image = new Image();
            image.setImage(file);
            image.setPost(post);
            imageRepository.save(image);
        }
    }

    @Override
    public Page<PostRes> search(FilterPostReq filterPostReq) {
        Pageable pageable = PageRequest.of(filterPostReq.getPage(), filterPostReq.getSize());
        Page<Post> posts = postRepository.findAll(pageable);
        List<PostRes> postRes = postMapper.postsToPosts(posts.getContent());
        Map<String, Integer> mapCount = new HashMap<>();
        Set<String> emails = postRes.stream()
                .map(PostRes::getCreatedBy)
                .collect(Collectors.toSet());
        Map<String, User> userMap = userRepository.findAllByEmailIn(emails)
                .stream()
                .collect(Collectors.toMap(User::getEmail, user -> user));

        emails.forEach(email -> mapCount.put(email, postRepository.countByCreatedBy(email)));
        postRes.forEach(post -> {
            PostRes.UserPostRes userPostRes = new PostRes.UserPostRes();
            User user = userMap.get(post.getCreatedBy());
            if (user == null) {
                throw new ValidateException(ErrorCodes.USER_NOT_EXIST);
            }
            userPostRes.setTotalPost(mapCount.getOrDefault(user.getEmail(), 0));
            userPostRes.setId(user.getId());
            userPostRes.setAvatar(user.getAvatar());
            userPostRes.setFullName(user.getFullName());
            userPostRes.setPhone(user.getPhone());
            userPostRes.setEmail(user.getEmail());
            userPostRes.setUptime(TimeUtils.formatTimeDifference(user.getUptime(), OffsetDateTime.now()));
            userPostRes.setDateOfJoin(TimeUtils.formatTimeDifference(user.getCreatedAt(), OffsetDateTime.now()));
            post.setUserPostRes(userPostRes);
            post.setUptime(TimeUtils.formatTimeDifference(post.getUpdatedAt(), OffsetDateTime.now()));
            post.setDateOfJoin(TimeUtils.formatTimeDifference(post.getCreatedAt(), OffsetDateTime.now()));
        });
        return new PageImpl<>(postRes, pageable, posts.getTotalElements());
    }

    @Override
    public PostRes getPostById(String id) {
        Post post = postRepository.findById(id).orElseThrow(() -> new ValidateException(ErrorCodes.POST_NOT_EXIST));
        post.setView(post.getView() + 1);
        postRepository.save(post);
        User user = userRepository.findByEmail(post.getCreatedBy()).orElseThrow(() -> new ValidateException(ErrorCodes.USER_NOT_EXIST));
        PostRes.UserPostRes userPostRes = buildUserPostRes(user);
        PostRes postRes = postMapper.postToPostRes(post);
        postRes.setUserPostRes(userPostRes);
        postRes.setUptime(TimeUtils.formatTimeDifference(postRes.getUpdatedAt(), OffsetDateTime.now()));
        postRes.setDateOfJoin(TimeUtils.formatTimeDifference(postRes.getCreatedAt(), OffsetDateTime.now()));
        return postRes;
    }

    @Override
    public CommentRes createComment(CommentReq commentReq) {
        String email = jwtCommon.extractUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ValidateException(ErrorCodes.USER_NOT_EXIST));
        Comment comment = commentMapper.comentRequestToComment(commentReq);
        comment.setUserId(user.getId());
        commentRepository.save(comment);
        CommentRes commentRes = commentMapper.commentToCommentRes(comment);
        if (!postRepository.existsById(commentReq.getPostId())) throw new ValidateException(ErrorCodes.POST_NOT_EXIST);
        commentRes.setAvatar(user.getAvatar());
        commentRes.setFullName(user.getFullName());
        commentRes.setUserId(user.getId());
        commentRes.setEmail(email);
        simpMessagingTemplate.convertAndSend("/topic/comments/" + comment.getPostId(), commentRes);
        return commentRes;
    }

    @Override
    public Page<PostUserRes> searchPostUser(Integer page, Integer size, String key, ActiveStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("updatedAt").descending());
        String username = jwtCommon.extractUsername();
        Page<Post> posts;
        if (status == null) {
            posts = postRepository.findByCreatedByAndContentContaining(username, key, pageable);
        } else {
            posts = postRepository.findByCreatedByAndContentContainingAndActive(username, key, status, pageable);
        }

        List<PostUserRes> postUserResList = postMapper.postsToPostsUsers(posts.getContent());
        return new PageImpl<>(postUserResList, pageable, posts.getTotalElements());
    }

    @Override
    public Page<CommentRes> getListCommentPost(CommentPostSearchReq commentPostSearchReq) {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        Page<Comment> page;
        if (commentPostSearchReq.getCommentTime() == null)
            page = commentRepository.findByPostId(commentPostSearchReq.getPostId(), pageable);
        else
            page = commentRepository.findByPostIdAndCreatedAtBefore(commentPostSearchReq.getPostId(), commentPostSearchReq.getCommentTime(), pageable);
        List<Comment> comments = page.getContent();
        Set<String> ids = comments.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        Map<String, User> userMap = userRepository.findAllByIdIn(ids)
                .stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        List<CommentRes> commentResList = new ArrayList<>();
        for (Comment comment : comments) {
            CommentRes commentRes = commentMapper.commentToCommentRes(comment);
            User user = userMap.get(comment.getUserId());
            commentRes.setAvatar(user.getAvatar());
            commentRes.setFullName(user.getFullName());
            commentRes.setUserId(user.getId());
            commentRes.setEmail(user.getEmail());
            commentResList.add(commentRes);
        }
        return new CustomPage<>(commentResList, pageable, 0L, page.getTotalElements() > 10);
    }

    @Override
    public PostRes updatePost(PostUpdateReq postUpdateReq) {
        String email = jwtCommon.extractUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ValidateException(ErrorCodes.USER_NOT_EXIST));
        Post post = postRepository.findById(postUpdateReq.getPostId()).orElseThrow(() -> new ValidateException(ErrorCodes.POST_NOT_EXIST));
        if (!post.getCreatedBy().equals(user.getEmail()))
            throw new ValidateException(ErrorCodes.YOU_NOT_PERMISSION_UPDATE);
        post.setTitle(postUpdateReq.getTitle());
        post.setContent(postUpdateReq.getContent());
        post.setPrice(postUpdateReq.getPrice());
        post.setDeposit(postUpdateReq.getDeposit());
        post.setAddress(postUpdateReq.getAddress());
        post.setAcreage(postUpdateReq.getAcreage());
        post.setStatusRoom(postUpdateReq.getStatusRoom());
        post.setContact(postUpdateReq.getContact());
        post.setImages(post.getImages());
        post.setProvince(postUpdateReq.getProvince());
        post.setDistrict(postUpdateReq.getDistrict());
        post.setLongitude(postUpdateReq.getLongitude());
        post.setLatitude(postUpdateReq.getLatitude());
        post.setActive(postUpdateReq.getActive());
        post.setType(postUpdateReq.getType());
        post.setUpdatedBy(email);
        post.setUpdatedAt(OffsetDateTime.now());
        postRepository.save(post);
        PostRes postRes = postMapper.postToPostRes(post);
        PostRes.UserPostRes userPostRes = buildUserPostRes(user);
        postRes.setUserPostRes(userPostRes);
        postRes.setUptime(TimeUtils.formatTimeDifference(postRes.getUpdatedAt(), OffsetDateTime.now()));
        postRes.setDateOfJoin(TimeUtils.formatTimeDifference(postRes.getCreatedAt(), OffsetDateTime.now()));
        return postRes;
    }

    @Override
    public void likePost(String id) {
        String email = jwtCommon.extractUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new ValidateException(ErrorCodes.USER_NOT_EXIST));
        Post post = postRepository.findById(id).orElseThrow(() -> new ValidateException(ErrorCodes.POST_NOT_EXIST));
        LikePost likePost = new LikePost();
        likePost.setUserId(user.getId());
        likePost.setPostId(post.getId());
        likePostRepository.save(likePost);
    }

    private PostRes.UserPostRes buildUserPostRes(User user) {
        PostRes.UserPostRes userPostRes = new PostRes.UserPostRes();
        userPostRes.setTotalPost(postRepository.countByCreatedBy(user.getEmail()));
        userPostRes.setId(user.getId());
        userPostRes.setAvatar(user.getAvatar());
        userPostRes.setFullName(user.getFullName());
        userPostRes.setPhone(user.getPhone());
        userPostRes.setEmail(user.getEmail());
        userPostRes.setUptime(TimeUtils.formatTimeDifference(user.getUptime(), OffsetDateTime.now()));
        userPostRes.setDateOfJoin(TimeUtils.formatTimeDifference(user.getCreatedAt(), OffsetDateTime.now()));
        return userPostRes;
    }
}
