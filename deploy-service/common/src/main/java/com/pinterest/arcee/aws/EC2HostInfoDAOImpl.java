/**
 * Copyright 2016 Pinterest, Inc.
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
package com.pinterest.arcee.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;

import com.pinterest.arcee.bean.GroupBean;
import com.pinterest.deployservice.bean.HostBean;
import com.pinterest.arcee.dao.HostInfoDAO;
import com.pinterest.deployservice.common.DeployInternalException;

import com.pinterest.deployservice.bean.HostState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class EC2HostInfoDAOImpl implements HostInfoDAO {
    private static final Logger LOG = LoggerFactory.getLogger(EC2HostInfoDAOImpl.class);
    private AmazonEC2Client ec2Client;
    // from aws sdk doc:
    // http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/ec2/model/InstanceState.html
    private static final int TERMINATED_CODE = 48;
    private static final int STOPPED_CODE = 80;
    private static final String RUNNING_CODE = "16";

    public EC2HostInfoDAOImpl(AmazonEC2Client client) {
        this.ec2Client = client;
    }

    @Override
    public Set<String> getTerminatedHosts(Set<String> staleIds) throws Exception {
        HashSet<String> terminatedHosts = new HashSet<>();

        for (String staleId : staleIds) {
            try {
                DescribeInstancesRequest request = new DescribeInstancesRequest();
                request.setInstanceIds(Collections.singletonList(staleId));
                DescribeInstancesResult result = ec2Client.describeInstances(request);
                List<Reservation> reservations = result.getReservations();
                for (Reservation reservation : reservations) {
                    for (Instance instance : reservation.getInstances()) {
                        int stateCode = instance.getState().getCode();
                        String id = instance.getInstanceId();
                        if (stateCode == TERMINATED_CODE || stateCode == STOPPED_CODE) {
                            LOG.info(String.format(
                                "Instance %s has already been terminated or stopped.", id));
                            terminatedHosts.add(id);
                        }
                    }
                }
            } catch (AmazonServiceException ex) {
                terminatedHosts.add(staleId);
            } catch (AmazonClientException ex) {
                LOG.error(String.format("Get AmazonClientException, exit with terminiatedHost %s", terminatedHosts.toString()), ex);
            } finally {
                Thread.sleep(500);
            }
        }

        return terminatedHosts;
    }

    @Override
    public void terminateHost(String hostId) throws Exception {
        TerminateInstancesRequest request = new TerminateInstancesRequest();
        request.setInstanceIds(Arrays.asList(hostId));
        try {
            ec2Client.terminateInstances(request);
        } catch (AmazonClientException ex) {
            LOG.error(String.format("Failed to call aws terminateInstances whem terminating host %s", hostId), ex);
            throw new DeployInternalException(String.format("Failed to call aws terminateInstances whem terminating host %s", hostId), ex);
        }
    }

    @Override
    public List<HostBean> launchEC2Instances(GroupBean groupBean, int instanceCnt, String subnet) throws Exception {
        RunInstancesRequest request = new RunInstancesRequest();
        request.setImageId(groupBean.getImage_id());
        request.setInstanceType(groupBean.getInstance_type());
        request.setKeyName("ops");
        request.setSecurityGroupIds(Arrays.asList(groupBean.getSecurity_group()));
        request.setSubnetId(subnet);
        request.setUserData(groupBean.getUser_data());
        IamInstanceProfileSpecification iamRole = new IamInstanceProfileSpecification();
        iamRole.setArn(groupBean.getIam_role());
        request.setIamInstanceProfile(iamRole);
        request.setMinCount(instanceCnt);
        request.setMaxCount(instanceCnt);

        List<HostBean> newHosts = new ArrayList<>();
        try {
            RunInstancesResult result = ec2Client.runInstances(request);
            List<Instance> instances = result.getReservation().getInstances();
            LOG.info("Launch instances {}", instances.toString());
            for (Instance instance : instances) {
                HostBean host = new HostBean();
                host.setHost_name(instance.getInstanceId());
                host.setHost_id(instance.getInstanceId());
                host.setIp(instance.getPrivateIpAddress());
                host.setGroup_name(groupBean.getGroup_name());
                host.setState(HostState.PROVISIONED);
                host.setCreate_date(instance.getLaunchTime().getTime());
                host.setLast_update(instance.getLaunchTime().getTime());
                newHosts.add(host);
            }
        } catch (AmazonClientException ex) {
            LOG.error(String.format("Failed to call aws runInstances when launching host %s", newHosts.toString()), ex);
            throw new DeployInternalException(String.format("Failed to call aws runInstances when launching host %s", newHosts.toString()), ex);
        }
        return newHosts;
    }

    @Override
    public List<String> getRunningInstances(List<String> runningIds) throws Exception {
        ArrayList<String> resultIds = new ArrayList<>();
        for (String runningId : runningIds) {
            try {
                DescribeInstancesRequest request = new DescribeInstancesRequest();
                request.setFilters(Collections.singletonList(new Filter("instance-state-code", Collections.singletonList(RUNNING_CODE))));
                request.setInstanceIds(Collections.singletonList(runningId));
                DescribeInstancesResult result = ec2Client.describeInstances(request);
                List<Reservation> reservations = result.getReservations();
                for (Reservation reservation : reservations) {
                    for (Instance instance : reservation.getInstances()) {
                        resultIds.add(instance.getInstanceId());
                    }
                }
            } catch (AmazonServiceException ex) {
                LOG.error(String.format("Failed to process id: %s. Ignore it.", runningId), ex);
            } catch (AmazonClientException ex) {
                LOG.error("Get AmazonClientException", ex);
            } finally {
                Thread.sleep(500);
            }
        }

        return resultIds;
    }
}
