package com.pinterest.teletraan.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;

abstract class BasePathExtractorTest {
    protected ContainerRequestContext context;
    protected MultivaluedMap<String, String> pathParameters;

    @BeforeEach
    void setUp() {
        UriInfo uriInfo = mock(UriInfo.class);

        context = mock(ContainerRequestContext.class);
        pathParameters = new MultivaluedHashMap<>();

        when(context.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPathParameters()).thenReturn(pathParameters);
    }
}
