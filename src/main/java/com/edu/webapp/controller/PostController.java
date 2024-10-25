package com.edu.webapp.controller;

import com.edu.webapp.entity.post.Post;
import com.edu.webapp.model.request.FilterPostReq;
import com.edu.webapp.model.request.PostCreateReq;
import com.edu.webapp.model.response.PostRes;
import com.edu.webapp.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/v1/posts")
@RestController
public class PostController {
    private final PostService postService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> createPost(@ModelAttribute PostCreateReq postCreateRequest) {
        postService.createPost(postCreateRequest);
        return ResponseEntity.ok("Post created successfully");
    }

    @PostMapping("/search")
    public ResponseEntity<Page<PostRes>> getPosts(@RequestBody(required = false) FilterPostReq filterPostReq) {
        if (filterPostReq == null) filterPostReq = new FilterPostReq();
        return ResponseEntity.ok(postService.search(filterPostReq));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostRes> getPost(@PathVariable String id) {
        return ResponseEntity.ok(postService.getPostById(id));
    }
}