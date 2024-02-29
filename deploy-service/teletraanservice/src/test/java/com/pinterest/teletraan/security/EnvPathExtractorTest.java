package com.pinterest.teletraan.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

class EnvPathExtractorTest extends BasePathExtractorTest {
    private EnvPathExtractor sut;

    @BeforeEach
    void setUp() {
        super.setUp();
        sut = new EnvPathExtractor();
    }

    @Test
    void testExtractResource_noPathParams_exception() {
        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_0EnvName() {
        pathParameters.add("nonEnvName", "testEnv");

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_1EnvName() {
        pathParameters.add("envName", "testEnv");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertEquals("testEnv", resource.getName());
        assertEquals(AuthZResource.Type.ENV, resource.getType());
    }

    @Test
    void testExtractResource_2EnvNames() {
        pathParameters.add("envName", "testEnv");
        pathParameters.add("envName", "testEnv2");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertEquals("testEnv", resource.getName());
        assertEquals(AuthZResource.Type.ENV, resource.getType());
    }
}
