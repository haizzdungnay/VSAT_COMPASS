package com.vsatcompass.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

public class AuthResponse {

    @Data
    @Builder
    public static class TokenPair {
        private String accessToken;
        private String refreshToken;
        private UserInfo user;
    }

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String email;
        private String fullName;
        private String phone;
        private String gender;
        private String dateOfBirth;
        private String avatarUrl;
        private String role;
        private String status;
        private Boolean emailVerified;
        private OffsetDateTime lastLoginAt;
        private OffsetDateTime createdAt;
    }
}
