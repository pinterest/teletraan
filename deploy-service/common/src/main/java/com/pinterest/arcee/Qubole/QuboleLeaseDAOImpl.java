package com.pinterest.arcee.Qubole;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pinterest.arcee.dao.LeaseDAO;
import com.pinterest.deployservice.common.HTTPClient;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class QuboleLeaseDAOImpl implements LeaseDAO {
    private String credential;

    private static String API_URL = "https://api.qubole.com/api/v1.3";
    private static String MIN_SIZE_TAG = "initial_nodes";
    private static String MAX_SIZE_TAG = "max_nodes";

    public QuboleLeaseDAOImpl(String credential) {
        this.credential = credential;
    }

    @Override
    public void lendInstances(String cluster, int count) throws Exception {
        changeInstanceSize(cluster, count, true);
    }

    @Override
    public void returnInstances(String cluster, int count) throws Exception {
        changeInstanceSize(cluster, count, false);
    }

    private void changeInstanceSize(String cluster, int count, boolean increase) throws Exception {
        QuboleClusterBean quboleClusterBean = getCluster(cluster);
        QuboleClusterBean newBean = new QuboleClusterBean();
        if (increase) {
            newBean.setMinSize(quboleClusterBean.getMinSize() + count);
            newBean.setMaxSize(quboleClusterBean.getMaxSize() + count);
        } else {
            newBean.setMinSize(quboleClusterBean.getMinSize() - count);
            newBean.setMaxSize(quboleClusterBean.getMaxSize() - count);
        }

        updateClusterConfiguration(cluster, newBean);
    }

    private void updateClusterConfiguration(String clusterName, QuboleClusterBean quboleClusterBean) throws Exception {
        HTTPClient client = new HTTPClient();
        JsonObject configuration = new JsonObject();
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty(MIN_SIZE_TAG, quboleClusterBean.getMinSize());
        jsonObject.addProperty(MAX_SIZE_TAG, quboleClusterBean.getMaxSize());

        configuration.add("node_configuration", jsonObject);
        configuration.addProperty("push", true);
        String path = String.format("%s/clusters/%s", API_URL, clusterName);
        client.put(path, configuration.toString(), generateHeaders(), 3);
    }

    public QuboleClusterBean getCluster(String clusterName) throws Exception {
        HTTPClient client = new HTTPClient();
        String statePath = String.format("%s/clusters/%s/state",API_URL, clusterName);
        String clusterState = client.get(statePath, null, generateHeaders(), 3);

        String configurationPath = String.format("%s/clusters/%s", API_URL, clusterName);
        String configuration = client.get(configurationPath, null, generateHeaders(), 3);
        return fromJson(clusterName, configuration, clusterState);
    }

    private Map<String, String> generateHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-type", "application/json");
        headers.put("X-AUTH-TOKEN", credential);
        headers.put("Accept", "application/json");
        return headers;
    }

    public QuboleClusterBean fromJson(String clusterId, String configuration, String clusterState) {
        if (StringUtils.isEmpty(configuration) || StringUtils.isEmpty(clusterState)) {
            return null;
        }

        QuboleClusterBean quboleClusterBean = new QuboleClusterBean();
        quboleClusterBean.setClusterId(clusterId);
        JsonParser parser = new JsonParser();
        JsonObject jsonObj = (JsonObject) parser.parse(configuration);
        // http://docs.qubole.com/en/latest/rest-api/cluster_api/get-cluster-information.html
        JsonObject nodeConfiguration = jsonObj.getAsJsonObject("node_configuration");
        quboleClusterBean.setMinSize(nodeConfiguration.getAsJsonPrimitive(MIN_SIZE_TAG).getAsInt());
        quboleClusterBean.setMaxSize(nodeConfiguration.getAsJsonPrimitive(MAX_SIZE_TAG).getAsInt());


        JsonObject stateObj = (JsonObject)parser.parse(clusterState);
        JsonArray array = stateObj.getAsJsonArray("nodes");
        int reservedInsanceCount = 0;
        int spotInstanceCount = 0;
        for (int i = 0; i < array.size(); ++i) {
            JsonObject nodeObject = array.get(i).getAsJsonObject();
            Boolean isSpotInstance = nodeObject.getAsJsonPrimitive("is_spot_instance").getAsBoolean();
            if (isSpotInstance) {
                spotInstanceCount++;
            } else {
                reservedInsanceCount++;
            }
        }
        quboleClusterBean.setRunningReservedInstanceCount(reservedInsanceCount);
        quboleClusterBean.setRunningSpotInstanceCount(spotInstanceCount);
        return quboleClusterBean;
    }
}
