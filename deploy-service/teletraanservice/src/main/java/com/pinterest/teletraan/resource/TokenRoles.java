/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
package com.pinterest.teletraan.resource;

import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.TokenRolesDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.bean.AuthZResource;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.ws.rs.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TokenRoles {
    private static final Logger LOG = LoggerFactory.getLogger(TokenRoles.class);
    public static final long VALIDATE_TIME = 180;
    private final TokenRolesDAO tokenRolesDAO;

    protected TokenRoles(TeletraanServiceContext context) {
        tokenRolesDAO = context.getTokenRolesDAO();
    }

    public List<TokenRolesBean> getByResource(String resourceId, AuthZResource.Type resourceType)
            throws Exception {
        return tokenRolesDAO.getByResource(resourceId, resourceType);
    }

    public TokenRolesBean getByNameAndResource(
            String scriptName, String resourceId, AuthZResource.Type resourceType)
            throws Exception {
        return tokenRolesDAO.getByNameAndResource(scriptName, resourceId, resourceType);
    }

    public void update(
            TokenRolesBean bean,
            String scriptName,
            String resourceId,
            AuthZResource.Type resourceType)
            throws Exception {
        tokenRolesDAO.update(bean, scriptName, resourceId, resourceType);
        LOG.info(
                "Successfully updated script {} permission for resource {} with {}",
                scriptName,
                resourceId,
                bean);
    }

    public Response create(
            UriInfo uriInfo,
            TokenRolesBean bean,
            String resourceId,
            AuthZResource.Type resourceType)
            throws Exception {
        String token = CommonUtils.getBase64UUID();
        bean.setToken(token);
        bean.setResource_id(resourceId);
        bean.setResource_type(resourceType);
        bean.setExpire_date(Instant.now().plus(VALIDATE_TIME, ChronoUnit.DAYS).toEpochMilli());
        tokenRolesDAO.insert(bean);
        bean.setToken("xxxxxxxx");
        LOG.info(
                "Successfully created new script permission for resource {} with {}",
                resourceId,
                bean);
        TokenRolesBean newBean = tokenRolesDAO.getByToken(token);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI roleUri = ub.path(newBean.getScript_name()).build();
        return Response.created(roleUri).entity(newBean).build();
    }

    public void delete(String scriptName, String resourceId, AuthZResource.Type resourceType)
            throws Exception {
        tokenRolesDAO.delete(scriptName, resourceId, resourceType);
        LOG.info(
                "Successfully deleted script {} permission for resource {}",
                scriptName,
                resourceId);
    }
}
