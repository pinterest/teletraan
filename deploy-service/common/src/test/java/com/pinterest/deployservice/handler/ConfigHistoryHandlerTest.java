package com.pinterest.deployservice.handler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.ConfigHistoryBean;
import com.pinterest.deployservice.bean.DeployPriority;
import com.pinterest.deployservice.bean.DeployType;
import com.pinterest.deployservice.bean.EnvState;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.common.ChangeFeedJob;
import com.pinterest.deployservice.common.Constants;
import com.pinterest.deployservice.dao.ConfigHistoryDAO;
import com.pinterest.deployservice.dao.EnvironDAO;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

public class ConfigHistoryHandlerTest {

  private static final String testConfigId = "testConfigID";
  private ConfigHistoryHandler configHistoryHandler;
  private ExecutorService mockJobPool;
  private EnvironDAO mockEnvironDAO;
  private ConfigHistoryDAO mockConfigHistoryDAO;

  private ServiceContext createMockServiceContext() {
    mockEnvironDAO = mock(EnvironDAO.class);
    mockConfigHistoryDAO = mock(ConfigHistoryDAO.class);
    mockJobPool = mock(ExecutorService.class);

    ServiceContext serviceContext = new ServiceContext();
    serviceContext.setJobPool(mockJobPool);
    serviceContext.setConfigHistoryDAO(mockConfigHistoryDAO);
    serviceContext.setEnvironDAO(mockEnvironDAO);
    return serviceContext;
  }

  @Before
  public void setUp() throws Exception {
    configHistoryHandler = new ConfigHistoryHandler(createMockServiceContext());
  }

  @Test
  public void updateChangeFeed_withUUIDinPayload() throws Exception {
    String payload =
        "{\"type\":\"Deploy Env Config Change\",\"environment\":\"foo (foo)\","
            + "\"description\":\"https://deploy.pinadmin.com/env/foo/foo/config_history/\","
            + "\"author\":\"test\",\"automation\":\"False\",\"source\":\"Teletraan\","
            + "\"optional-1\":\"Env Advanced Config\",\"optional-2\":\"0000-0000\"}";

    when(mockEnvironDAO.getById(testConfigId)).thenReturn(genDefaultEnvBean());
    when(mockConfigHistoryDAO.getLatestChangesByType(testConfigId,
        Constants.TYPE_ENV_ADVANCED)).thenReturn(
        Arrays.asList(new ConfigHistoryBean(), new ConfigHistoryBean()));

    ArgumentCaptor<ChangeFeedJob>
        changeFeedJobArgumentCaptor =
        ArgumentCaptor.forClass(ChangeFeedJob.class);
    configHistoryHandler.updateChangeFeed(Constants.CONFIG_TYPE_ENV, testConfigId,
        Constants.TYPE_ENV_ADVANCED, "test", "0000-0000");
    verify(mockJobPool).submit(changeFeedJobArgumentCaptor.capture());
    assertEquals(changeFeedJobArgumentCaptor.getValue().getPayload(), payload);
  }

  EnvironBean genDefaultEnvBean() {
    EnvironBean envBean = new EnvironBean();
    envBean.setEnv_id(testConfigId);
    envBean.setEnv_name("foo");
    envBean.setStage_name("foo");
    envBean.setEnv_state(EnvState.NORMAL);
    envBean.setPriority(DeployPriority.NORMAL);
    envBean.setDeploy_id("foo");
    envBean.setDeploy_type(DeployType.REGULAR);
    return envBean;
  }
}