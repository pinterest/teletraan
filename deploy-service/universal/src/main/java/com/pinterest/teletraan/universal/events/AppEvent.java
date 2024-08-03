/**
 * Copyright (c) 2023 Pinterest, Inc.
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
package com.pinterest.teletraan.universal.events;

import java.util.EventObject;
import lombok.Getter;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public abstract class AppEvent extends EventObject {
    @Getter private final long timestamp;

    protected AppEvent(Object source) {
        super(source);
        timestamp = System.currentTimeMillis();
    }

    protected AppEvent(Object source, long timestamp) {
        super(source);
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
