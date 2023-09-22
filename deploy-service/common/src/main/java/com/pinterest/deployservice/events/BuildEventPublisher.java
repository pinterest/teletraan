package com.pinterest.deployservice.events;

import com.pinterest.deployservice.bean.BuildBean;

public interface BuildEventPublisher {
  void publish(BuildBean buildBean, String action);
}
