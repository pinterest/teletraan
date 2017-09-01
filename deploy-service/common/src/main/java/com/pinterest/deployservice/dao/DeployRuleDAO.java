package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.DeployRuleBean;

/**
 * Created by suli on 8/31/17.
 */
public interface DeployRuleDAO {
    DeployRuleBean getById(String ruleId) throws Exception;

    void insert(DeployRuleBean deployRuleBean) throws Exception;

    void delete(String ruleId) throws Exception;
}
