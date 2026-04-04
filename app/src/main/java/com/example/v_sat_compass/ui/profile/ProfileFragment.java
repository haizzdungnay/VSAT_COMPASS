package com.example.v_sat_compass.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.AuthApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.UserProfile;
import com.example.v_sat_compass.databinding.FragmentProfileBinding;
import com.example.v_sat_compass.ui.auth.LoginActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        loadProfile();

        binding.btnLogout.setOnClickListener(v -> {
            ApiClient.clearTokens();
            startActivity(new Intent(requireActivity(), LoginActivity.class));
            requireActivity().finishAffinity();
        });
    }

    private void loadProfile() {
        AuthApi api = ApiClient.getClient().create(AuthApi.class);
        api.getMe().enqueue(new Callback<ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfile>> call, Response<ApiResponse<UserProfile>> response) {
                if (binding == null) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    UserProfile user = response.body().getData();
                    binding.tvFullName.setText(user.getFullName() != null ? user.getFullName() : "N/A");
                    binding.tvEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
                    binding.tvPhone.setText(user.getPhone() != null ? user.getPhone() : "Chua cap nhat");
                    binding.tvRole.setText(user.getRole() != null ? user.getRole() : "STUDENT");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfile>> call, Throwable t) {
                // silent fail
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
