package com.example.v_sat_compass.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Map;

public final class AvatarPreferences {

    private static final String PROFILE_PREFS = "profile_local_overrides";
    private static final String LEGACY_KEY_AVATAR_URI = "avatar_uri";
    private static final String KEY_PREFIX_USER_ID = "avatar_uri_user_id_";
    private static final String KEY_PREFIX_USER_EMAIL = "avatar_uri_user_email_";
    private static final String KEY_ANONYMOUS = "avatar_uri_user_anonymous";

    private AvatarPreferences() {}

    public static void saveCurrentUserAvatar(Context context, String avatarUri) {
        prefs(context).edit()
                .putString(currentUserAvatarKey(), avatarUri != null ? avatarUri : "")
                .remove(LEGACY_KEY_AVATAR_URI)
                .apply();
    }

    public static String getCurrentUserAvatar(Context context) {
        SharedPreferences prefs = prefs(context);
        String key = currentUserAvatarKey();
        String avatarUri = prefs.getString(key, "");
        if (avatarUri != null && !avatarUri.isEmpty()) return avatarUri;

        String migrated = findAvatarForCurrentUserId(prefs, key);
        if (!migrated.isEmpty()) {
            prefs.edit().putString(key, migrated).apply();
            return migrated;
        }

        String legacyAvatarUri = prefs.getString(LEGACY_KEY_AVATAR_URI, "");
        if (legacyAvatarUri != null && !legacyAvatarUri.isEmpty()) {
            prefs.edit()
                    .putString(key, legacyAvatarUri)
                    .remove(LEGACY_KEY_AVATAR_URI)
                    .apply();
            return legacyAvatarUri;
        }

        return "";
    }

    public static String buildAvatarKey(long userId, String email) {
        String normalizedEmail = normalizeEmail(email);
        String emailHash = normalizedEmail.isEmpty() ? "no_email" : sha256Prefix(normalizedEmail);
        if (userId > 0) return KEY_PREFIX_USER_ID + userId + "_email_" + emailHash;
        if (!normalizedEmail.isEmpty()) return KEY_PREFIX_USER_EMAIL + emailHash;
        return KEY_ANONYMOUS;
    }

    private static String currentUserAvatarKey() {
        return buildAvatarKey(UserRoleHelper.getUserId(), UserRoleHelper.getEmail());
    }

    private static String findAvatarForCurrentUserId(SharedPreferences prefs, String currentKey) {
        long userId = UserRoleHelper.getUserId();
        if (userId <= 0) return "";

        String prefix = KEY_PREFIX_USER_ID + userId + "_email_";
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (currentKey.equals(entry.getKey()) || !entry.getKey().startsWith(prefix)) continue;
            Object value = entry.getValue();
            if (value instanceof String && !((String) value).isEmpty()) return (String) value;
        }
        return "";
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.US);
    }

    private static String sha256Prefix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                builder.append(String.format(Locale.US, "%02x", hash[i]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PROFILE_PREFS, Context.MODE_PRIVATE);
    }
}
