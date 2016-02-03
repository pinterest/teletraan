package com.pinterest.arcee.bean;



public class ReservedInstanceBean {

    private String instanceType;

    private String availabilityZone;

    private Integer reservedInstanceCount;

    private Integer runningInstance;


    public String getInstanceType() { return instanceType; }

    public void setInstanceType(String instanceType) { this.instanceType = instanceType; }

    public String getAvailabilityZone() { return availabilityZone; }

    public void setAvailabilityZone(String availabilityZone) { this.availabilityZone = availabilityZone; }

    public Integer getReservedInstanceCount() { return reservedInstanceCount; }

    public void setReservedInstanceCount(Integer reservedInstanceCount) { this. reservedInstanceCount = reservedInstanceCount; }

    public Integer getRunningInstance() { return runningInstance; }

    public void setRunningInstance(Integer runningInstance) { this.runningInstance = runningInstance; }
}
