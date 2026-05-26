package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.UpdateUserRoleRequest;
import com.vsatcompass.api.dto.response.UserSummaryResponse;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.entity.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<UserSummaryResponse> listUsers(UserRole role, UserStatus status, String keyword, Pageable pageable);

    UserSummaryResponse updateRole(Long userId, UpdateUserRoleRequest request, Long actorUserId);

    void lockUser(Long userId, Long actorUserId);

    void unlockUser(Long userId, Long actorUserId);
}
