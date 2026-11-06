package com.example.v_sat_compass.util;

import com.example.v_sat_compass.data.repository.OfflineExamRepository;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import static org.junit.Assert.*;

public class AudioPlayLimiterTest {

    private AudioPlayLimiter limiter;

    @Before
    public void setUp() {
        limiter = new AudioPlayLimiter(2);
    }

    @Test
    public void testDefaultMaxPlaysIsTwo() {
        assertEquals(2, limiter.getMaxPlays(101L));
        assertEquals(2, limiter.getRemainingPlays(101L));
        assertTrue(limiter.canPlay(101L));
    }

    @Test
    public void testPlayCountTrackingAndExhaustion() {
        long groupId = 202L;

        // Play 1
        assertTrue(limiter.canPlay(groupId));
        boolean play1Success = limiter.recordPlay(groupId);
        assertTrue(play1Success);
        assertEquals(1, limiter.getPlayCount(groupId));
        assertEquals(1, limiter.getRemainingPlays(groupId));

        // Play 2
        assertTrue(limiter.canPlay(groupId));
        boolean play2Success = limiter.recordPlay(groupId);
        assertTrue(play2Success);
        assertEquals(2, limiter.getPlayCount(groupId));
        assertEquals(0, limiter.getRemainingPlays(groupId));

        // Play 3: exhausted
        assertFalse(limiter.canPlay(groupId));
        boolean play3Success = limiter.recordPlay(groupId);
        assertFalse(play3Success);
        assertEquals(2, limiter.getPlayCount(groupId));
        assertEquals(0, limiter.getRemainingPlays(groupId));
    }

    @Test
    public void testCustomMaxPlaysPerGroup() {
        long group1 = 1L;
        long group2 = 2L;

        limiter.setGroupMaxPlays(group1, 1);
        limiter.setGroupMaxPlays(group2, 3);

        assertEquals(1, limiter.getRemainingPlays(group1));
        assertEquals(3, limiter.getRemainingPlays(group2));

        assertTrue(limiter.recordPlay(group1));
        assertFalse(limiter.canPlay(group1));

        assertTrue(limiter.recordPlay(group2));
        assertTrue(limiter.recordPlay(group2));
        assertTrue(limiter.canPlay(group2));
        assertTrue(limiter.recordPlay(group2));
        assertFalse(limiter.canPlay(group2));
    }

    @Test
    public void testSeekingIsBlocked() {
        // Any seek position change must be disallowed
        assertFalse(limiter.validateSeekPosition(1000, 5000));
        assertFalse(limiter.validateSeekPosition(5000, 1000));
        assertFalse(limiter.validateSeekPosition(0, 10000));
    }

    @Test
    public void testResetClearsCounts() {
        long groupId = 99L;
        limiter.recordPlay(groupId);
        limiter.recordPlay(groupId);
        assertFalse(limiter.canPlay(groupId));

        limiter.reset();
        assertEquals(2, limiter.getRemainingPlays(groupId));
        assertTrue(limiter.canPlay(groupId));
    }

    @Test
    public void testOfflineMediaEncryptionAndDecryptionAesGcm() throws IOException, GeneralSecurityException {
        byte[] originalMedia = "MOCK_AUDIO_MP3_STREAM_DATA_FOR_LISTENING_SECTION".getBytes(StandardCharsets.UTF_8);

        // Encrypt with VSAT\x02 AES-128-GCM
        byte[] encrypted = OfflineExamRepository.encryptMediaBytes(originalMedia);
        assertNotNull(encrypted);

        // Magic header check: 'V', 'S', 'A', 'T', 0x02
        byte[] expectedMagic = new byte[]{'V', 'S', 'A', 'T', 0x02};
        byte[] actualMagic = Arrays.copyOfRange(encrypted, 0, 5);
        assertArrayEquals(expectedMagic, actualMagic);

        // Length check: magic (5) + IV (12) + ciphertext (originalMedia.length + 16 auth tag)
        assertEquals(5 + 12 + originalMedia.length + 16, encrypted.length);

        // Decrypt
        byte[] decrypted = OfflineExamRepository.decryptMediaBytes(encrypted);
        assertArrayEquals(originalMedia, decrypted);
    }

    @Test(expected = IOException.class)
    public void testDecryptMedia_InvalidMagicHeaderThrows() throws IOException, GeneralSecurityException {
        byte[] corrupted = new byte[]{ 'B', 'A', 'D', '!', 0x00, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34 };
        OfflineExamRepository.decryptMediaBytes(corrupted);
    }
}
