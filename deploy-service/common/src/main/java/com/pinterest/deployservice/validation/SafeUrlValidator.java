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
package com.pinterest.deployservice.validation;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class SafeUrlValidator implements ConstraintValidator<SafeUrl, String> {

    @Override
    public boolean isValid(String url, ConstraintValidatorContext context) {
        if (url == null || url.isBlank()) {
            return true;
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return false;
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            return false;
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return false;
        }

        if (isInternalHost(host)) {
            return false;
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            return false;
        }

        for (InetAddress addr : addresses) {
            if (addr.isLoopbackAddress()
                    || addr.isSiteLocalAddress()
                    || addr.isLinkLocalAddress()
                    || addr.isAnyLocalAddress()) {
                return false;
            }
        }

        return true;
    }

    private boolean isInternalHost(String host) {
        String lower = host.toLowerCase();
        return lower.equals("localhost")
                || lower.endsWith(".local")
                || lower.endsWith(".internal")
                || lower.equals("metadata.google.internal")
                || lower.equals("[::1]");
    }
}
