package com.pinterest.deployservice.bean;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;


public class EnvironBeanTest {
    static private ObjectMapper objectMapper;

    @BeforeClass
    public static void beforeAll() {
        objectMapper = new ObjectMapper();
    }

    @Test
    public void whenMappingJsonToBean_shouldUpdatePrivateBuild() throws Exception {
        String jsonContent = "{\"allowPrivateBuild\": \"false\",  \"stageType\": \"DEV\"}";
        EnvironBean bean = objectMapper.readValue(jsonContent, EnvironBean.class);
        assertEquals(bean.getStage_type(), EnvType.DEV);
        assertTrue(bean.getAllow_private_build());
    }

}
