/**
 * Copyright (c) 2017 Pinterest, Inc.
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
package com.pinterest.deployservice.bean;

import org.joda.time.DateTime;

/*
alert_name=response+codes&
triggered=True&
triggered_date=1510165897.62&empty_data_untriggered_date=None&empty_data_triggered_date=None&empty_data_triggered=False&alert_id=-1&untriggered_date=None
 */
public class ExternalAlert {
    private String name;
    private boolean triggered;
    private DateTime triggeredDate;
    private DateTime unTriggeredDate;
    private boolean emptyDataTriggered;
    private String id;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
    }

    public DateTime getTriggeredDate() {
        return triggeredDate;
    }

    public void setTriggeredDate(DateTime triggeredDate) {
        this.triggeredDate = triggeredDate;
    }

    public DateTime getUnTriggeredDate() {
        return unTriggeredDate;
    }

    public void setUnTriggeredDate(DateTime unTriggeredDate) {
        this.unTriggeredDate = unTriggeredDate;
    }

    public boolean isEmptyDataTriggered() {
        return emptyDataTriggered;
    }

    public void setEmptyDataTriggered(boolean emptyDataTriggered) {
        this.emptyDataTriggered = emptyDataTriggered;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
