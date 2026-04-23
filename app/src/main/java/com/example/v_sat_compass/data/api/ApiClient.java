package com.example.v_sat_compass.data.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.example.v_sat_compass.BuildConfig;
import com.example.v_sat_compass.VsatApp;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String TAG = "ApiClient";

    // Debug -> local backend, Release -> cloud backend.
    // Local host is chosen at runtime:
    // - Emulator: 10.0.2.2
    // - Physical device: LOCAL_LAN_HOST from BuildConfig
    private static final String EMULATOR_LOCAL_HOST = "10.0.2.2";

    // === CHẾ ĐỘ XỬ LÝ ĐỀ THI ===
    // true  → Timer + chấm điểm chạy trực tiếp trên thiết bị (KHÔNG gửi từng đáp án lên server).
    //         App vẫn cần mạng để đăng nhập, kiểm tra quyền mua đề, và lấy dữ liệu đề thi.
    //         Chỉ POST kết quả cuối cùng {score, correct, total, timeSpent} lên DB.
    // false → Server xử lý toàn bộ (nhận từng đáp án, tự chấm điểm).
    private static final boolean CLIENT_SIDE_EXAM_PROCESSING = true;

    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            String baseUrl = resolveBaseUrl();
            Log.i(TAG, "Using backend URL: " + baseUrl);

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
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static String getCurrentBaseUrl() {
        return resolveBaseUrl();
    }

    private static String resolveBaseUrl() {
        if (!BuildConfig.USE_LOCAL_BACKEND) {
            return BuildConfig.BASE_URL_CLOUD;
        }

        String host = isProbablyRunningOnEmulator() ? EMULATOR_LOCAL_HOST : BuildConfig.LOCAL_LAN_HOST;
        return "http://" + host + ":8080/api/v1/";
    }

    private static boolean isProbablyRunningOnEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic")
                || Build.DEVICE.startsWith("generic")
                || "google_sdk".equals(Build.PRODUCT);
    }

    private static String getAccessToken() {
        SharedPreferences prefs = VsatApp.getInstance()
                .getSharedPreferences("vsat_prefs", Context.MODE_PRIVATE);
        return prefs.getString("access_token", null);
    }

    public static String getRefreshToken() {
        SharedPreferences prefs = VsatApp.getInstance()
                .getSharedPreferences("vsat_prefs", Context.MODE_PRIVATE);
        return prefs.getString("refresh_token", null);
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

    public static boolean isClientSideExamProcessingEnabled() {
        return CLIENT_SIDE_EXAM_PROCESSING;
    }
}
