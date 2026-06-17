package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.UpdateUserRoleRequest;
import com.vsatcompass.api.dto.response.UserSummaryResponse;
import com.vsatcompass.api.entity.User;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.entity.enums.UserStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.UserRepository;
import com.vsatcompass.api.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> listUsers(
            UserRole role,
            UserStatus status,
            String keyword,
            Pageable pageable
    ) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return userRepository.findAll(buildUserFilter(role, status, normalizedKeyword), pageable)
                .map(this::toSummary);
    }

    private Specification<User> buildUserFilter(UserRole role, UserStatus status, String keyword) {
        Specification<User> spec = Specification.where(null);

        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (keyword != null) {
            String pattern = "%" + keyword.toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)
            ));
        }

        return spec;
    }

    @Override
    @Transactional
    public UserSummaryResponse updateRole(Long userId, UpdateUserRoleRequest request, Long actorUserId) {
        User user = loadUser(userId);
        UserRole newRole = request.getRole();

        if (user.getId().equals(actorUserId) && user.getRole() == UserRole.SUPER_ADMIN
                && newRole != UserRole.SUPER_ADMIN) {
            long superAdminCount = userRepository.countByRole(UserRole.SUPER_ADMIN);
            if (superAdminCount <= 1) {
                throw AppException.invalidState("Không thể hạ quyền Super Admin cuối cùng");
            }
        }

        user.setRole(newRole);
        return toSummary(userRepository.save(user));
    }

    @Override
    @Transactional
    public void lockUser(Long userId, Long actorUserId) {
        if (userId.equals(actorUserId)) {
            throw AppException.badRequest("Không thể khoá tài khoản của chính bạn");
        }
        User user = loadUser(userId);
        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void unlockUser(Long userId, Long actorUserId) {
        User user = loadUser(userId);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User", userId));
    }

    private UserSummaryResponse toSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .avatarUrl(user.getAvatarUrl())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
