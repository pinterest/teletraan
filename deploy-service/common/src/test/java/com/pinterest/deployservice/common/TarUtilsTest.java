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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TarUtilsTest {

    @Test
    public void testTarUntar() throws Exception {
        Map<String, String> data = new HashMap<String, String>();
        data.put("foo", "This is foo!");
        data.put("bar", "This is bar!");
        byte[] bytes = TarUtils.tar(data);

        InputStream is = new ByteArrayInputStream(bytes);
        Map<String, String> result = TarUtils.untar(is);

        assertEquals(result.size(), 2);
        assertEquals(result.get("foo"), "This is foo!");
        assertEquals(result.get("bar"), "This is bar!");
    }
}
