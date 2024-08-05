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
package com.pinterest.teletraan.universal.metrics.micrometer;

import static io.micrometer.core.instrument.config.MeterRegistryConfigValidator.checkAll;
import static io.micrometer.core.instrument.config.MeterRegistryConfigValidator.checkRequired;
import static io.micrometer.core.instrument.config.validate.PropertyValidator.getString;
import static io.micrometer.core.instrument.config.validate.PropertyValidator.getUriString;

import io.micrometer.core.instrument.config.validate.Validated;
import io.micrometer.core.instrument.push.PushRegistryConfig;
import io.micrometer.opentsdb.OpenTSDBConfig;
import java.net.URI;

public interface PinStatsConfig extends OpenTSDBConfig {

    PinStatsConfig DEFAULT = k -> null;

    public default String prefix() {
        return "mm";
    }

    @Override
    public default String uri() {
        return getUriString(this, "uri").orElse("tcp://localhost:18126");
    }

    public default String namePrefix() {
        return getString(this, "namePrefix").orElse("mm.");
    }

    public default String host() {
        return URI.create(uri()).getHost();
    }

    public default Integer port() {
        int port = URI.create(uri()).getPort();
        return port > 0 ? port : null;
    }

    @Override
    default Validated<?> validate() {
        return checkAll(
                this,
                c -> PushRegistryConfig.validate(c),
                checkRequired("namePrefix", PinStatsConfig::namePrefix),
                checkRequired("host", PinStatsConfig::host),
                checkRequired("port", PinStatsConfig::port));
    }
}
