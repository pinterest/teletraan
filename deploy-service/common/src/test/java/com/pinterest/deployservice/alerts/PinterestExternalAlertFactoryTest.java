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
package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.bean.ExternalAlert;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PinterestExternalAlertFactoryTest {

    @Test
    void getAlert() {
        PinterestExternalAlertFactory factory = new PinterestExternalAlertFactory();
        ExternalAlert alert =
                factory.getAlert(
                        "alert_name=response+codes&triggered=True"
                                + "&triggered_date=1510165897.62&empty_data_untriggered_date=None&\n"
                                + " empty_data_triggered_date=None&empty_data_triggered=False&alert_id=-1"
                                + "&untriggered_date=None");

        Assertions.assertTrue(alert.isTriggered());
        Assertions.assertFalse(alert.isEmptyDataTriggered());
        Assertions.assertEquals(alert.getId(), "-1");
        Assertions.assertEquals(
                alert.getTriggeredDate(), new DateTime().withMillis(1510165897620L));
    }
}
