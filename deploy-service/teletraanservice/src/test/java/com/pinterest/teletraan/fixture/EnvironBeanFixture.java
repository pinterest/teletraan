package com.pinterest.teletraan.fixture;

import java.util.Random;
import java.util.UUID;

import com.pinterest.deployservice.bean.AcceptanceType;
import com.pinterest.deployservice.bean.EnvironBean;
import com.pinterest.deployservice.bean.EnvironState;
import com.pinterest.deployservice.bean.OverridePolicy;

public class EnvironBeanFixture {
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    public static EnvironBean createRandomEnvironBean() {
        EnvironBean environBean = new EnvironBean();
        environBean.setEnv_name(UUID.randomUUID().toString());
        environBean.setStage_name(UUID.randomUUID().toString());
        environBean.setEnv_id(UUID.randomUUID().toString());
        environBean.setDeploy_id(UUID.randomUUID().toString());
        environBean.setState(EnvironState.NORMAL);
        environBean.setSuccess_th(10000);
        environBean.setDescription("description");
        environBean.setAdv_config_id("config_id_1");
        environBean.setSc_config_id("envvar_id_1");
        environBean.setLast_operator("bar");
        environBean.setLast_update(System.currentTimeMillis());
        environBean.setAccept_type(AcceptanceType.AUTO);
        environBean.setNotify_authors(false);
        environBean.setWatch_recipients("watcher");
        environBean.setMax_deploy_num(5100);
        environBean.setMax_deploy_day(366);
        environBean.setIs_docker(false);
        environBean.setMax_parallel_pct(0);
        environBean.setState(EnvironState.NORMAL);
        environBean.setMax_parallel_rp(1);
        environBean.setOverride_policy(OverridePolicy.OVERRIDE);
        environBean.setAllow_private_build(false);
        environBean.setEnsure_trusted_build(false);
        return environBean;
    }
}
