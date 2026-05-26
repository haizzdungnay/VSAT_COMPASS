package com.vsatcompass.api.dto.request;

import com.vsatcompass.api.entity.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateUserRoleRequest {

    @NotNull(message = "role không được để trống")
    private UserRole role;
}
