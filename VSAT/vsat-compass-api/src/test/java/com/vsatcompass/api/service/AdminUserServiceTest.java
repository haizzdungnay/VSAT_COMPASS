package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.UpdateUserRoleRequest;
import com.vsatcompass.api.dto.response.UserSummaryResponse;
import com.vsatcompass.api.entity.User;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.entity.enums.UserStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.UserRepository;
import com.vsatcompass.api.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock UserRepository userRepository;

    @InjectMocks AdminUserServiceImpl adminUserService;

    @Test
    @DisplayName("listUsers maps repository page to summary DTOs")
    void listUsers_returnsMappedPage() {
        User user = User.builder()
                .id(1L)
                .email("a@test.vn")
                .fullName("User A")
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .passwordHash("hash")
                .build();
        Page<User> page = new PageImpl<>(List.of(user), PageRequest.of(0, 50), 1);
        when(userRepository.findByFilters(eq(UserRole.STUDENT), eq(null), eq(null), any()))
                .thenReturn(page);

        Page<UserSummaryResponse> result = adminUserService.listUsers(
                UserRole.STUDENT, null, null, PageRequest.of(0, 50));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("a@test.vn");
    }

    @Test
    @DisplayName("lockUser rejects self-lock")
    void lockUser_selfLock_throwsBadRequest() {
        assertThatThrownBy(() -> adminUserService.lockUser(9L, 9L))
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("BAD_REQUEST"));
    }

    @Test
    @DisplayName("updateRole prevents demoting last super admin")
    void updateRole_lastSuperAdmin_throwsInvalidState() {
        User admin = User.builder()
                .id(1L)
                .email("admin@test.vn")
                .fullName("Admin")
                .role(UserRole.SUPER_ADMIN)
                .status(UserStatus.ACTIVE)
                .passwordHash("hash")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRole(UserRole.SUPER_ADMIN)).thenReturn(1L);

        UpdateUserRoleRequest request = new UpdateUserRoleRequest();
        request.setRole(UserRole.STUDENT);

        assertThatThrownBy(() -> adminUserService.updateRole(1L, request, 1L))
                .isInstanceOfSatisfying(AppException.class, ex ->
                        assertThat(ex.getCode()).isEqualTo("INVALID_STATE"));
    }

    @Test
    @DisplayName("unlockUser sets status ACTIVE")
    void unlockUser_setsActive() {
        User user = User.builder()
                .id(2L)
                .email("b@test.vn")
                .fullName("User B")
                .role(UserRole.STUDENT)
                .status(UserStatus.LOCKED)
                .passwordHash("hash")
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        adminUserService.unlockUser(2L, 1L);

        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepository).save(user);
    }
}
