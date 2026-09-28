package com.taskflow.backend.repository;

import com.taskflow.backend.entity.Comment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment,Long> {

    List<Comment> getCommentsByTaskIdOrderByCreatedAtAsc(Long taskId);

    boolean existsByIdAndAuthorId(Long commentId, Long authorId);

    boolean existsByIdAndTaskProjectManagerId(Long commentId, Long managerId);

    @Query("select case when count(t2) > 0 then true else false end " +
            "from Task t1, Task t2 " +
            "where t1.id = :taskId and t2.project = t1.project and t2.assignedTo.id = :userId")
    boolean existsAssignedTaskInSameProjectAsTask(@Param("taskId") Long taskId, @Param("userId") Long userId);
}
