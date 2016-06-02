package com.pinterest.arcee.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public enum PasState {
    ENABLED,
    DISABLED;

    private static Map<String, PasState> stateMap = new HashMap<>(2);

    static {
        stateMap.put("enabled", ENABLED);
        stateMap.put("disabled", DISABLED);
    }

    @JsonCreator
    public static PasState forValue(String value) {
        return stateMap.get(StringUtils.lowerCase(value));
    }

    @JsonValue
    public String toValue() {
        for (Map.Entry<String, PasState> entry : stateMap.entrySet()) {
            if (entry.getValue() == this)
                return entry.getKey();
        }

        return null; // or fail
    }
}
