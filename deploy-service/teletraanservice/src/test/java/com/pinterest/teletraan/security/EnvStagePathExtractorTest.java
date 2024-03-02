package com.pinterest.teletraan.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor.ExtractionException;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

class EnvStagePathExtractorTest extends BasePathExtractorTest {
    private EnvStagePathExtractor sut;
    private static final String ENV_NAME_KEY = "envName";
    private static final String STAGE_NAME_KEY = "stageName";

    @BeforeEach
    void setUp() {
        super.setUp();
        sut = new EnvStagePathExtractor();
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
    void testExtractResource_onlyStageName() {
        pathParameters.add(STAGE_NAME_KEY, "testEnv");

        assertThrows(ExtractionException.class, () -> sut.extractResource(context));
    }

    @Test
    void testExtractResource_1EnvName() {
        pathParameters.add(ENV_NAME_KEY, "testEnv");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertTrue(resource.getName().contains("testEnv"));
        assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
    }

    @Test
    void testExtractResource_1EnvName1StageName() {
        pathParameters.add(ENV_NAME_KEY, "testEnv");
        pathParameters.add(STAGE_NAME_KEY, "testStage");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertTrue(resource.getName().contains("testEnv"));
        assertTrue(resource.getName().contains("testStage"));
        assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
    }

    @Test
    void testExtractResource_2EnvNames1StageName() {
        pathParameters.add(ENV_NAME_KEY, "testEnv");
        pathParameters.add(ENV_NAME_KEY, "testEnv2");
        pathParameters.add(STAGE_NAME_KEY, "testStage");

        AuthZResource resource = sut.extractResource(context);
        assertNotNull(resource);
        assertTrue(resource.getName().contains("testEnv"));
        assertTrue(resource.getName().contains("testStage"));
        assertEquals(AuthZResource.Type.ENV_STAGE, resource.getType());
    }
}
