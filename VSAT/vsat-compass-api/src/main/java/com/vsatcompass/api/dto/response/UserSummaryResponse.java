package com.vsatcompass.api.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryResponse {

    private Long id;

    @JsonProperty("full_name")
    private String fullName;

    private String email;
    private String phone;
    private UserRole role;
    private UserStatus status;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("last_login_at")
    private OffsetDateTime lastLoginAt;
}
