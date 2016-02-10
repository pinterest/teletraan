package com.pinterest.arcee.Qubole;



public class QuboleClusterBean {
    private int minSize;

    private int maxSize;

    private int runningReservedInstanceCount;

    private int runningSpotInstanceCount;

    private String clusterId;


    public  void setMinSize(int minSize) { this.minSize = minSize; }

    public int getMinSize() { return minSize; }

    public void setMaxSize(int maxSize) { this.maxSize = maxSize; }

    public int getMaxSize() { return maxSize; }

    public void setRunningReservedInstanceCount(int runningReservedInstanceCount) {
        this.runningReservedInstanceCount = runningReservedInstanceCount; }

    public int getRunningReservedInstanceCount() { return runningReservedInstanceCount; }

    public void setRunningSpotInstanceCount(int runningSpotInstanceCount) {
        this.runningSpotInstanceCount = runningSpotInstanceCount;
    }

    public int getRunningSpotInstanceCount() { return runningSpotInstanceCount; }

    public void setClusterId(String clusterId) { this.clusterId = clusterId; }

    public String getClusterId() { return this.clusterId; }
}
