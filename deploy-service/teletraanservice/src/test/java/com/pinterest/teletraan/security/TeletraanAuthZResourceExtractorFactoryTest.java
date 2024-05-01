package com.pinterest.teletraan.security;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.teletraan.security.TeletraanAuthZResourceExtractorFactory.UnsupportedResourceInfoException;
import com.pinterest.teletraan.universal.security.AuthZResourceExtractor;
import com.pinterest.teletraan.universal.security.ResourceAuthZInfo;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;

class TeletraanAuthZResourceExtractorFactoryTest {
    private ServiceContext serviceContext;
    private ResourceAuthZInfo authZInfo;
    private TeletraanAuthZResourceExtractorFactory extractorFactory;

    @BeforeEach
    void setUp() {
        serviceContext = mock(ServiceContext.class);
        authZInfo = mock(ResourceAuthZInfo.class);
        extractorFactory = new TeletraanAuthZResourceExtractorFactory(serviceContext);
    }

    @Test
    void create_shouldReturnCorrectExtractorForPathLocation() {
        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.PATH);
        when(authZInfo.type()).thenReturn(AuthZResource.Type.BUILD);

        AuthZResourceExtractor extractor = extractorFactory.create(authZInfo);

        assertTrue(extractor instanceof BuildPathExtractor);
    }

    @Test
    void create_shouldReturnCorrectExtractorForBodyLocation() {
        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.BODY);
        when(authZInfo.type()).thenReturn(AuthZResource.Type.BUILD);

        AuthZResourceExtractor extractor = extractorFactory.create(authZInfo);

        assertTrue(extractor instanceof BuildBodyExtractor);
    }

    @Test
    void create_shouldReturnCorrectExtractorForEnvPathLocation() {
        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.PATH);
        when(authZInfo.type()).thenReturn(AuthZResource.Type.ENV);

        AuthZResourceExtractor extractor = extractorFactory.create(authZInfo);

        assertTrue(extractor instanceof EnvPathExtractor);
    }

    @Test
    void create_shouldReturnCorrectExtractorForEnvStageBodyLocation() {
        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.BODY);
        when(authZInfo.type()).thenReturn(AuthZResource.Type.ENV_STAGE);

        AuthZResourceExtractor extractor = extractorFactory.create(authZInfo);

        assertTrue(extractor instanceof EnvStageBodyExtractor);
    }

    @Test
    void create_shouldReturnCorrectExtractorForHotfixPathLocation() {
        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.PATH);
        when(authZInfo.type()).thenReturn(AuthZResource.Type.HOTFIX);

        AuthZResourceExtractor extractor = extractorFactory.create(authZInfo);

        assertTrue(extractor instanceof HotfixPathExtractor);
    }

    @Test
    void create_shouldReturnCorrectExtractorForHotfixBodyLocation() {
        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.BODY);
        when(authZInfo.type()).thenReturn(AuthZResource.Type.HOTFIX);

        AuthZResourceExtractor extractor = extractorFactory.create(authZInfo);

        assertTrue(extractor instanceof HotfixBodyExtractor);
    }

    @Test
    void create_shouldReturnCorrectExtractorForDeployPathLocation() {
        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.PATH);
        when(authZInfo.type()).thenReturn(AuthZResource.Type.DEPLOY);

        AuthZResourceExtractor extractor = extractorFactory.create(authZInfo);

        assertTrue(extractor instanceof DeployPathExtractor);
    }

    @Test
    void create_shouldReturnCorrectExtractorForEnvStagePathLocation() {
        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.PATH);
        when(authZInfo.type()).thenReturn(AuthZResource.Type.ENV_STAGE);

        AuthZResourceExtractor extractor = extractorFactory.create(authZInfo);

        assertTrue(extractor instanceof EnvStagePathExtractor);
    }

    @Test
    void create_shouldThrowExceptionForUnsupportedLocation() {
        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.NA);

        assertThrows(IllegalArgumentException.class, () -> extractorFactory.create(authZInfo));
    }

    @Test
    void create_shouldThrowExceptionForUnsupportedType() {
        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.BODY);
        when(authZInfo.type()).thenReturn(AuthZResource.Type.SYSTEM);

        assertThrows(UnsupportedResourceInfoException.class, () -> extractorFactory.create(authZInfo));

        when(authZInfo.idLocation()).thenReturn(ResourceAuthZInfo.Location.PATH);
        when(authZInfo.type()).thenReturn(AuthZResource.Type.SYSTEM);

        assertThrows(UnsupportedResourceInfoException.class, () -> extractorFactory.create(authZInfo));
    }
}