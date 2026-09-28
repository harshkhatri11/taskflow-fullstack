package com.taskflow.backend.service;

import com.taskflow.backend.dto.request.RegisterRequest;
import com.taskflow.backend.dto.request.UserRequest;
import com.taskflow.backend.dto.request.UserUpdateRequest;
import com.taskflow.backend.dto.response.UserListItem;
import com.taskflow.backend.dto.response.UserResponse;
import com.taskflow.backend.dto.response.UserSummaryResponse;

import java.util.List;

public interface UserService {

    UserResponse register(RegisterRequest registerRequest);

    UserResponse createUser(UserRequest userRequest);

    /**
     * ADMIN-only. Updates name/email/role — no password field, that's a
     * separate concern. No self-protection guard: an ADMIN can demote or
     * (via deleteUser) remove their own account, by design choice.
     */
    UserResponse updateUser(Long userId, UserUpdateRequest userUpdateRequest);

    void deleteUser(Long userId);

    UserResponse getCurrentUser();

    /**
     * Role-aware listing for GET /api/users: ADMIN gets full records
     * (getAllUsersFull), MANAGER gets limited read-only fields
     * (getAllUsersSummary). The switch lives here, not in the controller,
     * per the project's gate-vs-scope rule — this is a scoping decision
     * (same call, different response shape per role), not an authorization
     * gate on its own, though the method itself IS gated to ADMIN/MANAGER only.
     */
    List<UserListItem> getUsers();

    List<UserResponse> getAllUsersFull();

    List<UserSummaryResponse> getAllUsersSummary();

    Boolean checkEmailExists(String email);
}