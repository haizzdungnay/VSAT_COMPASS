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
}
