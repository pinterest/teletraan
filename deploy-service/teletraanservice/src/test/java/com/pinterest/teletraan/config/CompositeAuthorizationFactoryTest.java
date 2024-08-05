/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
package com.pinterest.teletraan.config;

import static org.junit.Assert.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.TeletraanAuthZResourceExtractorFactory;
import com.pinterest.teletraan.universal.security.BasePastisAuthorizer;
import io.dropwizard.auth.Authorizer;
import org.junit.jupiter.api.Test;

class CompositeAuthorizationFactoryTest {
    @Test
    void testCreate() throws Exception {
        TeletraanServiceContext context = new TeletraanServiceContext();
        context.setAuthZResourceExtractorFactory(
                new TeletraanAuthZResourceExtractorFactory(context));
        CompositeAuthorizationFactory factory = new CompositeAuthorizationFactory();

        Authorizer<?> authorizer = factory.create(context);
        assertNotNull(authorizer);
        assertTrue(authorizer instanceof BasePastisAuthorizer);

        Authorizer<?> authorizer2 = factory.create(context);
        assertSame(authorizer, authorizer2);
    }
}
