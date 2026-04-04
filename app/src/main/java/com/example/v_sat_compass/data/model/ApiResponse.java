package com.example.v_sat_compass.data.model;

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
