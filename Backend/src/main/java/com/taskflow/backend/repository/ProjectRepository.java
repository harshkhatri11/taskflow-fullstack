package com.taskflow.backend.repository;

import com.taskflow.backend.entity.Project;
import com.taskflow.backend.enums.ProjectStatus;
import com.taskflow.backend.repository.projection.ManagerProjectCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByManagerId(Long managerId);

    @Query("SELECT DISTINCT t.project FROM Task t WHERE t.assignedTo.id = :employeeId")
    List<Project> findProjectsByAssignedEmployeeId(@Param("employeeId") Long employeeId);

    boolean existsByIdAndManagerId(Long projectId, Long managerId);

    // Grouped aggregate: one row per manager who owns at least one project with
    // the given status, paired with their count. Used by UserServiceImpl to
    // populate UserResponse.managedProjectCount for the admin user-card list —
    // fetched ONCE for all managers, then looked up per-user via a Map, rather
    // than querying per-row (would be an N+1 across the user list).
    @Query("SELECT p.manager.id AS managerId, COUNT(p) AS count " +
            "FROM Project p WHERE p.status = :status " +
            "GROUP BY p.manager.id")
    List<ManagerProjectCount> countProjectsGroupedByManager(@Param("status") ProjectStatus status);

}