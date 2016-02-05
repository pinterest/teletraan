package com.pinterest.arcee.aws;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.pinterest.arcee.bean.ReservedInstanceBean;
import com.pinterest.arcee.dao.ReservedInstanceInfoDAO;
import org.apache.commons.lang.StringUtils;

import java.util.*;


public class ReservedInstanceFetcher implements ReservedInstanceInfoDAO {
    private AmazonEC2Client ec2Client;
    private static final String RUNNING_CODE = "16";
    private static final Integer MAX_RETURN_RESULT = 1000;
    public ReservedInstanceFetcher(AmazonEC2Client client) {
        this.ec2Client = client;
    }

    @Override
    public int getReservedInstanceCount(String instanceType) throws Exception {
        DescribeReservedInstancesRequest request = new DescribeReservedInstancesRequest();
        Filter stateFilter = new Filter("state", Arrays.asList("active"));
        Filter instanceTypeFilter = new Filter("instance-type", Arrays.asList(instanceType));
        request.setFilters(Arrays.asList(stateFilter, instanceTypeFilter));
        DescribeReservedInstancesResult result = ec2Client.describeReservedInstances(request);
        List<ReservedInstances> reservationList = result.getReservedInstances();
        int reservedInstanceCount = 0;
        for (ReservedInstances reservedInstances : reservationList) {
            reservedInstanceCount += reservedInstances.getInstanceCount();
        }

        return reservedInstanceCount;
    }

    @Override
    public int getRunningReservedInstanceCount(String instanceType) throws Exception {
        DescribeInstancesRequest request = new DescribeInstancesRequest();
        ArrayList<Filter> filters = new ArrayList<>();
        filters.add(new Filter("instance-state-code", Arrays.asList(RUNNING_CODE)));
        filters.add(new Filter("instance-type", Arrays.asList(instanceType)));
        request.setFilters(filters);
        request.setMaxResults(MAX_RETURN_RESULT);
        DescribeInstancesResult result = ec2Client.describeInstances(request);

        String nextToken = result.getNextToken();
        Integer instanceCount  = 0;
        while (true) {
            List<Reservation> reservations = result.getReservations();
            for (Reservation reservation : reservations) {
                List<Instance> instances = reservation.getInstances();
                for (Instance instance : instances) {
                    if (StringUtils.isEmpty(instance.getInstanceLifecycle())) {
                        instanceCount++;
                    }
                }
            }

            if (StringUtils.isEmpty(nextToken)) {
                break;
            }
            request.setNextToken(nextToken);
            result = ec2Client.describeInstances(request);
            nextToken = result.getNextToken();
        }
        return instanceCount;
    }

    @Override
    public Collection<ReservedInstanceBean> getAllReservedInstanceInfo() throws Exception {
        DescribeReservedInstancesRequest request = new DescribeReservedInstancesRequest();
        Filter stateFilter = new Filter();
        stateFilter.setName("state");
        stateFilter.setValues(Arrays.asList("active"));
        request.setFilters(Arrays.asList(stateFilter));
        DescribeReservedInstancesResult result = ec2Client.describeReservedInstances(request);
        List<ReservedInstances> reservationList = result.getReservedInstances();
        HashMap<String, HashMap<String, Integer>> instanceCountMap = new HashMap<>();
        for (ReservedInstances reservedInstances : reservationList) {
            String zone = reservedInstances.getAvailabilityZone();
            String instanceType = reservedInstances.getInstanceType();
            int instanceCount = reservedInstances.getInstanceCount();
            if (instanceCountMap.containsKey(instanceType)) {
                HashMap<String, Integer> map = instanceCountMap.get(instanceType);
                Integer currentSize = map.getOrDefault(zone, 0);
                map.put(zone, currentSize + instanceCount);
            } else {
                HashMap<String, Integer> map = new HashMap<>();
                map.put(zone, instanceCount);
                instanceCountMap.put(instanceType, map);
            }
        }
        List<ReservedInstanceBean> reservedInstanceBeans = new ArrayList<>();
        for (HashMap.Entry<String, HashMap<String, Integer>> entry : instanceCountMap.entrySet()) {
            for (HashMap.Entry<String, Integer> item : entry.getValue().entrySet()) {
                ReservedInstanceBean reservedInstanceBean = new ReservedInstanceBean();
                reservedInstanceBean.setInstanceType(entry.getKey());
                reservedInstanceBean.setAvailabilityZone(item.getKey());
                reservedInstanceBean.setReservedInstanceCount(item.getValue());
                reservedInstanceBeans.add(reservedInstanceBean);
            }
        }
        return reservedInstanceBeans;
    }
}
