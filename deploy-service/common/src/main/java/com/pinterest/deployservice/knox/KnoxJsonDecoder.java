package com.pinterest.deployservice.knox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * KnoxJSONDecoder decodes a knox Key in it
 */
public class KnoxJsonDecoder {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * getPrimaryKey returns the primary key for the specified keyID
     */
    public static byte[] getPrimaryKey(String json) throws Exception {
        JsonNode key = mapper.readTree(json);
        return getPrimaryKeyHelper(key);
    }

    public static byte[] getPrimaryKey(InputStream json) throws Exception {
        JsonNode key = mapper.readTree(json);
        return getPrimaryKeyHelper(key);
    }

    public static byte[] getPrimaryKey(File json) throws Exception {
        JsonNode key = mapper.readTree(json);
        return getPrimaryKeyHelper(key);
    }

    private static byte[] getPrimaryKeyHelper(JsonNode key) throws Exception {
        for (JsonNode version : key.get("versions")) {
            if (version.get("status").asText().equals("Primary")) {
                // This is the only primary key; base64 decode it and return
                return version.get("data").binaryValue();
            }
        }
        throw new IllegalStateException("Invalid Knox key: No primary version in JSON.");
    }

    /**
     * getActiveKeys returns a list of active keys for the specified keyID
     */
    public static byte[][] getActiveKeys(String json) throws Exception {
        JsonNode key = mapper.readTree(json);
        return getActiveKeysHelper(key);
    }

    public static byte[][] getActiveKeys(InputStream json) throws Exception {
        JsonNode key = mapper.readTree(json);
        return getActiveKeysHelper(key);
    }

    public static byte[][] getActiveKeys(File json) throws Exception {
        JsonNode key = mapper.readTree(json);
        return getActiveKeysHelper(key);
    }

    public static byte[][] getActiveKeysHelper(JsonNode key) throws Exception {
        List<byte[]> results = new ArrayList<byte[]>();
        for (JsonNode version : key.get("versions")) {
            String status = version.get("status").textValue();
            if (status.equals("Primary") || status.equals("Active")) {
                results.add(version.get("data").binaryValue());
            }
        }
        return results.toArray(new byte[][]{});
    }

}
