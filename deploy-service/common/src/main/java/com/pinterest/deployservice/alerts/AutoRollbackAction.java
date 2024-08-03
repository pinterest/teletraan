/**
 * Copyright (c) 2017-2024 Pinterest, Inc.
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
package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** AutoRollbackAction rollback to the last known good version before action window */
public class AutoRollbackAction extends AlertAction {

    public static final Logger LOG = LoggerFactory.getLogger(AutoRollbackAction.class);

    // Maximum time looks back. May make it pass in from webhook later
    private static final int MaxLookbackDays = 30;
    private static final int MaxDeploysToCheck = 100;

    @Override
    public Object perform(
            AlertContext context,
            EnvironBean environ,
            DeployBean lastDeploy,
            int actionWindowInSeconds,
            String operator)
            throws Exception {
        // It is tricky to do roll back. Here we basically rollback to the last succeed deploy
        // before
        // the now-window
        List<DeployBean> candidates =
                context.getDeployHandler()
                        .getDeployCandidates(
                                environ.getEnv_id(),
                                new Interval(
                                        DateTime.now().minusDays(MaxLookbackDays),
                                        DateTime.now().minusSeconds(actionWindowInSeconds)),
                                MaxDeploysToCheck,
                                true);
        // Result sorted desending on start date
        if (candidates.size() > 0) {
            try {
                LOG.info(
                        "AutoRollback environ {} stage {} to {}",
                        environ.getEnv_name(),
                        environ.getStage_name(),
                        candidates.get(0).getDeploy_id());
                context.getDeployHandler()
                        .rollback(
                                environ,
                                candidates.get(0).getDeploy_id(),
                                "Alert triggered autorollback",
                                operator);
                return candidates.get(0);

            } catch (Exception ex) {
                LOG.error(
                        "Failed to rollback for env:{} stage:{} error:{}",
                        environ.getEnv_name(),
                        environ.getStage_name(),
                        ExceptionUtils.getRootCauseMessage(ex));
                return ExceptionUtils.getRootCauseMessage(ex);
            }
        }
        return "No rollback candidate available";
    }
}
