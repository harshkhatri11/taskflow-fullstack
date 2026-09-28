package com.taskflow.backend.security;

import com.taskflow.backend.repository.ProjectRepository;
import com.taskflow.backend.repository.TaskRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("projectSecurity")
public class ProjectSecurity {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ProjectSecurity(ProjectRepository projectRepository, TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Does the calling MANAGER own this project?
     * Used two ways: as SpEL in @PreAuthorize (updateProject/deleteProject, and
     * reused by @taskSecurity.createTask), AND as a plain method call inside
     * getProjectById's switch (which must throw 404, not rely on @PreAuthorize's
     * automatic 403 — see ProjectServiceImpl for why).
     *
     * EXISTS query, not findById().map(...) — same LazyInitializationException
     * risk as TaskSecurity/CommentSecurity: project.getManager() is a LAZY
     * @ManyToOne, and findById()'s own transaction/session had already closed
     * by the time a .map() lambda would have touched it. No entity loaded here,
     * so no lazy proxy to fail on.
     */
    public boolean isOwner(Long projectId, Authentication authentication) {
        Long currentUserId = AuthUtil.extractUserId(authentication);
        return projectRepository.existsByIdAndManagerId(projectId, currentUserId);
    }

    /**
     * Does the calling EMPLOYEE have any task assigned to them within this project?
     * Used only as a plain method call inside getProjectById's switch — no
     * corresponding @PreAuthorize use case exists for this one currently.
     */
    public boolean hasAssignedTask(Long projectId, Authentication authentication) {
        Long currentUserId = AuthUtil.extractUserId(authentication);
        return taskRepository.existsByProjectIdAndAssignedToId(projectId, currentUserId);
    }
}