package com.pinterest.deployservice.pingrequests;

import java.util.Set;

import com.pinterest.deployservice.bean.PingRequestBean;

public abstract class PingRequestValidator {
    public abstract void validate(PingRequestBean bean, Set<String> accountAllowList) throws Exception;
}
