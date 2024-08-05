/**
 * Copyright (c) 2018-2024 Pinterest, Inc.
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
package com.pinterest.deployservice.pingrequests;

import com.pinterest.deployservice.bean.PingRequestBean;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.common.MetricsDataFactory;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            LOG.warn(
                    "Ignore ping request from host {} with unknown account id: {}",
                    bean.getHostName(),
                    accountId);
            throw new DeployInternalException(
                    String.format("Host id {} is from an disallowed AWS account: {}"),
                    StringUtils.defaultString(bean.getHostId()),
                    accountId);
        }
    }
}
