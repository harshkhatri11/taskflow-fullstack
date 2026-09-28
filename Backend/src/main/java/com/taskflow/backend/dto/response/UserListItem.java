package com.taskflow.backend.dto.response;

import com.taskflow.backend.enums.Role;

/**
 * Common shape shared by UserResponse (ADMIN's full view) and
 * UserSummaryResponse (MANAGER's limited view).
 * Records automatically satisfy this via their
 * generated accessors — no method bodies needed, just `implements`.
 * Doesn't change either record's JSON output: Jackson serializes by
 * runtime type, not by whatever type a method signature declares.
 */
public interface UserListItem {
    Long id();
    String name();
    Role role();
}