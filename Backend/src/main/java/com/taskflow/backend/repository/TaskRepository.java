package com.taskflow.backend.repository;

import com.taskflow.backend.entity.Task;
import com.taskflow.backend.enums.TaskStatus;
import com.taskflow.backend.repository.projection.AssignedTaskCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Paginated task lists, scoped by role (used by GET /api/projects/{projectId}/tasks
    // and similar list endpoints)
    Page<Task> getTasksByProjectId(Long projectId, Pageable pageable);

    Page<Task> getTasksByAssignedToId(Long userId, Pageable pageable);

    Page<Task> getTasksByProjectManagerId(Long managerId, Pageable pageable);

    // Does this user have ANY task assigned to them, in any project?
    // Used as a delete-guard in UserServiceImpl.deleteUser (block deletion if
    // the user still has assigned work).
    boolean existsByAssignedToId(Long assignedToId);

    // Does this specific project have a task assigned to this specific employee?
    // Used in ProjectServiceImpl.getProjectById to check EMPLOYEE-level project
    // access per the permission matrix ("theirs via tasks").
    boolean existsByProjectIdAndAssignedToId(Long projectId, Long assignedToId);

    // Does this project have ANY task at all, regardless of assignee?
    // Used as a delete-guard in ProjectServiceImpl.deleteProject (block deletion
    // if the project still has tasks, mirroring deleteUser's guard style).
    boolean existsByProjectId(Long projectId);

    Page<Task> getTasksByProjectIdAndProjectManagerId(Long projectId, Long managerId, Pageable pageable);
    Page<Task> getTasksByProjectIdAndAssignedToId(Long projectId, Long assignedToId, Pageable pageable);

    boolean existsByIdAndProjectManagerId(Long taskId, Long managerId);
    boolean existsByIdAndAssignedToId(Long taskId, Long assignedToId);

    // Does the given user have ANY task assigned to them within the SAME PROJECT
    // as the given task (not necessarily that exact task)? Used by
    // CommentSecurity.hasAssignedTaskInProject — a self-join, since a plain
    // derived method name can't express "same project as a DIFFERENT task's
    // project" without loading that task first. Not the DISTINCT-forces-@Query
    // case from the repository rule, but the same underlying reason: this
    // relationship shape genuinely can't be named as a derived method.
    @Query("select case when count(t2) > 0 then true else false end " +
            "from Task t1, Task t2 " +
            "where t1.id = :taskId and t2.project = t1.project and t2.assignedTo.id = :userId")
    boolean existsAssignedTaskInSameProjectAsTask(@Param("taskId") Long taskId, @Param("userId") Long userId);

    // Grouped aggregate: one row per employee with at least one task whose
    // status is NOT the excluded one, paired with their count. Same batch
    // pattern as ProjectRepository.countProjectsGroupedByManager — fetched
    // once, looked up per-user via a Map in UserServiceImpl.
    @Query("SELECT t.assignedTo.id AS assignedToId, COUNT(t) AS count " +
            "FROM Task t WHERE t.status <> :excludedStatus " +
            "GROUP BY t.assignedTo.id")
    List<AssignedTaskCount> countTasksGroupedByAssignee(@Param("excludedStatus") TaskStatus excludedStatus);

}