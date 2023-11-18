package com.pinterest.deployservice.pingrequests;

import java.util.List;

import com.pinterest.deployservice.bean.PingRequestBean;

public abstract class PingRequestValidator {
    public abstract void validate(PingRequestBean bean, List<String> accountAllowList) throws Exception;
}
