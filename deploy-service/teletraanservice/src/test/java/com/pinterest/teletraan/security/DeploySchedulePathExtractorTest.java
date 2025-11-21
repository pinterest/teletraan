package com.pinterest.teletraan.security;

import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeploySchedulePathExtractorTest extends BasePathExtractorTest {
  private DeploySchedulePathExtractor sut;
  private static final String ENV_NAME_KEY = "envName";
  private static final String STAGE_NAME_KEY = "stageName";

  @BeforeEach
  @Override
  void setUp() {
    super.setUp();
    sut = new DeploySchedulePathExtractor();
  }

  @Test
  void testExtractResource_noPathParams_exception() {
    assertThrows(AuthZResourceExtractor.ExtractionException.class, () -> sut.extractResource(context));
  }

  @Test
  void testExtractResource_0EnvName() {
    pathParameters.add("nonEnvName", "testEnv");

    assertThrows(AuthZResourceExtractor.ExtractionException.class, () -> sut.extractResource(context));
  }

  @Test
  void testExtractResource_onlyStageName() {
    pathParameters.add(STAGE_NAME_KEY, "testEnv");

    assertThrows(AuthZResourceExtractor.ExtractionException.class, () -> sut.extractResource(context));
  }

  @Test
  void testExtractResource_1EnvName() throws AuthZResourceExtractor.ExtractionException {
    pathParameters.add(ENV_NAME_KEY, "testEnv");

    AuthZResource resource = sut.extractResource(context);
    assertNotNull(resource);
    assertTrue(resource.getName().contains("testEnv"));
    assertEquals(AuthZResource.Type.DEPLOY_SCHEDULE, resource.getType());
  }

  @Test
  void testExtractResource_1EnvName1StageName() throws AuthZResourceExtractor.ExtractionException {
    pathParameters.add(ENV_NAME_KEY, "testEnv");
    pathParameters.add(STAGE_NAME_KEY, "testStage");

    AuthZResource resource = sut.extractResource(context);
    assertNotNull(resource);
    assertEquals(AuthZResource.Type.DEPLOY_SCHEDULE, resource.getType());
    assertEquals("testEnv/testStage", resource.getName());
  }

  @Test
  void testExtractResource_2EnvNames1StageName() throws AuthZResourceExtractor.ExtractionException {
    pathParameters.add(ENV_NAME_KEY, "testEnv");
    pathParameters.add(ENV_NAME_KEY, "testEnv2");
    pathParameters.add(STAGE_NAME_KEY, "testStage");

    AuthZResource resource = sut.extractResource(context);
    assertNotNull(resource);
    assertEquals("testEnv/testStage", resource.getName());
    assertEquals(AuthZResource.Type.DEPLOY_SCHEDULE, resource.getType());
  }

}
