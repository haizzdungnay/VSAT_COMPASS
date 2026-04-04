const fs = require("fs");
const path = require("path");
const base = "D:/lap trinh kiem com/Java/VSAT_COMPASS/app/src/main/java/com/example/v_sat_compass";

function w(rel, content) {
    const fp = path.join(base, rel);
    fs.mkdirSync(path.dirname(fp), { recursive: true });
    fs.writeFileSync(fp, content, "utf8");
    console.log("Created: " + rel);
}

// ========== VsatApp.java ==========
w("VsatApp.java", `package com.example.v_sat_compass;

import android.app.Application;

public class VsatApp extends Application {
    private static VsatApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static VsatApp getInstance() {
        return instance;
    }
}
`);

// ========== data/api/ApiClient.java ==========
w("data/api/ApiClient.java", `package com.example.v_sat_compass.data.api;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.v_sat_compass.VsatApp;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    // Doi thanh IP/domain cua backend
    private static final String BASE_URL = "http://10.0.2.2:8080/api/v1/";
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        String token = getAccessToken();
                        if (token != null) {
                            Request.Builder builder = original.newBuilder()
                                    .header("Authorization", "Bearer " + token);
                            return chain.proceed(builder.build());
                        }
                        return chain.proceed(original);
                    })
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    private static String getAccessToken() {
        SharedPreferences prefs = VsatApp.getInstance()
                .getSharedPreferences("vsat_prefs", Context.MODE_PRIVATE);
        return prefs.getString("access_token", null);
    }

    public static void saveTokens(String accessToken, String refreshToken) {
        SharedPreferences prefs = VsatApp.getInstance()
                .getSharedPreferences("vsat_prefs", Context.MODE_PRIVATE);
        prefs.edit()
                .putString("access_token", accessToken)
                .putString("refresh_token", refreshToken)
                .apply();
    }

    public static void clearTokens() {
        SharedPreferences prefs = VsatApp.getInstance()
                .getSharedPreferences("vsat_prefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        retrofit = null;
    }

    public static boolean isLoggedIn() {
        return getAccessToken() != null;
    }
}
`);

// ========== data/api/AuthApi.java ==========
w("data/api/AuthApi.java", `package com.example.v_sat_compass.data.api;

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
    Call<ApiResponse<Void>> logout();

    @GET("auth/me")
    Call<ApiResponse<UserProfile>> getMe();
}
`);

// ========== data/model/ApiResponse.java ==========
w("data/model/ApiResponse.java", `package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class ApiResponse<T> {
    @SerializedName("data")
    private T data;

    @SerializedName("message")
    private String message;

    @SerializedName("error")
    private ErrorInfo error;

    public T getData() { return data; }
    public String getMessage() { return message; }
    public ErrorInfo getError() { return error; }
    public boolean isSuccess() { return error == null; }

    public static class ErrorInfo {
        @SerializedName("code")
        private String code;
        @SerializedName("message")
        private String message;

        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
}
`);

// ========== data/model/LoginRequest.java ==========
w("data/model/LoginRequest.java", `package com.example.v_sat_compass.data.model;

public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
}
`);

// ========== data/model/RegisterRequest.java ==========
w("data/model/RegisterRequest.java", `package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    private String email;
    private String password;
    @SerializedName("full_name")
    private String fullName;

    public RegisterRequest(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }
}
`);

// ========== data/model/AuthResponse.java ==========
w("data/model/AuthResponse.java", `package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    @SerializedName("token_type")
    private String tokenType;

    private UserProfile user;

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public UserProfile getUser() { return user; }
}
`);

// ========== data/model/UserProfile.java ==========
w("data/model/UserProfile.java", `package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class UserProfile {
    private Long id;
    private String email;
    @SerializedName("full_name")
    private String fullName;
    private String phone;
    private String role;
    private String status;
    @SerializedName("avatar_url")
    private String avatarUrl;

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public String getAvatarUrl() { return avatarUrl; }
}
`);

// ========== data/repository/AuthRepository.java ==========
w("data/repository/AuthRepository.java", `package com.example.v_sat_compass.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.AuthApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.AuthResponse;
import com.example.v_sat_compass.data.model.LoginRequest;
import com.example.v_sat_compass.data.model.RegisterRequest;
import com.example.v_sat_compass.data.model.UserProfile;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final AuthApi authApi;

    public AuthRepository() {
        authApi = ApiClient.getClient().create(AuthApi.class);
    }

    public LiveData<Resource<AuthResponse>> login(String email, String password) {
        MutableLiveData<Resource<AuthResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        authApi.login(new LoginRequest(email, password)).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse auth = response.body().getData();
                    ApiClient.saveTokens(auth.getAccessToken(), auth.getRefreshToken());
                    result.setValue(Resource.success(auth));
                } else {
                    result.setValue(Resource.error("Dang nhap that bai"));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                result.setValue(Resource.error("Loi ket noi: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<AuthResponse>> register(String email, String password, String fullName) {
        MutableLiveData<Resource<AuthResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        authApi.register(new RegisterRequest(email, password, fullName)).enqueue(new Callback<ApiResponse<AuthResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuthResponse>> call, Response<ApiResponse<AuthResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    AuthResponse auth = response.body().getData();
                    ApiClient.saveTokens(auth.getAccessToken(), auth.getRefreshToken());
                    result.setValue(Resource.success(auth));
                } else {
                    result.setValue(Resource.error("Dang ky that bai"));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<AuthResponse>> call, Throwable t) {
                result.setValue(Resource.error("Loi ket noi: " + t.getMessage()));
            }
        });
        return result;
    }

    public LiveData<Resource<UserProfile>> getMe() {
        MutableLiveData<Resource<UserProfile>> result = new MutableLiveData<>();
        result.setValue(Resource.loading());
        authApi.getMe().enqueue(new Callback<ApiResponse<UserProfile>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfile>> call, Response<ApiResponse<UserProfile>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    result.setValue(Resource.success(response.body().getData()));
                } else {
                    result.setValue(Resource.error("Khong lay duoc thong tin"));
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<UserProfile>> call, Throwable t) {
                result.setValue(Resource.error("Loi ket noi: " + t.getMessage()));
            }
        });
        return result;
    }

    public void logout() {
        authApi.logout().enqueue(new Callback<ApiResponse<Void>>() {
            @Override public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {}
            @Override public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {}
        });
        ApiClient.clearTokens();
    }
}
`);

// ========== data/repository/Resource.java ==========
w("data/repository/Resource.java", `package com.example.v_sat_compass.data.repository;

public class Resource<T> {
    public enum Status { LOADING, SUCCESS, ERROR }

    private final Status status;
    private final T data;
    private final String message;

    private Resource(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static <T> Resource<T> loading() { return new Resource<>(Status.LOADING, null, null); }
    public static <T> Resource<T> success(T data) { return new Resource<>(Status.SUCCESS, data, null); }
    public static <T> Resource<T> error(String msg) { return new Resource<>(Status.ERROR, null, msg); }

    public Status getStatus() { return status; }
    public T getData() { return data; }
    public String getMessage() { return message; }
}
`);

// ========== ui/auth/AuthViewModel.java ==========
w("ui/auth/AuthViewModel.java", `package com.example.v_sat_compass.ui.auth;

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
`);

// ========== ui/auth/LoginActivity.java ==========
w("ui/auth/LoginActivity.java", `package com.example.v_sat_compass.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

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
        binding.tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }

    private void doLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui long dien day du thong tin", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Dang nhap thanh cong!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finishAffinity();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnLogin.setEnabled(true);
                    Toast.makeText(this, resource.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }
}
`);

// ========== ui/auth/RegisterActivity.java ==========
w("ui/auth/RegisterActivity.java", `package com.example.v_sat_compass.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.v_sat_compass.MainActivity;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnRegister.setOnClickListener(v -> doRegister());
        binding.tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void doRegister() {
        String fullName = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui long dien day du thong tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Mat khau khong khop", Toast.LENGTH_SHORT).show();
            return;
        }

        viewModel.register(email, password, fullName).observe(this, resource -> {
            switch (resource.getStatus()) {
                case LOADING:
                    binding.progressBar.setVisibility(View.VISIBLE);
                    binding.btnRegister.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Dang ky thanh cong!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finishAffinity();
                    break;
                case ERROR:
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRegister.setEnabled(true);
                    Toast.makeText(this, resource.getMessage(), Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }
}
`);

// ========== SplashActivity.java ==========
w("ui/SplashActivity.java", `package com.example.v_sat_compass.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.v_sat_compass.MainActivity;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.ui.auth.LoginActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (ApiClient.isLoggedIn()) {
                startActivity(new Intent(this, MainActivity.class));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        }, 1000);
    }
}
`);

console.log("\\n=== ALL ANDROID JAVA FILES CREATED ===");
