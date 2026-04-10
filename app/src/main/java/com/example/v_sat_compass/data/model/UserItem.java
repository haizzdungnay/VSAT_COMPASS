package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class UserItem {

    private Long id;

    @SerializedName("full_name")
    private String fullName;

    private String email;
    private String phone;
    private String role;
    private String status;

    @SerializedName("avatar_url")
    private String avatarUrl;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("last_login_at")
    private String lastLoginAt;

    public Long getId()            { return id; }
    public String getFullName()    { return fullName; }
    public String getEmail()       { return email; }
    public String getPhone()       { return phone; }
    public String getRole()        { return role; }
    public String getStatus()      { return status; }
    public String getAvatarUrl()   { return avatarUrl; }
    public String getCreatedAt()   { return createdAt; }
    public String getLastLoginAt() { return lastLoginAt; }

    public void setRole(String role)     { this.role = role; }
    public void setStatus(String status) { this.status = status; }

    public String getRoleDisplayName() {
        return com.example.v_sat_compass.util.UserRoleHelper.getRoleDisplayName(role);
    }
}
