package com.pinterest.deployservice.alerts;

import com.pinterest.deployservice.bean.DeployBean;
import com.pinterest.deployservice.bean.EnvironBean;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * AutoRollbackAction rollback to the last known good version before action window
 */
public class AutoRollbackAction extends AlertAction {

  public final static Logger LOG = LoggerFactory.getLogger(AutoRollbackAction.class);

  //Maximum time looks back. May make it pass in from webhook later
  private final static int MaxLookbackDays = 30;
  private final static int MaxDeploysToCheck = 100;

  @Override
  public Object perform(AlertContext context, EnvironBean environ, DeployBean lastDeploy,
                        int actionWindowInSeconds,
                        String operator) throws Exception {
    //It is tricky to do roll back. Here we basically rollback to the last succeed deploy before
    // the now-window
    List<DeployBean>
        candidates =
        context.getDeployHandler().getDeployCandidates(environ.getEnv_id(),
            new Interval(DateTime.now().minusDays(MaxLookbackDays),
                DateTime.now().minusSeconds(actionWindowInSeconds)),
            MaxDeploysToCheck, true);
    //Result sorted desending on start date
    if (candidates.size() > 0) {
      try {
        LOG.info("AutoRollback environ {} stage {} to {}", environ.getEnv_name(),
            environ.getStage_name(), candidates.get(0).getDeploy_id());
        context.getDeployHandler().rollback(environ, candidates.get(0).getDeploy_id(),
            "Alert triggered autorollback",
            operator);
        return candidates.get(0);

      } catch (Exception ex) {
        LOG.error("Failed to rollback for env:{} stage:{} error:{}", environ.getEnv_name(),
            environ.getStage_name(),
            ExceptionUtils.getRootCauseMessage(ex));
        return ExceptionUtils.getRootCauseMessage(ex);
      }
    }
    return "No rollback candidate available";
  }
}
