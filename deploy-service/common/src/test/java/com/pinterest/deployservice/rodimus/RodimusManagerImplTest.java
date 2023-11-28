package com.pinterest.deployservice.rodimus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.pinterest.deployservice.common.HTTPClient;
import com.pinterest.deployservice.rodimus.RodimusManagerImpl.Verb;

public class RodimusManagerImplTest {
    private RodimusManagerImpl sut;
    private HTTPClient mockHttpClient;
    private String TEST_URL = "testUrl";

    @Before
    public void setUp() throws Exception {
        sut = new RodimusManagerImpl("http://localhost", null, false, "", "");

        mockHttpClient = Mockito.mock(HTTPClient.class);
        Field classHttpClient = sut.getClass().getDeclaredField("httpClient");
        classHttpClient.setAccessible(true);
        classHttpClient.set(sut, mockHttpClient);
        classHttpClient.setAccessible(false);
    }

    @Test
    public void nullKnoxKey_defaultKeyIsUsed() throws Exception {
        sut.callHttpClient(Verb.DELETE, TEST_URL, null);

        ArgumentCaptor<Map<String, String>> argument = ArgumentCaptor
                .forClass((Class<Map<String, String>>) (Class) Map.class);
        verify(mockHttpClient).delete(eq(TEST_URL), eq(null), argument.capture(), eq(3));

        Map<String, String> headers = argument.getValue();
        assertTrue(headers.containsKey("Authorization"));
        assertEquals("token defaultKeyContent", headers.get("Authorization"));
    }

    @Test
    public void invalidKnoxKey_exceptionThrown() throws Exception {
        RodimusManagerImpl sut = new RodimusManagerImpl("http://localhost", "invalidRodimusKnoxKey", false, "", "");
        assertThrows(IllegalStateException.class, () -> sut.callHttpClient(Verb.DELETE, TEST_URL, null));
    }
}
