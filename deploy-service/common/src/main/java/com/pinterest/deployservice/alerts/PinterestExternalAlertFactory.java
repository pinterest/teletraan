package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.bean.ExternalAlert;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PinterestExternalAlertFactory. Building alert from following sample

 POST / HTTP/1.1
 Host: localhost:6000
 Connection: keep-alive
 Accept-Encoding: gzip, deflate
 Accept:
 User-Agent: python-requests/2.13.0
 Authorization: token put_token_here
 Content-Length: 194
 Content-Type: application/x-www-form-urlencoded

 alert_name=response+codes&triggered=True&triggered_date=1510165897.62
 &empty_data_untriggered_date=None&
 empty_data_triggered_date=None&empty_data_triggered=False&alert_id=-1&untriggered_date=None
 */
public class PinterestExternalAlertFactory extends ExternalAlertFactory {

  private static final Logger LOG = LoggerFactory.getLogger(PinterestExternalAlertFactory.class);

  private static DateTime tryGetDate(Map<String, String> nvp, String key) {
    DateTime ret = null;
    if (nvp.containsKey(key)) {
      String strVal = nvp.get(key);
      if (!StringUtils.equals("None", strVal)) {
        double d = 1000 * Double.parseDouble(strVal);
        ret = new DateTime().withMillis((long) d);
      }
    }
    return ret;
  }

  public static Map<String, String> getValues(String body) throws UnsupportedEncodingException {
    Map<String, String> values = new LinkedHashMap<String, String>();
    String[] pairs = body.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
      String
          value =
          idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                                             : null;
      values.put(key, value);
    }
    return values;
  }

  @Override
  public ExternalAlert getAlert(String webhookBody) {
    try {
      if (!StringUtils.isBlank((webhookBody))) {
        Map<String, String> nvp = getValues(webhookBody);
        ExternalAlert alert = new ExternalAlert();
        alert.setName(nvp.get("alert_name"));
        alert.setId(nvp.get("alert_id"));
        alert.setTriggered(Boolean.parseBoolean(nvp.get("triggered").toLowerCase()));
        DateTime triggeredDate = tryGetDate(nvp, "triggered_date");
        if (triggeredDate != null) {
          alert.setTriggeredDate(triggeredDate);
        }
        DateTime untriggeredDate = tryGetDate(nvp, "untriggered_date");
        if (untriggeredDate != null) {
          alert.setUnTriggeredDate(untriggeredDate);
        }
        alert.setEmptyDataTriggered(Boolean.parseBoolean(nvp.get("empty_data_triggered")));

        return alert;

      }
    } catch (Exception e) {
      LOG.error("Failed to parse alert body {} error is {}", webhookBody, e.getMessage());
    }
    return null;
  }

}
