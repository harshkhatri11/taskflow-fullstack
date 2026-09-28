package com.taskflow.backend.service.impl;

import com.taskflow.backend.dto.request.TaskRequest;
import com.taskflow.backend.dto.request.TaskStatusUpdateRequest;
import com.taskflow.backend.dto.response.ProjectSummaryResponse;
import com.taskflow.backend.dto.response.TaskResponse;
import com.taskflow.backend.dto.response.UserSummaryResponse;
import com.taskflow.backend.entity.Project;
import com.taskflow.backend.entity.Task;
import com.taskflow.backend.entity.User;
import com.taskflow.backend.enums.Role;
import com.taskflow.backend.enums.TaskPriority;
import com.taskflow.backend.enums.TaskStatus;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.repository.ProjectRepository;
import com.taskflow.backend.repository.TaskRepository;
import com.taskflow.backend.repository.UserRepository;
import com.taskflow.backend.security.CustomUserDetails;
import com.taskflow.backend.service.TaskService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@AllArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream().map(this::toTaskResponse).toList();
    }

    // Gate: does this caller have ANY right to see tasks for THIS project at all?
    // ADMIN always yes. MANAGER only if they own it. EMPLOYEE only if they have a
    // task assigned within it. This reuses the same @projectSecurity methods
    // getProjectById already relies on — single source of truth, not reimplemented.
    @Override
    @PreAuthorize("hasRole('ADMIN') " +
            "or (hasRole('MANAGER') and @projectSecurity.isOwner(#projectId, authentication)) " +
            "or (hasRole('EMPLOYEE') and @projectSecurity.hasAssignedTask(#projectId, authentication))")
    public Page<TaskResponse> getTasksByProjectId(Long projectId, Pageable pageable) {
        Long currentUserId = extractUserId();
        Role role = extractRole();

        // Scoping: gate above already confirmed access to THIS project — this just
        // decides which rows WITHIN it come back per role.
        Page<Task> tasks = switch (role) {
            case ADMIN ->
                    taskRepository.getTasksByProjectId(projectId, pageable);
            case MANAGER ->
                    taskRepository.getTasksByProjectIdAndProjectManagerId(projectId, currentUserId, pageable);
            case EMPLOYEE ->
                    taskRepository.getTasksByProjectIdAndAssignedToId(projectId, currentUserId, pageable);
        };

        return tasks.map(this::toTaskResponse);
    }

    // Reuses @projectSecurity — NOT duplicated here. "Does MANAGER own this project"
    // is a Project-ownership question, already answered by ProjectSecurity.isOwner.
    @Override
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @projectSecurity.isOwner(#projectId, authentication))")
    public TaskResponse createTask(Long projectId, TaskRequest taskRequest) {

        Project project =
                projectRepository.findById(projectId).orElseThrow(() -> new ResourceNotFoundException("Project not found or access is denied"));

        User assignee = resolveAssignee(taskRequest.assignedToId());
        Task task = new Task();
        task.setTitle(taskRequest.title());
        task.setDescription(taskRequest.description());
        task.setPriority(TaskPriority.valueOf(taskRequest.priority()));
        task.setDueDate(taskRequest.dueDate());
        task.setStatus(TaskStatus.TODO);
        task.setProject(project);
        task.setAssignedTo(assignee);

        Task saved = taskRepository.save(task);
        return toTaskResponse(saved);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') " +
            "or (hasRole('MANAGER') and @taskSecurity.ownsTaskProject(#taskId, authentication)) " +
            "or (hasRole('EMPLOYEE') and @taskSecurity.isAssignedTask(#taskId, authentication))")
    public TaskResponse updateTaskStatus(Long taskId, TaskStatusUpdateRequest taskStatusUpdateRequest) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found or access denied"));

        task.setStatus(TaskStatus.valueOf(taskStatusUpdateRequest.status()));
        Task saved = taskRepository.save(task);
        return toTaskResponse(saved);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @taskSecurity" +
            ".ownsTaskProject(#taskId,authentication))")
    public TaskResponse updateTask(Long taskId, TaskRequest taskRequest) {

        Task task =
                taskRepository.findById(taskId).
                        orElseThrow(()-> new ResourceNotFoundException("Task not found or access denied"));
        User assignee = resolveAssignee(taskRequest.assignedToId());
        task.setTitle(taskRequest.title());
        task.setDescription(taskRequest.description());
        task.setStatus(TaskStatus.valueOf(taskRequest.status()));
        task.setPriority(TaskPriority.valueOf(taskRequest.priority()));
        task.setDueDate(taskRequest.dueDate());
        task.setAssignedTo(assignee);

        Task saved = taskRepository.save(task);
        return toTaskResponse(saved);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or (hasRole('MANAGER') and @taskSecurity" +
            ".ownsTaskProject(#taskId,authentication))")
    public void deleteTask(Long taskId) {
       if(!taskRepository.existsById(taskId)){
           throw new ResourceNotFoundException("Task not found or access " +
                   "denied");
       }
        taskRepository.deleteById(taskId);

    }

    // ---- private helpers ----

    private User resolveAssignee(Long employeeId) {
        User candidate = userRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid assignee"));
        if (candidate.getRole() != Role.EMPLOYEE) {
            throw new IllegalArgumentException("Invalid assignee");
        }
        return candidate;
    }


    private Role extractRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();
        return principal.getRole();
    }

    private Long extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((CustomUserDetails) auth.getPrincipal()).getId();
    }

    private TaskResponse toTaskResponse(Task task) {

        ProjectSummaryResponse projectSummary = new ProjectSummaryResponse(
                task.getProject().getId(),
                task.getProject().getTitle()
        );

        UserSummaryResponse userSummary = new UserSummaryResponse(
                task.getAssignedTo().getId(),
                task.getAssignedTo().getName(),
                task.getAssignedTo().getRole()
        );

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDueDate(),
                projectSummary,
                userSummary,
                task.getCreatedAt()
        );
    }
}
