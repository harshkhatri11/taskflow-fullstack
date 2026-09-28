package com.taskflow.backend.repository.projection;

// Spring Data interface projection for ProjectRepository.countProjectsGroupedByManager.
// Method names must match the @Query alias names exactly (managerId, count).
public interface ManagerProjectCount {
    Long getManagerId();
    Long getCount();
}
