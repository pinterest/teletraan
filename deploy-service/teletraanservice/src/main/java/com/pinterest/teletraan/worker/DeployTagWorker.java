package com.pinterest.teletraan.worker;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.dao.*;
import com.pinterest.deployservice.db.DatabaseUtil;
import com.pinterest.deployservice.rodimus.RodimusManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.TransformerUtils;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.*;

public class DeployTagWorker implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(DeployTagWorker.class);
    private static final int MAX_QUERY_TAGS_SIZE = 200;

    private final HostDAO hostDAO;
    private final EnvironDAO environDAO;
    private final DeployConstraintDAO deployConstraintDAO;
    private final HostTagDAO hostTagDAO;
    private final RodimusManager rodimusManager;
    private final BasicDataSource dataSource;
    private final UtilDAO utilDAO;

    public DeployTagWorker(ServiceContext serviceContext) {
        hostDAO = serviceContext.getHostDAO();
        environDAO = serviceContext.getEnvironDAO();
        deployConstraintDAO = serviceContext.getDeployConstraintDAO();
        hostTagDAO = serviceContext.getHostTagDAO();
        rodimusManager = serviceContext.getRodimusManager();
        dataSource = serviceContext.getDataSource();
        utilDAO = serviceContext.getUtilDAO();
    }


    private void processEachEnvironConstraint(DeployConstraintBean bean) throws Exception {
        EnvironBean environBean = environDAO.getEnvByDeployConstraintId(bean.getConstraint_id());
        String tagName = bean.getConstraint_key();
        String envId = environBean.getEnv_id();
        Collection<HostBean> hostBeans = hostDAO.getHostsByEnvId(envId);
        Collection<HostTagBean> hostTagBeans = hostTagDAO.getAllByEnvIdAndTagName(envId, tagName);


        Collection<String> envHostIds = CollectionUtils.collect(hostBeans, TransformerUtils.invokerTransformer("getHost_id"));
        Collection<String> envHostIdsWithHostTag = CollectionUtils.collect(hostTagBeans, TransformerUtils.invokerTransformer("getHost_id"));

        List<String> missings = new ArrayList(CollectionUtils.subtract(envHostIds, envHostIdsWithHostTag));
        List<String> extras = new ArrayList(CollectionUtils.subtract(envHostIdsWithHostTag, envHostIds));
        if (missings.size() == 0 && extras.size() == 0) {
            LOG.info(String.format("Env %s host tag is in sync", envId));
            if(bean.getState() != TagSyncState.FINISHED) {
                bean.setLast_update(System.currentTimeMillis());
                bean.setState(TagSyncState.FINISHED);
                deployConstraintDAO.updateById(bean.getConstraint_id(), bean);
                LOG.info(String.format("Env %s deploy constraint state has been updated to %s", envId, bean.getState()));
            }
            return;
        }

        LOG.info(String.format("Env %s host tag is not in sync, working on it, extras %s, missings %s", envId, extras, missings));

        // need to update deploy constraint state into PROCESSING
        bean.setState(TagSyncState.PROCESSING);
        bean.setLast_update(System.currentTimeMillis());
        deployConstraintDAO.updateById(bean.getConstraint_id(), bean);
        LOG.info(String.format("Env %s deploy constraint state has been updated into %s", envId, bean.getState()));


        // 1. remove host tags from extras in db
        if (!extras.isEmpty()) {
            hostTagDAO.deleteAllByEnvIdAndHostIds(envId, extras);
            LOG.info(String.format("Env %s host tags of %s have been removed", envId, extras));
        }

        // 2. add host tags from missing in db ( query for CMDB for the missing host tags )
        if (!missings.isEmpty()) {
            List<UpdateStatement> statements = new ArrayList<>();

            for(int i = 0; i < missings.size(); i += MAX_QUERY_TAGS_SIZE) {
                Collection<String> oneBatch = missings.subList(i, Math.min(i + MAX_QUERY_TAGS_SIZE, missings.size()));
                LOG.info(String.format("Env %s start get ec2 tags %s for host_ids %s", envId, tagName, oneBatch));
                Map<String, Map<String, String>> hostMissingEc2Tags = rodimusManager.getEc2Tags(oneBatch);
                LOG.info(String.format("Env %s host ec2 tags %s results: %s", envId, tagName, hostMissingEc2Tags));
                if (hostMissingEc2Tags == null) {
                    continue;
                }
                for (String hostId : hostMissingEc2Tags.keySet()) {
                    Map<String, String> ec2Tags = hostMissingEc2Tags.get(hostId);
                    if (ec2Tags == null) {
                        continue;
                    }
                    if (ec2Tags.containsKey(tagName)) {
                        String tagValue = ec2Tags.get(tagName);
                        if (tagValue == null) {
                            continue;
                        }
                        HostTagBean hostTagBean = new HostTagBean();
                        hostTagBean.setHost_id(hostId);
                        hostTagBean.setTag_name(tagName);
                        hostTagBean.setTag_value(tagValue);
                        hostTagBean.setEnv_id(envId);
                        hostTagBean.setCreate_date(System.currentTimeMillis());
                        statements.add(hostTagDAO.genInsertOrUpdate(hostTagBean));
                    }
                }
            }
            DatabaseUtil.transactionalUpdate(dataSource, statements);
            LOG.info(String.format("Env %s host tags have been updated", envId));
        }

    }

    private void processBatch() throws Exception {
        List<DeployConstraintBean> jobs = deployConstraintDAO.getAllActiveDeployConstraint();
        if (CollectionUtils.isNotEmpty(jobs)) {
            Collections.shuffle(jobs);
            for (DeployConstraintBean job : jobs) {
                LOG.info("process job: {}", job);
                String lockName = String.format("DeployTagWorker-%s", job.getConstraint_id());
                Connection connection = utilDAO.getLock(lockName);
                if (connection != null) {
                    try {
                        processEachEnvironConstraint(job);
                    } catch (Exception e) {
                        LOG.error("failed to process job: {}", job.toString(), e);
                        job.setState(TagSyncState.ERROR);
                        deployConstraintDAO.updateById(job.getConstraint_id(), job);
                        LOG.error("updated job state to {}", TagSyncState.ERROR);
                    } finally {
                        utilDAO.releaseLock(lockName, connection);
                    }

                } else {
                    LOG.warn("failed to get lock {}", lockName);
                }
            }
        }
    }

    @Override
    public void run() {
        try {
            processBatch();
        } catch (Throwable t) {
            LOG.error("Failed to run DeployTagWorker", t);
        }
    }
}
