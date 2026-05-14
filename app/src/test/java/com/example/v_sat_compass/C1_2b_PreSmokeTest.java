package com.example.v_sat_compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;

import okhttp3.mockwebserver.MockWebServer;

import org.junit.Rule;
import org.junit.Test;

public class C1_2b_PreSmokeTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void junitBaselineSanity() {
        assertEquals(2, 1 + 1);
    }

    @Test
    public void mockitoAvailable() {
        Runnable runnable = mock(Runnable.class);

        runnable.run();

        verify(runnable).run();
    }

    @Test
    public void mockWebServerAvailable() throws Exception {
        MockWebServer server = new MockWebServer();

        try {
            server.start();
            assertNotNull(server.url("/"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void archCoreTestingAvailable() {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();

        liveData.setValue(42);

        assertEquals(Integer.valueOf(42), liveData.getValue());
    }
}
