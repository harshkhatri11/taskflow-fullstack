package com.taskflow.backend.service.impl;

import com.taskflow.backend.dto.request.ProjectRequest;
import com.taskflow.backend.dto.response.ProjectResponse;
import com.taskflow.backend.dto.response.UserSummaryResponse;
import com.taskflow.backend.entity.Project;
import com.taskflow.backend.entity.User;
import com.taskflow.backend.enums.ProjectStatus;
import com.taskflow.backend.enums.Role;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.repository.ProjectRepository;
import com.taskflow.backend.repository.TaskRepository;
import com.taskflow.backend.repository.UserRepository;
import com.taskflow.backend.security.CustomUserDetails;
import com.taskflow.backend.security.ProjectSecurity;
import com.taskflow.backend.service.ProjectService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Transactional
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectSecurity projectSecurity; // reused as plain calls in getProjectById

    @Override
    @Transactional(readOnly = true)
    // No gate — scoping problem, same shape as TaskServiceImpl.getTasksByProjectId
    public List<ProjectResponse> getProjects() {
        Role role = extractRole();
        Long userId = extractUserId();

        List<Project> projects = switch (role) {
            case ADMIN -> projectRepository.findAll();
            case MANAGER -> projectRepository.findByManagerId(userId);
            case EMPLOYEE -> projectRepository.findProjectsByAssignedEmployeeId(userId);
        };

        return projects.stream().map(this::toProjectResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    // Deliberately NOT @PreAuthorize — must stay 404 on access-denial, not 403.
    // Reuses ProjectSecurity's boolean methods directly (single source of truth),
    // just called as plain Java here instead of SpEL.
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Role role = extractRole();

        switch (role) {
            case ADMIN -> { }
            case MANAGER -> {
                if (!projectSecurity.isOwner(id, auth)) {
                    throw new ResourceNotFoundException("Project not found");
                }
            }
            case EMPLOYEE -> {
                if (!projectSecurity.hasAssignedTask(id, auth)) {
                    throw new ResourceNotFoundException("Project not found");
                }
            }
        }
        return toProjectResponse(project);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    // Coarse role gate only. Who's allowed to SET managerId is business logic,
    // not an authorization gate, so it stays in the method body below.
    public ProjectResponse createProject(ProjectRequest projectRequest) {
        Role role = extractRole();
        Long userId = extractUserId();
        Long managerId = projectRequest.managerId();

        User manager;

        if (role == Role.ADMIN) {
            if (managerId == null) {
                throw new IllegalArgumentException("Manager Id is required when creating project");
            }
            manager = resolveManager(managerId);
        } else { // MANAGER — the only other role that passes the @PreAuthorize gate
            if (managerId != null) {
                throw new IllegalArgumentException("Manager Id is not accepted for manager role");
            }
            manager = userRepository.getReferenceById(userId);
        }

        Project project = new Project();
        project.setTitle(projectRequest.title());
        project.setDescription(projectRequest.description());
        project.setStatus(ProjectStatus.valueOf(projectRequest.status()));
        project.setManager(manager);

        Project savedProject = projectRepository.save(project);
        return toProjectResponse(savedProject);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @projectSecurity.isOwner(#id, authentication))")
    // Ownership gate now lives in SpEL — the old inline "if not owner, throw" is
    // gone from the body below, since a MANAGER who fails isOwner never reaches
    // this method at all anymore.
    public ProjectResponse updateProject(Long id, ProjectRequest projectRequest) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found"));

        Role role = extractRole();

        if (role == Role.ADMIN) {
            Long managerId = projectRequest.managerId();
            if (managerId != null) {
                project.setManager(resolveManager(managerId));
            }
        } else { // MANAGER — already proven to own this project by @PreAuthorize
            if (projectRequest.managerId() != null) {
                throw new IllegalArgumentException("Manager Id is not accepted for manager role");
            }
        }

        project.setTitle(projectRequest.title());
        project.setDescription(projectRequest.description());
        project.setStatus(ProjectStatus.valueOf(projectRequest.status()));

        Project updatedProject = projectRepository.save(project);
        return toProjectResponse(updatedProject);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @projectSecurity.isOwner(#id, authentication))")
    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Project not found");
        }
        projectRepository.deleteById(id);
    }

    // ---- private helpers ----

    private ProjectResponse toProjectResponse(Project project) {
        User manager = project.getManager();
        UserSummaryResponse managerSummary = new UserSummaryResponse(
                manager.getId(),
                manager.getName(),
                manager.getRole()
        );

        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getStatus(),
                managerSummary,
                project.getCreatedAt()
        );
    }

    private Long extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }

    private Role extractRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        return principal.getRole();
    }

    private User resolveManager(Long managerId) {
        User candidate = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Manager not found"));
        if (candidate.getRole() != Role.MANAGER) {
            throw new IllegalArgumentException("User " + managerId + " is not a MANAGER");
        }
        return candidate;
    }
}