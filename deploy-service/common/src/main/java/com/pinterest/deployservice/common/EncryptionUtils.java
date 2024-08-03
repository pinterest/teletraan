/**
 * Copyright (c) 2022 Pinterest, Inc.
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

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/*
 * Utility class for loading certificates and keys. Based on:
 * https://github.com/Mastercard/client-encryption-java/blob/main/src/main/java/com/mastercard/developer/utils/EncryptionUtils.java
 */
public final class EncryptionUtils {

    private static final String PKCS_1_PEM_HEADER = "-----BEGIN RSA PRIVATE KEY-----";
    private static final String PKCS_1_PEM_FOOTER = "-----END RSA PRIVATE KEY-----";
    private static final String PKCS_8_PEM_HEADER = "-----BEGIN PRIVATE KEY-----";
    private static final String PKCS_8_PEM_FOOTER = "-----END PRIVATE KEY-----";

    private EncryptionUtils() {}

    public static String createGithubJWT(
            String githubAppId, String privateKeyPEMString, long ttlMillis) throws Exception {
        // The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;

        // We will sign our JWT with our private key
        Key signingKey = EncryptionUtils.loadDecryptionKey(privateKeyPEMString);

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        // Let's set the JWT Claims
        JwtBuilder builder =
                Jwts.builder()
                        .setIssuedAt(now)
                        .setIssuer(githubAppId)
                        .signWith(signingKey, signatureAlgorithm);

        // if it has been specified, let's add the expiration
        if (ttlMillis > 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }

        // Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();
    }

    /** Load a RSA decryption key from a file (PEM or DER). */
    public static PrivateKey loadDecryptionKey(String keyString)
            throws GeneralSecurityException, IOException {
        if (keyString.contains(PKCS_1_PEM_HEADER)) {
            // OpenSSL / PKCS#1 Base64 PEM encoded file
            keyString = keyString.replace(PKCS_1_PEM_HEADER, "");
            keyString = keyString.replace(PKCS_1_PEM_FOOTER, "");
            keyString = keyString.replace("\n", "");
            keyString = keyString.replace("\r\n", "");
            return readPkcs1PrivateKey(Base64.getDecoder().decode(keyString));
        } else if (keyString.contains(PKCS_8_PEM_HEADER)) {
            // PKCS#8 Base64 PEM encoded file
            keyString = keyString.replace(PKCS_8_PEM_HEADER, "");
            keyString = keyString.replace(PKCS_8_PEM_FOOTER, "");
            keyString = keyString.replace("\n", "");
            keyString = keyString.replace("\r\n", "");
            return readPkcs8PrivateKey(Base64.getDecoder().decode(keyString));
        } else {
            throw new IllegalArgumentException("Unexpected key format!");
        }
    }

    /** Create a PrivateKey instance from raw PKCS#8 bytes. */
    private static PrivateKey readPkcs8PrivateKey(byte[] pkcs8Bytes)
            throws GeneralSecurityException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8Bytes);
        try {
            return keyFactory.generatePrivate(keySpec);
        } catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Unexpected key format!", e);
        }
    }

    /** Create a PrivateKey instance from raw PKCS#1 bytes. */
    private static PrivateKey readPkcs1PrivateKey(byte[] pkcs1Bytes)
            throws GeneralSecurityException {
        // We can't use Java internal APIs to parse ASN.1 structures, so we build a PKCS#8 key Java
        // can understand
        int pkcs1Length = pkcs1Bytes.length;
        int totalLength = pkcs1Length + 22;
        // spotless:off
        byte[] pkcs8Header = new byte[] {
                0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff), // Sequence + total length
                0x2, 0x1, 0x0, // Integer (0)
                0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1, 0x1, 0x5, 0x0, // Sequence: 1.2.840.113549.1.1.1, NULL
                0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff) // Octet string + length
        };
        // spotless:on
        byte[] pkcs8bytes = join(pkcs8Header, pkcs1Bytes);
        return readPkcs8PrivateKey(pkcs8bytes);
    }

    private static byte[] join(byte[] byteArray1, byte[] byteArray2) {
        byte[] bytes = new byte[byteArray1.length + byteArray2.length];
        System.arraycopy(byteArray1, 0, bytes, 0, byteArray1.length);
        System.arraycopy(byteArray2, 0, bytes, byteArray1.length, byteArray2.length);
        return bytes;
    }
}
