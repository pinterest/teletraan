package com.pinterest.deployservice.handler;

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

        //Context maxParallelThershold set 
        Assert.assertEquals(1, PingHandler.calculateParallelThreshold(bean, 2, 1), 1);
        Assert.assertEquals(10, PingHandler.calculateParallelThreshold(bean, 2, 1), 10);
        Assert.assertEquals(10, PingHandler.calculateParallelThreshold(bean, 2, 1), 100);

        
    }
}
