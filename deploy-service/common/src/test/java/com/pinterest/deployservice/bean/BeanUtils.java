package com.pinterest.deployservice.bean;

import java.time.Instant;
import java.util.UUID;

public class BeanUtils {
    public static HostBean createHostBean(Instant createDate) {
        HostBean bean = new HostBean();
        bean.setHost_id("i-" + UUID.randomUUID().toString().substring(0, 8));
        bean.setGroup_name("testEnv-testStage");
        bean.setCreate_date(createDate.toEpochMilli());
        bean.setLast_update(createDate.plusSeconds(1).toEpochMilli());
        bean.setCan_retire(0);
        bean.setState(HostState.PROVISIONED);
        return bean;
    }
}
