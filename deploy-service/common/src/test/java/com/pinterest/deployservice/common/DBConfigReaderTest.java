/**
 * Copyright 2016 Pinterest, Inc.
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DBConfigReaderTest {
    @Test
    public void testParse() throws Exception {
        String credJson = "{" +
            "    \"scriptrw\": {\n"
            + "        \"users\": [\n"
            + "            {\n"
            + "                \"enabled\": true,\n"
            + "                \"password\": \"12345\",\n"
            + "                \"username\": \"scriptrw\"\n"
            + "            },\n"
            + "            {\n"
            + "                \"enabled\": false,\n"
            + "                \"password\": \"67890\",\n"
            + "                \"username\": \"scriptrw2\"\n"
            + "            }\n"
            + "        ]\n"
            + "    }" +
            "}";
        String dbJson = "{" +
            "    \"admindb\": {\n"
            + "        \"master\": {\n"
            + "            \"host\": \"admindb001c\", \n"
            + "            \"port\": 3306\n"
            + "        }, \n"
            + "        \"slave\": {\n"
            + "            \"host\": \"admindb001d\", \n"
            + "            \"port\": 3306\n"
            + "        }\n"
            + "    }" +
            "}";
        DBConfigReader reader = new DBConfigReader(dbJson, credJson);
        assertEquals("admindb001c", reader.getHost("admindb"));
        assertEquals(new Integer(3306), reader.getPort("admindb"));
        assertEquals("scriptrw", reader.getUsername("scriptrw"));
        assertEquals("12345", reader.getPassword("scriptrw"));
    }
}
