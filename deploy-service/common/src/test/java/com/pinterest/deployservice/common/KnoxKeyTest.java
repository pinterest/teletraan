/**
 * Copyright (c) 2022-2023 Pinterest, Inc.
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
package com.pinterest.deployservice.common;

import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.deployservice.rodimus.RodimusManagerImpl;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

@SuppressWarnings("unchecked")
class KnoxKeyTest {

    private static enum Answer {
        NULL,
        EXCEPTION,
        ARRAY,
        LATENCY
    }

    private static final String msgUnauthException =
            "HTTP request failed, status = 401, content = Unauthorized";
    private static final String postAnswerTag =
            "{\"i-001\":{\"Name\": \"devapp-example1\"},\"i-002\":{\"Name\": \"devrestricted-example2\"}}";
    private static final String postAnswerArray = "[\"i-001\",\"i-002\"]";

    private RodimusManager rodimusManager = null;
    private KnoxKeyReader mockKnoxKeyReader;
    private List<Answer> answerList;
    private String[] testKey = new String[2];

    @BeforeEach
    void setUp() throws Exception {
        // Load testKeys
        testKey[0] = "aaa"; // auth error
        testKey[1] = "bbb"; // auth ok

        // Create mock for Knox
        mockKnoxKeyReader = Mockito.mock(KnoxKeyReader.class);

        // Create mock for httpClient

        rodimusManager =
                new RodimusManagerImpl("http://localhost", "teletraan:test", false, "", "");

        // Allocate answerList
        answerList = new ArrayList<Answer>();
        mockClasses(rodimusManager, mockKnoxKeyReader);
    }

    // ### HELPER METHODS ###

    private void mockClasses(RodimusManager rodimusMngr, KnoxKeyReader mokKnox) throws Exception {
        // Modify fsKnox to use our mock
        Field classKnox = rodimusMngr.getClass().getDeclaredField("knoxKeyReader");
        classKnox.setAccessible(true);
        classKnox.set(rodimusMngr, mokKnox);
        classKnox.setAccessible(false);
    }
}
