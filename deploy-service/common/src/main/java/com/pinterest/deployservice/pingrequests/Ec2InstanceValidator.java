package com.pinterest.deployservice.pingrequests;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.bean.PingRequestBean;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.common.MetricsDataFactory;

public class Ec2InstanceValidator extends PingRequestValidator {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsDataFactory.class);

    @Override
    public void validate(PingRequestBean bean, Set<String> accountAllowList) throws Exception {
        // Validate instance for ec2
        if (!StringUtils.startsWith(bean.getHostId(), "i-")) {
            LOG.warn("Ignore invalid id {} for host {}", bean.getHostId(), bean.getHostName());
            throw new DeployInternalException(
                    String.format("Host id {} is not a valid ec2 instance id"),
                    StringUtils.defaultString(bean.getHostId()));
        }

        // Validate account id of EC2 instance
        if (accountAllowList == null) {
            return;
        }

        String accountId = bean.getAccountId();
        if (StringUtils.isBlank(accountId)) {
            return;
        }

        if (!accountAllowList.contains(accountId)) {
            LOG.warn("Ignore ping request from host {} with unknown account id: {}", bean.getHostName(), accountId);
            throw new DeployInternalException(
                    String.format("Host id {} is from an disallowed AWS account: {}"),
                    StringUtils.defaultString(bean.getHostId()), accountId);
        }
    }
}
