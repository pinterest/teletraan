package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.DeployConstraintBean;
import com.pinterest.deployservice.bean.EnvironBean;

import org.junit.Assert;
import org.junit.Test;


public class PingHandlerTest {
    @Test
    public void getGetFinalMaxParallelCount() throws Exception {
        EnvironBean bean = new EnvironBean();
        //Always return 1 when nothing set
        Assert.assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 1));
        Assert.assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 10));
        Assert.assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 100));

        //Only hosts set
        bean.setMax_parallel(10);
        Assert.assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 1));
        Assert.assertEquals(10, PingHandler.getFinalMaxParallelCount(bean, 10));
        Assert.assertEquals(10, PingHandler.getFinalMaxParallelCount(bean, 100));

        //Only percentage set
        bean.setMax_parallel(null);
        bean.setMax_parallel_pct(20);
        Assert.assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 1));
        Assert.assertEquals(2, PingHandler.getFinalMaxParallelCount(bean, 10));
        Assert.assertEquals(20, PingHandler.getFinalMaxParallelCount(bean, 100));

        //Both set, pick the smaller one
        bean.setMax_parallel(10);
        Assert.assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 1));
        Assert.assertEquals(2, PingHandler.getFinalMaxParallelCount(bean, 10));
        Assert.assertEquals(10, PingHandler.getFinalMaxParallelCount(bean, 100));

    }

    @Test
    public void ensureMaxParallelizationForAllEnvironServiceContext() throws Exception {
        EnvironBean bean = new EnvironBean();
   
        Assert.assertEquals(1, PingHandler.getFinalMaxParallelCount(bean, 1, 1));
        Assert.assertEquals(10, PingHandler.getFinalMaxParallelCount(bean, 10, 1));
        Assert.assertEquals(10, PingHandler.getFinalMaxParallelCount(bean, 100, 10));

    }

    @Test
    public void ensureMaxParallelizationForConstraint() throws Exception {
        EnvironBean environBean = new EnvironBean();
        
        //TODO implement test
        DeployConstraintBean deployBean = new DeployConstraintBean();
        deployBean.setMax_parallel(10L);

        Assert.assertEquals(1, PingHandler.getFinalMaxParallelCount(deployBean, environBean, 1));
        Assert.assertEquals(10, PingHandler.getFinalMaxParallelCount(deployBean, environBean, 10));
        Assert.assertEquals(10, PingHandler.getFinalMaxParallelCount(deployBean, environBean, 100));

    }

}
