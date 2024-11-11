package com.edu.webapp.service;

import com.edu.webapp.model.enums.ActiveStatus;
import com.edu.webapp.model.request.*;
import com.edu.webapp.model.response.CommentRes;
import com.edu.webapp.model.response.PostRes;
import com.edu.webapp.model.response.PostUserRes;
import org.springframework.data.domain.Page;

public interface PostService {
    void createPost(PostCreateReq postCreateReq);

    Page<PostRes> search(FilterPostReq filterPostReq);

    PostRes getPostById(String id);

    CommentRes createComment(CommentReq commentReq);

    Page<PostUserRes> searchPostUser(Integer page, Integer size, String key, ActiveStatus status);

    Page<CommentRes> getListCommentPost(CommentPostSearchReq commentPostSearchReq);

    PostRes updatePost(PostUpdateReq postUpdateReq);
}
