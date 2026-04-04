package com.example.v_sat_compass.data.api;

import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.AuthResponse;
import com.example.v_sat_compass.data.model.LoginRequest;
import com.example.v_sat_compass.data.model.RegisterRequest;
import com.example.v_sat_compass.data.model.UserProfile;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface AuthApi {

    @POST("auth/login")
    Call<ApiResponse<AuthResponse>> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<ApiResponse<AuthResponse>> register(@Body RegisterRequest request);

    @POST("auth/refresh")
    Call<ApiResponse<AuthResponse>> refreshToken(@Body java.util.Map<String, String> body);

    @POST("auth/logout")
    Call<ApiResponse<Void>> logout(@Body java.util.Map<String, String> body);

    @GET("auth/me")
    Call<ApiResponse<UserProfile>> getMe();
}
