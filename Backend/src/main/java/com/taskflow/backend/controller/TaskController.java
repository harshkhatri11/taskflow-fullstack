package com.taskflow.backend.controller;

import com.taskflow.backend.dto.request.TaskRequest;
import com.taskflow.backend.dto.request.TaskStatusUpdateRequest;
import com.taskflow.backend.dto.response.TaskResponse;
import com.taskflow.backend.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // ADMIN-only
    @GetMapping("/api/tasks")
    public ResponseEntity<List<TaskResponse>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    // Pageable auto-binds from query params: ?page=0&size=20&sort=dueDate,desc
    // Spring Data resolves this automatically — no manual @RequestParam parsing
    // needed. @PageableDefault sets sane fallbacks if the client omits params.
    @GetMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<Page<TaskResponse>> getTasksByProjectId(
            @PathVariable Long projectId,
            @PageableDefault(size = 20, sort = "dueDate") Pageable pageable) {
        return ResponseEntity.ok(taskService.getTasksByProjectId(projectId, pageable));
    }

    @PostMapping("/api/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(@PathVariable Long projectId,
                                                   @Valid @RequestBody TaskRequest request) {
        TaskResponse created = taskService.createTask(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Deliberately separate from PUT /api/tasks/{id} — Employee can ONLY reach
    // this narrow endpoint (TaskStatusUpdateRequest structurally has no title/
    // priority/assignedToId fields to tamper with), never the full PUT below.
    // This is least-privilege enforced by DTO shape + routing, not just
    // @PreAuthorize — see TaskServiceImpl.updateTaskStatus's three-way SpEL.
    @PatchMapping("/api/tasks/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(@PathVariable Long id,
                                                         @Valid @RequestBody TaskStatusUpdateRequest request) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, request));
    }

    // Full edit — ADMIN/owning MANAGER only, never reachable by EMPLOYEE
    // (TaskServiceImpl's @PreAuthorize rejects EMPLOYEE outright here).
    @PutMapping("/api/tasks/{id}")
    public ResponseEntity<TaskResponse> updateTask(@PathVariable Long id,
                                                   @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/api/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}