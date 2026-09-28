package com.taskflow.backend.security;

import com.taskflow.backend.repository.TaskRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("taskSecurity")
@AllArgsConstructor
public class TaskSecurity {

    private final TaskRepository taskRepository;


    /**
     * Used by updateTask / deleteTask.
     * Does the calling MANAGER own the PROJECT that this task belongs to?
     * Task has no managerId of its own — ownership is derived through task.project.manager.
     *
     * Deliberately an EXISTS query, not findById().map(...) — the earlier
     * fetch-then-navigate version triggered a LazyInitializationException on
     * task.getProject().getManager() once findById()'s own transaction (and the
     * Hibernate session with it) had already closed by the time the .map()
     * lambda ran. Letting the DB do the join means no entity is ever loaded here,
     * so there's no lazy proxy to fail on in the first place.
     */
    public boolean ownsTaskProject(Long taskId, Authentication authentication) {
        Long currentUserId = AuthUtil.extractUserId(authentication);
        return taskRepository.existsByIdAndProjectManagerId(taskId, currentUserId);
    }

    /**
     * Used by updateTaskStatus.
     * Is this task assigned to the calling EMPLOYEE?
     * Same EXISTS-query fix as ownsTaskProject above — assignedTo is also a
     * LAZY @ManyToOne, so the same failure mode applied here too.
     */
    public boolean isAssignedTask(Long taskId, Authentication authentication) {
        Long currentUserId = AuthUtil.extractUserId(authentication);
        return taskRepository.existsByIdAndAssignedToId(taskId, currentUserId);
    }
}