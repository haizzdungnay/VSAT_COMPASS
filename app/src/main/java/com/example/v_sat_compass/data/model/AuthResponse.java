package com.example.v_sat_compass.data.model;

public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private UserProfile user;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public UserProfile getUser() { return user; }
}
