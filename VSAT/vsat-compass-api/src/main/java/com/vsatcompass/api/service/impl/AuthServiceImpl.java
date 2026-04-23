package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.AuthRequest;
import com.vsatcompass.api.dto.response.AuthResponse;
import com.vsatcompass.api.entity.RefreshToken;
import com.vsatcompass.api.entity.User;
import com.vsatcompass.api.entity.enums.GenderType;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.entity.enums.UserStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.mapper.UserMapper;
import com.vsatcompass.api.repository.RefreshTokenRepository;
import com.vsatcompass.api.repository.UserRepository;
import com.vsatcompass.api.security.jwt.JwtUtils;
import com.vsatcompass.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse.TokenPair register(AuthRequest.Register request) {
        // Check email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.authEmailTaken();
        }

        // Create user
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .role(UserRole.STUDENT)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {} (id={})", user.getEmail(), user.getId());

        return generateTokenPair(user, null);
    }

    @Override
    @Transactional
    public AuthResponse.TokenPair login(AuthRequest.Login request) {
        // Find user
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> AppException.authInvalidCredentials());

        // Check status
        if (user.getStatus() == UserStatus.LOCKED) {
            throw AppException.forbidden("Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }
        if (user.getStatus() == UserStatus.DEACTIVATED) {
            throw AppException.forbidden("Tài khoản đã bị vô hiệu hóa.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw AppException.authInvalidCredentials();
        }

        // Update last login
        user.setLastLoginAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("User logged in: {} (id={})", user.getEmail(), user.getId());
        return generateTokenPair(user, request.getDeviceInfo());
    }

    @Override
    @Transactional
    public AuthResponse.TokenPair refreshToken(AuthRequest.RefreshToken request) {
        // Validate refresh token in DB
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> AppException.authRefreshInvalid());

        // Check expiration
        if (storedToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.revokeByToken(request.getRefreshToken());
            throw AppException.authRefreshInvalid();
        }

        // Revoke old token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Generate new pair
        User user = storedToken.getUser();
        return generateTokenPair(user, storedToken.getDeviceInfo());
    }

    @Override
    @Transactional
    public void logout(AuthRequest.RefreshToken request) {
        refreshTokenRepository.revokeByToken(request.getRefreshToken());
        log.info("User logged out, token revoked");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse.UserInfo getMe(Long userId) {
        User user = findUserById(userId);
        return userMapper.toUserInfo(user);
    }

    @Override
    @Transactional
    public AuthResponse.UserInfo updateProfile(Long userId, AuthRequest.UpdateProfile request) {
        User user = findUserById(userId);

        if (request.getFullName() != null) user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
        if (request.getGender() != null) {
            try {
                user.setGender(GenderType.valueOf(request.getGender()));
            } catch (IllegalArgumentException e) {
                throw AppException.badRequest("Giới tính không hợp lệ. Chọn: MALE, FEMALE, OTHER");
            }
        }
        if (request.getDateOfBirth() != null) {
            try {
                user.setDateOfBirth(LocalDate.parse(request.getDateOfBirth()));
            } catch (Exception e) {
                throw AppException.badRequest("Ngày sinh không hợp lệ. Format: yyyy-MM-dd");
            }
        }

        user = userRepository.save(user);
        return userMapper.toUserInfo(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, AuthRequest.ChangePassword request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw AppException.badRequest("Mật khẩu cũ không đúng");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke all refresh tokens (force re-login on other devices)
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("Password changed for user id={}, all tokens revoked", userId);
    }

    // ========== Private helpers ==========

    private AuthResponse.TokenPair generateTokenPair(User user, String deviceInfo) {
        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshTokenStr = jwtUtils.generateRefreshToken(user.getId());

        // Save refresh token to DB
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .deviceInfo(deviceInfo)
                .expiresAt(OffsetDateTime.now().plusSeconds(jwtUtils.getRefreshTokenExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .user(userMapper.toUserInfo(user))
                .build();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User", userId));
    }
}
