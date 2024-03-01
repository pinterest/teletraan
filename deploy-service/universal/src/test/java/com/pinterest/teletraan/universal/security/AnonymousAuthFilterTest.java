/**
 * Copyright (c) 2024 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.teletraan.universal.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AnonymousAuthFilterTest {
    @Test
    void testFilter() throws IOException {
        AnonymousAuthFilter sut = new AnonymousAuthFilter();
        ContainerRequestContext containerRequestContext = mock(ContainerRequestContext.class);
        sut.filter(containerRequestContext);

        ArgumentCaptor<SecurityContext> securityContextCaptor =
                ArgumentCaptor.forClass(SecurityContext.class);
        verify(containerRequestContext).setSecurityContext(securityContextCaptor.capture());
        SecurityContext securityContext = securityContextCaptor.getValue();

        assertEquals(AnonymousAuthFilter.USER, securityContext.getUserPrincipal());
        assertEquals("Anonymous", securityContext.getAuthenticationScheme());
        assertEquals(false, securityContext.isSecure());
        assertEquals(true, securityContext.isUserInRole("any"));
    }
}
