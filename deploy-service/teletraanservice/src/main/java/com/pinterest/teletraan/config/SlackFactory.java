/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.pinterest.deployservice.chat.ChatManager;
import com.pinterest.deployservice.chat.SlackChatManager;
import com.pinterest.deployservice.common.KeyReader;
import com.pinterest.deployservice.common.KeyReaderFactory;
import javax.validation.constraints.NotEmpty;

@JsonTypeName("slack")
public class SlackFactory implements ChatFactory {
    @NotEmpty @JsonProperty private String key;

    @NotEmpty @JsonProperty private String reader; // specific to slack token reading

    @JsonProperty private String domain; // email domain

    public String getKey() {
        return key;
    }

    public void setReader(String reader) {
        this.reader = reader;
    }

    public String getReader() {
        return reader;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public ChatManager create() throws Exception {
        KeyReader keyReader = new KeyReaderFactory().create(this.reader);
        keyReader.init(key);
        return new SlackChatManager(keyReader.getKey(), domain);
    }
}
