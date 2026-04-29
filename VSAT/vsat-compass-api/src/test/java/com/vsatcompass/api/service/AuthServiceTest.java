package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AuthRequest;
import com.vsatcompass.api.dto.response.AuthResponse;
import com.vsatcompass.api.entity.RefreshToken;
import com.vsatcompass.api.entity.User;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.entity.enums.UserStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.mapper.UserMapper;
import com.vsatcompass.api.repository.RefreshTokenRepository;
import com.vsatcompass.api.repository.UserRepository;
import com.vsatcompass.api.security.jwt.JwtUtils;
import com.vsatcompass.api.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — characterization tests (Spring Boot 3.2.5 baseline)")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtils jwtUtils;
    @Mock UserMapper userMapper;

    @InjectMocks AuthServiceImpl authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .email("student@vsat.com")
                .passwordHash("$2a$10$syntheticHashedValueForTests")
                .fullName("Nguyễn Test")
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
    }

    private AuthRequest.Register registerReq() {
        AuthRequest.Register r = new AuthRequest.Register();
        r.setEmail("New@vsat.com");
        r.setPassword("Passw0rd!");
        r.setFullName("  Học Viên Mới  ");
        r.setPhone("0900000000");
        return r;
    }

    private AuthRequest.Login loginReq(String email, String password) {
        AuthRequest.Login r = new AuthRequest.Login();
        r.setEmail(email);
        r.setPassword(password);
        r.setDeviceInfo("Pixel-Test");
        return r;
    }

    private AuthRequest.RefreshToken refreshReq(String token) {
        AuthRequest.RefreshToken r = new AuthRequest.RefreshToken();
        r.setRefreshToken(token);
        return r;
    }

    // ===== register =====

    @Test
    @DisplayName("register: happy path persists user with normalized email and returns token pair")
    void register_happyPath_persistsUserAndReturnsTokens() {
        when(userRepository.existsByEmail("New@vsat.com")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });
        when(jwtUtils.generateAccessToken(eq(42L), anyString(), eq("STUDENT"))).thenReturn("ACCESS_T");
        when(jwtUtils.generateRefreshToken(42L)).thenReturn("REFRESH_T");
        when(jwtUtils.getRefreshTokenExpirationMs()).thenReturn(604_800_000L);
        when(userMapper.toUserInfo(any(User.class))).thenReturn(AuthResponse.UserInfo.builder().id(42L).build());

        AuthResponse.TokenPair pair = authService.register(registerReq());

        assertThat(pair).isNotNull();
        assertThat(pair.getAccessToken()).isEqualTo("ACCESS_T");
        assertThat(pair.getRefreshToken()).isEqualTo("REFRESH_T");

        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCap.capture());
        User saved = userCap.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@vsat.com");
        assertThat(saved.getFullName()).isEqualTo("Học Viên Mới");
        assertThat(saved.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getEmailVerified()).isFalse();
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$10$encoded");

        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("register: duplicate email throws AUTH_EMAIL_TAKEN with 409 and does not save user")
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("New@vsat.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerReq()))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("AUTH_EMAIL_TAKEN");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                });

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    // ===== login =====

    @Test
    @DisplayName("login: happy path returns tokens and updates lastLoginAt")
    void login_happyPath_returnsTokensAndUpdatesLastLogin() {
        when(userRepository.findByEmail("student@vsat.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("Passw0rd!", existingUser.getPasswordHash())).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtils.generateAccessToken(eq(1L), eq("student@vsat.com"), eq("STUDENT"))).thenReturn("ACCESS");
        when(jwtUtils.generateRefreshToken(1L)).thenReturn("REFRESH");
        when(jwtUtils.getRefreshTokenExpirationMs()).thenReturn(604_800_000L);
        when(userMapper.toUserInfo(any(User.class))).thenReturn(AuthResponse.UserInfo.builder().id(1L).build());

        OffsetDateTime before = OffsetDateTime.now().minusSeconds(1);
        AuthResponse.TokenPair pair = authService.login(loginReq("STUDENT@vsat.com", "Passw0rd!"));

        assertThat(pair.getAccessToken()).isEqualTo("ACCESS");
        assertThat(pair.getRefreshToken()).isEqualTo("REFRESH");
        assertThat(existingUser.getLastLoginAt()).isAfter(before);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login: wrong password throws AUTH_INVALID_CREDENTIALS with 401 (no token issued)")
    void login_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail("student@vsat.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("WrongPass1", existingUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginReq("student@vsat.com", "WrongPass1")))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("AUTH_INVALID_CREDENTIALS");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });

        verify(refreshTokenRepository, never()).save(any());
        verify(jwtUtils, never()).generateAccessToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("login: non-existent email throws AUTH_INVALID_CREDENTIALS (does not leak existence)")
    void login_nonExistentEmail_throwsUnauthorized() {
        when(userRepository.findByEmail("nobody@vsat.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginReq("nobody@vsat.com", "Passw0rd!")))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("AUTH_INVALID_CREDENTIALS");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("login: non-ACTIVE account (LOCKED or DEACTIVATED) throws FORBIDDEN before password check")
    void login_nonActiveAccount_throwsForbidden() {
        for (UserStatus blocked : new UserStatus[]{UserStatus.LOCKED, UserStatus.DEACTIVATED}) {
            existingUser.setStatus(blocked);
            when(userRepository.findByEmail("student@vsat.com")).thenReturn(Optional.of(existingUser));

            assertThatThrownBy(() -> authService.login(loginReq("student@vsat.com", "Passw0rd!")))
                    .as("status=%s should throw FORBIDDEN", blocked)
                    .isInstanceOfSatisfying(AppException.class, ex -> {
                        assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    });
        }

        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    // ===== refresh =====

    @Test
    @DisplayName("refresh: valid token rotates pair and revokes the old token")
    void refresh_validToken_rotatesAndRevokesOld() {
        RefreshToken stored = RefreshToken.builder()
                .id(99L)
                .user(existingUser)
                .token("OLD_REFRESH")
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenAndRevokedFalse("OLD_REFRESH"))
                .thenReturn(Optional.of(stored));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtUtils.generateAccessToken(eq(1L), eq("student@vsat.com"), eq("STUDENT"))).thenReturn("NEW_ACCESS");
        when(jwtUtils.generateRefreshToken(1L)).thenReturn("NEW_REFRESH");
        when(jwtUtils.getRefreshTokenExpirationMs()).thenReturn(604_800_000L);
        when(userMapper.toUserInfo(any(User.class))).thenReturn(AuthResponse.UserInfo.builder().id(1L).build());

        AuthResponse.TokenPair pair = authService.refreshToken(refreshReq("OLD_REFRESH"));

        assertThat(pair.getAccessToken()).isEqualTo("NEW_ACCESS");
        assertThat(pair.getRefreshToken()).isEqualTo("NEW_REFRESH");
        assertThat(stored.getRevoked()).isTrue();
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("refresh: revoked or unknown token throws AUTH_REFRESH_INVALID with 401")
    void refresh_revokedOrUnknownToken_throwsUnauthorized() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse("REVOKED")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(refreshReq("REVOKED")))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("AUTH_REFRESH_INVALID");
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });

        verify(refreshTokenRepository, never()).save(any());
        verify(jwtUtils, never()).generateAccessToken(anyLong(), anyString(), anyString());
    }

    @Test
    @DisplayName("refresh: expired token is revoked via revokeByToken and throws AUTH_REFRESH_INVALID")
    void refresh_expiredToken_revokesAndThrows() {
        RefreshToken expired = RefreshToken.builder()
                .id(50L)
                .user(existingUser)
                .token("EXPIRED")
                .expiresAt(OffsetDateTime.now().minusMinutes(1))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenAndRevokedFalse("EXPIRED"))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refreshToken(refreshReq("EXPIRED")))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo("AUTH_REFRESH_INVALID");
                });

        verify(refreshTokenRepository).revokeByToken("EXPIRED");
        verify(jwtUtils, never()).generateAccessToken(anyLong(), anyString(), anyString());
    }

    // ===== logout =====

    @Test
    @DisplayName("logout: revokes the refresh token via revokeByToken")
    void logout_revokesToken() {
        authService.logout(refreshReq("SOME_REFRESH"));

        verify(refreshTokenRepository).revokeByToken("SOME_REFRESH");
    }
}
