package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AuthRequest;
import com.vsatcompass.api.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse.TokenPair register(AuthRequest.Register request);

    AuthResponse.TokenPair login(AuthRequest.Login request);

    AuthResponse.TokenPair refreshToken(AuthRequest.RefreshToken request);

    void logout(AuthRequest.RefreshToken request);

    AuthResponse.UserInfo getMe(Long userId);

    AuthResponse.UserInfo updateProfile(Long userId, AuthRequest.UpdateProfile request);

    void changePassword(Long userId, AuthRequest.ChangePassword request);
}
