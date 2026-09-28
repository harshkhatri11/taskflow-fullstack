package com.taskflow.backend.service;

import com.taskflow.backend.dto.request.CommentRequest;
import com.taskflow.backend.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    List<CommentResponse> getCommentsByTaskId(Long taskId);

    CommentResponse createComment(Long taskId, CommentRequest commentRequest);

    void deleteComment(Long commentId);

}
