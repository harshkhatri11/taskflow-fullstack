package com.taskflow.backend.repository.projection;

// Spring Data interface projection for TaskRepository.countTasksGroupedByAssignee.
public interface AssignedTaskCount {
    Long getAssignedToId();
    Long getCount();
}
