/**
 * Copyright (c) 2017 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.DeployConstraintBean;
import com.pinterest.deployservice.bean.UpdateStatement;
import java.util.List;

public interface DeployConstraintDAO {
    DeployConstraintBean getById(String constraintId) throws Exception;

    void updateById(String constraintId, DeployConstraintBean deployConstraintBean)
            throws Exception;

    UpdateStatement genInsertStatement(DeployConstraintBean deployConstraintBean) throws Exception;

    UpdateStatement genUpdateStatement(
            String constraintId, DeployConstraintBean deployConstraintBean) throws Exception;

    void delete(String constraintId) throws Exception;

    List<DeployConstraintBean> getAllActiveDeployConstraint() throws Exception;
}
