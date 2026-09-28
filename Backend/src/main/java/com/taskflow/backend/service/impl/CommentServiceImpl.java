package com.taskflow.backend.service.impl;

import com.taskflow.backend.dto.request.CommentRequest;
import com.taskflow.backend.dto.response.CommentResponse;
import com.taskflow.backend.dto.response.UserSummaryResponse;
import com.taskflow.backend.entity.Comment;
import com.taskflow.backend.entity.Task;
import com.taskflow.backend.entity.User;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.repository.CommentRepository;
import com.taskflow.backend.repository.TaskRepository;
import com.taskflow.backend.repository.UserRepository;
import com.taskflow.backend.security.CustomUserDetails;
import com.taskflow.backend.service.CommentService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN') " +
            "or (hasRole('MANAGER') and @taskSecurity.ownsTaskProject(#taskId, authentication)) " +
            "or (hasRole('EMPLOYEE') and @commentSecurity.hasAssignedTaskInProject(#taskId, authentication))")
    public List<CommentResponse> getCommentsByTaskId(Long taskId) {

        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task not found or access denied");
        }
        return commentRepository.getCommentsByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') " +
            "or (hasRole('MANAGER') and @taskSecurity.ownsTaskProject(#taskId, authentication)) " +
            "or (hasRole('EMPLOYEE') and @taskSecurity.isAssignedTask(#taskId, authentication))")
    public CommentResponse createComment(Long taskId, CommentRequest commentRequest) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found or access denied"));

        User currentUser = resolveCurrentUser();
        Comment comment = new Comment();
        comment.setContent(commentRequest.content());
        comment.setTask(task);
        comment.setAuthor(currentUser);
        comment.setAuthorName(currentUser.getName());

        Comment saved = commentRepository.save(comment);
        return toResponse(saved);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') " +
            "or @commentSecurity.isCommentAuthor(#commentId, authentication) " +
            "or @commentSecurity.ownsCommentTaskProject(#commentId, authentication)")
    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new ResourceNotFoundException("Comment not found or access denied");
        }
        commentRepository.deleteById(commentId);
    }

    // ---- private helpers ----

    private User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        return userRepository.getReferenceById(principal.getId());
    }

    private CommentResponse toResponse(Comment comment) {
        Long authorId = comment.getAuthor() != null ? comment.getAuthor().getId() : null;
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                authorId,
                comment.getAuthorName(),
                comment.getCreatedAt()
        );
    }

}
