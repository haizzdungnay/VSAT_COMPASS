package com.example.v_sat_compass.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.v_sat_compass.MainActivity;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnLogin.setOnClickListener(v -> doLogin());

        binding.tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        binding.btnGoogleLogin.setOnClickListener(v ->
                Toast.makeText(this, "Tính năng đăng nhập Google sẽ sớm ra mắt!", Toast.LENGTH_SHORT).show());

        if (binding.btnBack != null) {
            binding.btnBack.setOnClickListener(v -> finish());
        }
    }

    private void doLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email hoặc số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.login(email, password).observe(this, resource -> {
            switch (resource.getStatus()) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.btnLogin.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finishAffinity();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnLogin.setEnabled(true);
                    String msg = resource.getMessage();
                    if (msg == null || msg.isEmpty()) msg = "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.";
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void showForgotPasswordDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Quên mật khẩu")
                .setMessage("Tính năng đặt lại mật khẩu sẽ gửi link xác nhận đến email của bạn.\n\nVui lòng liên hệ hỗ trợ tại: support@vsat-compass.vn")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }
}
