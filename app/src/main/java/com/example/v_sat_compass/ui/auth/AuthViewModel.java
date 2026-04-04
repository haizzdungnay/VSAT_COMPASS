package com.example.v_sat_compass.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.v_sat_compass.data.model.AuthResponse;
import com.example.v_sat_compass.data.model.UserProfile;
import com.example.v_sat_compass.data.repository.AuthRepository;
import com.example.v_sat_compass.data.repository.Resource;

public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository = new AuthRepository();

    public LiveData<Resource<AuthResponse>> login(String email, String password) {
        return authRepository.login(email, password);
    }

    public LiveData<Resource<AuthResponse>> register(String email, String password, String fullName) {
        return authRepository.register(email, password, fullName);
    }

    public LiveData<Resource<UserProfile>> getMe() {
        return authRepository.getMe();
    }

    public void logout() {
        authRepository.logout();
    }
}
