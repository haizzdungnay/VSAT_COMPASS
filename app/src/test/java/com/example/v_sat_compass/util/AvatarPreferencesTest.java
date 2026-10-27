package com.example.v_sat_compass.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class AvatarPreferencesTest {

    @Test
    public void buildAvatarKey_sameOfflineIdDifferentEmail_returnsDifferentKeys() {
        String firstUserKey = AvatarPreferences.buildAvatarKey(1L, "first@example.test");
        String secondUserKey = AvatarPreferences.buildAvatarKey(1L, "second@example.test");

        assertNotEquals(firstUserKey, secondUserKey);
    }

    @Test
    public void buildAvatarKey_sameEmailWithWhitespaceAndCase_returnsSameKey() {
        String originalKey = AvatarPreferences.buildAvatarKey(7L, "  User@Example.Test ");
        String normalizedKey = AvatarPreferences.buildAvatarKey(7L, "user@example.test");

        assertEquals(originalKey, normalizedKey);
    }

    @Test
    public void buildAvatarKey_sameEmailDifferentUserId_returnsDifferentKeys() {
        String firstUserKey = AvatarPreferences.buildAvatarKey(10L, "user@example.test");
        String secondUserKey = AvatarPreferences.buildAvatarKey(11L, "user@example.test");

        assertNotEquals(firstUserKey, secondUserKey);
    }
}
