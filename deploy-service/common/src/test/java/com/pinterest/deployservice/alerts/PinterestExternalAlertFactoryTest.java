package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.bean.ExternalAlert;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

public class PinterestExternalAlertFactoryTest {

  @Test
  public void getAlert() throws Exception {
    PinterestExternalAlertFactory factory = new PinterestExternalAlertFactory();
    ExternalAlert alert = factory.getAlert("alert_name=response+codes&triggered=True"
        + "&triggered_date=1510165897.62&empty_data_untriggered_date=None&\n"
        + " empty_data_triggered_date=None&empty_data_triggered=False&alert_id=-1"
        + "&untriggered_date=None");

    Assert.assertTrue(alert.isTriggered());
    Assert.assertFalse(alert.isEmptyDataTriggered());
    Assert.assertEquals(alert.getId(), "-1");
    Assert.assertEquals(alert.getTriggeredDate(), new DateTime().withMillis(1510165897620L));
  }

}
