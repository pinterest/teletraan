/**
 * Copyright (c) 2016-2017 Pinterest, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;

public class CommonUtilsTest {
    @Test
    public void testEncodeDecodeData() throws Exception {
        Map<String, String> data = new HashMap<String, String>();
        String script = "#!/bin/bash\necho \"Hello World!\"";
        data.put("simple", "How are you!");
        data.put("complicate", script);

        data = CommonUtils.encodeScript(data);

        String encodedScript1 = data.get("complicate");

        String encoded = CommonUtils.encodeData(data);
        Map<String, String> decoded = CommonUtils.decodeData(encoded);

        String encodedScript2 = decoded.get("complicate");

        decoded = CommonUtils.decodeScript(decoded);

        String decodedScript3 = decoded.get("complicate");

        assertEquals(decoded.get("simple"), "How are you!");
        assertEquals(decoded.get("complicate"), script);
    }

    @Test
    public void testURLIncludesDot() throws Exception {
        Pattern p = Pattern.compile("^[a-zA-Z0-9\\-_.]+$");
        Matcher m = p.matcher("username");
        assertTrue(m.find());
        m = p.matcher("username___000--valid");
        assertTrue(m.find());
        m = p.matcher("username_..d.000--valid");
        assertTrue(m.find());
        m = p.matcher("username***..d.000||invalid");
        assertFalse(m.find());
        m = p.matcher("***..d.000||invalid");
        assertFalse(m.find());
    }
}
