package com.pinterest.deployservice.pingrequests;

import com.pinterest.deployservice.bean.PingRequestBean;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.common.MetricsDataFactory;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Ec2InstanceValidator extends PingRequestValidator {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsDataFactory.class);

    @Override
    public void validate(PingRequestBean bean) throws Exception {
        //Validate instance for ec2
        if (!StringUtils.startsWith(bean.getHostId(), "i-")) {
            LOG.warn("Ignore invalid id {} for host {}", bean.getHostId(), bean.getHostName());
            throw new DeployInternalException(
                String.format("Host id %s is not a valid ec2 instance id"),
                bean.getHostId() == null ? "null" : bean.getHostId());
        }
    }
}
