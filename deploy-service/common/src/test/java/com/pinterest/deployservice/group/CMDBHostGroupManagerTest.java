package com.pinterest.deployservice.group;

import com.pinterest.deployservice.bean.HostBean;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

public class CMDBHostGroupManagerTest {

  @Test
  @Ignore
  public void getHostIdsByGroup() throws Exception {
    CMDBHostGroupManager manager= new CMDBHostGroupManager("http://cmdbapi.pinadmin.com");
    Map<String, HostBean>  ret = manager.getHostIdsByGroup("adminapp");
    Assert.assertTrue(ret.size()>0);
  }

  @Test
  @Ignore
  public void getLastInstanceId() throws Exception {
    CMDBHostGroupManager manager= new CMDBHostGroupManager("http://cmdbapi.pinadmin.com");
    String s = manager.getLastInstanceId("adminapp");
    Assert.assertTrue(s!=null && s.length()>0);
  }

}
