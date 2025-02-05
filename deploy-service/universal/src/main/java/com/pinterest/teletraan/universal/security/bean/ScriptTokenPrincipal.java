/**
 * Copyright (c) 2016-2025 Pinterest, Inc.
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
package com.pinterest.teletraan.universal.security.bean;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** A principal represented by a script token. */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptTokenPrincipal<R extends Role<R>> extends ServicePrincipal {
    private final R role;
    private final AuthZResource resource;

    public ScriptTokenPrincipal(String name, R role, AuthZResource resource) {
        super(name);
        this.role = role;
        this.resource = resource;
    }
}
