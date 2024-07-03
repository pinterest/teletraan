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
package com.pinterest.teletraan.universal.security;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class Constants {
    public static final String USER_HEADER = "x-forwarded-user";
    public static final String GROUPS_HEADER = "x-forwarded-groups";
    public static final String CLIENT_CERT_HEADER = "x-forwarded-client-cert";
    public static final String AUTHZ_ATTR_REQ_CXT_KEY = "AuthZAttributes";
}
