package com.vsatcompass.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequest {

    @Data
    public static class Register {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        private String email;

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8-100 ký tự")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).+$",
                message = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ và số"
        )
        private String password;

        @NotBlank(message = "Họ tên không được để trống")
        @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
        private String fullName;

        private String phone;
    }

    @Data
    public static class Login {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        private String email;

        @NotBlank(message = "Mật khẩu không được để trống")
        private String password;

        private String deviceInfo;
    }

    @Data
    public static class RefreshToken {
        @NotBlank(message = "Refresh token không được để trống")
        private String refreshToken;
    }

    @Data
    public static class ForgotPassword {
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        private String email;
    }

    @Data
    public static class ResetPassword {
        @NotBlank
        private String token;

        @NotBlank
        @Size(min = 8, max = 100)
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).+$",
                message = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ và số"
        )
        private String newPassword;
    }

    @Data
    public static class ChangePassword {
        @NotBlank(message = "Mật khẩu cũ không được để trống")
        private String oldPassword;

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 100, message = "Mật khẩu mới phải từ 8-100 ký tự")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).+$",
                message = "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ và số"
        )
        private String newPassword;
    }

    @Data
    public static class UpdateProfile {
        @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
        private String fullName;

        private String phone;
        private String gender;
        private String dateOfBirth;
        private String avatarUrl;
    }
}
