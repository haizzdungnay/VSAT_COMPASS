package com.example.v_sat_compass.data.api;

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

    // ===== CẤU HÌNH URL BACKEND =====
    // Sau khi deploy lên Render, thay URL bên dưới bằng URL Render của bạn
    // Ví dụ: "https://vsat-compass-api.onrender.com/api/v1/"
    // Render free tier sẽ sleep sau 15 phút không dùng, request đầu tiên mất ~30-60s
    private static final String BASE_URL_CLOUD = "https://vsat-compass-api.onrender.com/api/v1/";
    // Dùng IP LAN khi chạy backend local (chỉ dùng khi dev)
    private static final String BASE_URL_LOCAL = "http://192.168.100.8:8080/api/v1/";

    // >>> CHUYỂN GIỮA CLOUD VÀ LOCAL TẠI ĐÂY <<<
    private static final String BASE_URL = BASE_URL_CLOUD;

    // === CHẾ ĐỘ XỬ LÝ ĐỀ THI ===
    // true  → Timer + chấm điểm chạy trực tiếp trên thiết bị (KHÔNG gửi từng đáp án lên server).
    //         App vẫn cần mạng để đăng nhập, kiểm tra quyền mua đề, và lấy dữ liệu đề thi.
    //         Chỉ POST kết quả cuối cùng {score, correct, total, timeSpent} lên DB.
    // false → Server xử lý toàn bộ (nhận từng đáp án, tự chấm điểm).
    private static final boolean CLIENT_SIDE_EXAM_PROCESSING = true;

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
