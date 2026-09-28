package com.taskflow.backend.service;


import com.taskflow.backend.dto.request.TaskRequest;
import com.taskflow.backend.dto.request.TaskStatusUpdateRequest;
import com.taskflow.backend.dto.response.TaskResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TaskService {

    List<TaskResponse> getAllTasks();

    Page<TaskResponse> getTasksByProjectId(Long projectId, Pageable pageable);

    TaskResponse createTask(Long projectId, TaskRequest taskRequest);

    TaskResponse updateTaskStatus(Long taskId, TaskStatusUpdateRequest taskStatusUpdateRequest);

    TaskResponse updateTask(Long taskId, TaskRequest taskRequest);

    void deleteTask(Long taskId);
}
