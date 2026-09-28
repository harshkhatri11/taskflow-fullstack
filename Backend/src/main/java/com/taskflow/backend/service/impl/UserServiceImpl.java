package com.taskflow.backend.service.impl;

import com.taskflow.backend.dto.request.RegisterRequest;
import com.taskflow.backend.dto.request.UserRequest;
import com.taskflow.backend.dto.request.UserUpdateRequest;
import com.taskflow.backend.dto.response.UserListItem;
import com.taskflow.backend.dto.response.UserResponse;
import com.taskflow.backend.dto.response.UserSummaryResponse;
import com.taskflow.backend.entity.User;
import com.taskflow.backend.enums.ProjectStatus;
import com.taskflow.backend.enums.Role;
import com.taskflow.backend.enums.TaskStatus;
import com.taskflow.backend.exception.ResourceNotFoundException;
import com.taskflow.backend.repository.ProjectRepository;
import com.taskflow.backend.repository.TaskRepository;
import com.taskflow.backend.repository.UserRepository;
import com.taskflow.backend.repository.projection.AssignedTaskCount;
import com.taskflow.backend.repository.projection.ManagerProjectCount;
import com.taskflow.backend.security.CustomUserDetails;
import com.taskflow.backend.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    // No @PreAuthorize — public endpoint, called by AuthController.register().
    // Role is always hardcoded EMPLOYEE below; never accepts a caller-supplied role.
    @Override
    public UserResponse register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new IllegalArgumentException("Email address is already in use");
        }

        User user = new User();
        user.setName(registerRequest.name());
        user.setEmail(registerRequest.email());
        user.setPassword(passwordEncoder.encode(registerRequest.password()));
        user.setRole(Role.EMPLOYEE);
        user.setCreatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        return toUserResponse(savedUser, Map.of(), Map.of());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse createUser(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.email())) {
            throw new IllegalArgumentException("Email address is already in use");
        }

        User user = new User();
        user.setName(userRequest.name());
        user.setEmail(userRequest.email());
        user.setPassword(passwordEncoder.encode(userRequest.password()));
        user.setRole(Role.valueOf(userRequest.role()));
        user.setCreatedAt(Instant.now());

        User savedUser = userRepository.save(user);
        return toUserResponse(savedUser, Map.of(), Map.of());
    }

    // ADMIN-only, no self-protection guard by design — an ADMIN can demote
    // or (via deleteUser) remove their own account. Email uniqueness is
    // re-checked only when the email actually changes, so re-submitting a
    // user's own unchanged email never false-positives as "already in use".
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(Long userId, UserUpdateRequest userUpdateRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(userUpdateRequest.email());
        if (emailChanged && userRepository.existsByEmail(userUpdateRequest.email())) {
            throw new IllegalArgumentException("Email address is already in use");
        }

        user.setName(userUpdateRequest.name());
        user.setEmail(userUpdateRequest.email());
        user.setRole(Role.valueOf(userUpdateRequest.role()));
        User savedUser = userRepository.save(user);
        return toUserResponse(savedUser, Map.of(), Map.of());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }

        if (!projectRepository.findByManagerId(userId).isEmpty()) {
            throw new IllegalArgumentException("Cannot delete: user still owns active projects");
        }

        if (taskRepository.existsByAssignedToId(userId)) {
            throw new IllegalArgumentException("Cannot delete: user still has tasks assigned");
        }

        userRepository.deleteById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return toUserResponse(user, Map.of(), Map.of());
    }

    // Gate lives HERE, not on getAllUsersFull/getAllUsersSummary — those two
    // are called via `this.` below, which bypasses the CGLIB proxy entirely
    // (Spring AOP self-invocation limitation). @PreAuthorize on them would be
    // silently skipped when reached this way, so this outer method is the
    // only enforcement point.
    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public List<UserListItem> getUsers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Role role = ((CustomUserDetails) auth.getPrincipal()).getRole();

        return switch (role) {
            case ADMIN -> List.<UserListItem>copyOf(getAllUsersFull());
            case MANAGER -> List.<UserListItem>copyOf(getAllUsersSummary());
            case EMPLOYEE -> throw new IllegalStateException("Unreachable — gated by @PreAuthorize above");
        };
    }

    @Override
    public List<UserResponse> getAllUsersFull() {
        List<User> users = userRepository.findAll();

        Map<Long, Long> activeProjectCountByManager = projectRepository
                .countProjectsGroupedByManager(ProjectStatus.ACTIVE).stream()
                .collect(Collectors.toMap(ManagerProjectCount::getManagerId, ManagerProjectCount::getCount));

        Map<Long, Long> openTaskCountByAssignee = taskRepository
                .countTasksGroupedByAssignee(TaskStatus.DONE).stream()
                .collect(Collectors.toMap(AssignedTaskCount::getAssignedToId, AssignedTaskCount::getCount));

        return users.stream()
                .map(user -> toUserResponse(user, activeProjectCountByManager, openTaskCountByAssignee))
                .toList();
    }

    @Override
    public List<UserSummaryResponse> getAllUsersSummary() {
        return userRepository.findAll().stream().map(this::toUserSummaryResponse).toList();
    }

    @Override
    public Boolean checkEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }


    private UserResponse toUserResponse(User user,
                                        Map<Long, Long> activeProjectCountByManager,
                                        Map<Long, Long> openTaskCountByAssignee) {
        Long managedProjectCount = user.getRole() == Role.MANAGER
                ? activeProjectCountByManager.getOrDefault(user.getId(), 0L)
                : null;
        Long assignedTaskCount = user.getRole() == Role.EMPLOYEE
                ? openTaskCountByAssignee.getOrDefault(user.getId(), 0L)
                : null;

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                managedProjectCount,
                assignedTaskCount
        );
    }

    private UserSummaryResponse toUserSummaryResponse(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getName(),
                user.getRole()
        );
    }
}