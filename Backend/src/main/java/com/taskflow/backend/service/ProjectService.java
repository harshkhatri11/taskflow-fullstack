package com.taskflow.backend.service;

import com.taskflow.backend.dto.request.ProjectRequest;
import com.taskflow.backend.dto.response.ProjectResponse;

import java.util.List;

public interface ProjectService {

    List<ProjectResponse> getProjects();

    /**
     * Returns a single project by id, scoped by the same access rules as getProjects().
     * Throws 404 if the project does not exist, OR if the caller does not have access
     * per the matrix (deliberate — do not confirm existence to a caller without access).
     *
     * NOTE: deliberately NOT gated with @PreAuthorize, because that would throw 403
     * on access-denial, breaking the intended 404 asymmetry. Ownership/scope logic
     * stays inline, reusing ProjectSecurity's methods as plain method calls.
     */
    ProjectResponse getProjectById(Long id);

    ProjectResponse createProject(ProjectRequest projectRequest);

    /**
     * ADMIN or owning MANAGER only — enforced via @PreAuthorize + @projectSecurity
     * on the implementation. Uses 403, unlike getProjectById's 404 — intentional,
     * documented asymmetry.
     */
    ProjectResponse updateProject(Long id, ProjectRequest projectRequest);

    void deleteProject(Long id);
}