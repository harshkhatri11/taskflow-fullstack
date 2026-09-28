package com.taskflow.backend.security;

import com.taskflow.backend.repository.CommentRepository;
import com.taskflow.backend.repository.TaskRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("commentSecurity")
@AllArgsConstructor
public class CommentSecurity {

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;


    /**
     * Used by getCommentsByTaskId (EMPLOYEE branch only).
     * Broader than task-assignment: does this EMPLOYEE have ANY task assigned to
     * them within the SAME PROJECT as the given task? (Not just this exact task —
     * that narrower check is @taskSecurity.isAssignedTask, used for createComment.)
     *
     * Deliberately a single @Query, not findById() + a second lookup — the earlier
     * version loaded the full Task entity just to read task.getProject().getId()
     * (safe on its own, since the FK id doesn't need a session — but still a
     * wasted entity load), then ran a second query. This does the same check as
     * one DB round trip via a self-join, no entity loaded at all.
     */
    public boolean hasAssignedTaskInProject(Long taskId, Authentication authentication) {
        Long currentUserId = AuthUtil.extractUserId(authentication);
        return taskRepository.existsAssignedTaskInSameProjectAsTask(taskId, currentUserId);
    }


    /**
     * Used by deleteComment.
     * Is the calling user the original author of this comment?
     * Works for any role — an ADMIN or MANAGER deleting their own comment also
     * satisfies this, though ADMIN and owning-MANAGER already pass via other
     * branches in the SpEL expression regardless.
     *
     * EXISTS query, not findById().map(...) — avoids the same
     * LazyInitializationException risk fixed in TaskSecurity: comment.getAuthor()
     * is a LAZY @ManyToOne, and the old version touched it after findById()'s own
     * transaction/session had already closed.
     */
    public boolean isCommentAuthor(Long commentId, Authentication authentication) {
        Long currentUserId = AuthUtil.extractUserId(authentication);
        return commentRepository.existsByIdAndAuthorId(commentId, currentUserId);
    }

    /**
     * Used by deleteComment (MANAGER moderation-override branch).
     * Does the calling MANAGER own the project of the task this comment belongs to?
     * Same EXISTS-query fix — comment.getTask().getProject().getManager() is a
     * three-hop lazy chain, same failure mode as above, just one hop deeper.
     */
    public boolean ownsCommentTaskProject(Long commentId, Authentication authentication) {
        Long currentUserId = AuthUtil.extractUserId(authentication);
        return commentRepository.existsByIdAndTaskProjectManagerId(commentId, currentUserId);
    }
}