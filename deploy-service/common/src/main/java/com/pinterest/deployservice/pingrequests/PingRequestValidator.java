package com.pinterest.deployservice.pingrequests;

import com.pinterest.deployservice.bean.PingRequestBean;

public abstract class PingRequestValidator {
    public abstract void validate(PingRequestBean bean) throws Exception;
}
