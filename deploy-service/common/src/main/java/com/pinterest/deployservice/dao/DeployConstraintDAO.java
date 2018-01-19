package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.DeployConstraintBean;
import com.pinterest.deployservice.bean.UpdateStatement;

import java.util.List;

public interface DeployConstraintDAO {
    DeployConstraintBean getById(String constraintId) throws Exception;

    void updateById(String constraintId, DeployConstraintBean deployConstraintBean) throws Exception;

    UpdateStatement genInsertStatement(DeployConstraintBean deployConstraintBean) throws Exception;

    UpdateStatement genUpdateStatement(String constraintId, DeployConstraintBean deployConstraintBean) throws Exception;

    void delete(String constraintId) throws Exception;

    List<DeployConstraintBean> getAllActiveDeployConstraint() throws Exception;
}
