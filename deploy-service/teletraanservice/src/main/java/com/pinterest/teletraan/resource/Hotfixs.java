/**
 * Copyright 2016 Pinterest, Inc.
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

import com.google.common.base.Optional;
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.BuildDAO;
import com.pinterest.deployservice.dao.DeployDAO;
import com.pinterest.deployservice.dao.HotfixDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.deployservice.exception.TeletaanInternalException;
import com.pinterest.teletraan.security.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/v1/hotfixs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Hotfixs {
    private static final Logger LOG = LoggerFactory.getLogger(Hotfixs.class);
    private final static int DEFAULT_TIME_OUT = 30; // minutes
    private final static int DEFAULT_SIZE = 30;
    private DeployDAO deployDAO;
    private BuildDAO buildDAO;
    private HotfixDAO hotfixDAO;
    private final Authorizer authorizer;

    public Hotfixs(TeletraanServiceContext context) {
        deployDAO = context.getDeployDAO();
        buildDAO = context.getBuildDAO();
        hotfixDAO = context.getHotfixDAO();
        authorizer = context.getAuthorizer();
    }

    private HotfixBean getHotfixBean(String id) throws Exception {
        HotfixBean hotfixBean = hotfixDAO.getByHotfixId(id);
        if (hotfixBean == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND, String.format("Hotfix %s does not exist.", id));
        }
        return hotfixBean;
    }

    @GET
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    public HotfixBean get(@PathParam("id") String id) throws Exception {
        return getHotfixBean(id);
    }

    @GET
    public List<HotfixBean> getAll(@QueryParam("envName") String envName,
        @QueryParam("pageIndex") Optional<Integer> pageIndex,
        @QueryParam("pageSize") Optional<Integer> pageSize) throws Exception {
        return hotfixDAO.getHotfixes(envName, pageIndex.or(1), pageSize.or(DEFAULT_SIZE));
    }

    @PUT
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    public void update(@Context SecurityContext sc, @PathParam("id") String id,
        HotfixBean hotfixBean) throws Exception {
        authorizer.authorize(sc, new Resource(Resource.ALL, Resource.Type.SYSTEM), Role.PUBLISHER);
        hotfixDAO.update(id, hotfixBean);
        LOG.info("Successfully updated hotfix {} with {}.", id, hotfixBean);
    }

    private String generateJobName(String repo) {
        String[] repoSplit = repo.split("/");
        if (repoSplit.length > 1) {
            // Special case for Github repo to extract repo name from org
            repo = repoSplit[1];
        }
        return String.format("%s-hotfix-job", repo);
    }

    @POST
    public Response create(@Context SecurityContext sc,
                           @Context UriInfo uriInfo,
                           @Valid HotfixBean hotfixBean) throws Exception {
        authorizer.authorize(sc, new Resource(hotfixBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String hotfixId = CommonUtils.getBase64UUID();
        hotfixBean.setId(hotfixId);
        hotfixBean.setState(HotfixState.INITIAL);

        DeployBean deploy = deployDAO.getById(hotfixBean.getBase_deploy());
        BuildBean build = buildDAO.getById(deploy.getBuild_id());
        hotfixBean.setRepo(build.getScm_repo());
        hotfixBean.setJob_name(generateJobName(build.getScm_repo()));

        hotfixBean.setTimeout(DEFAULT_TIME_OUT);
        hotfixBean.setOperator(sc.getUserPrincipal().getName());

        long now = System.currentTimeMillis();
        hotfixBean.setStart_time(now);
        hotfixBean.setLast_worked_on(now);
        hotfixBean.setProgress(0);

        hotfixDAO.insert(hotfixBean);
        LOG.info("Successfully created hotfix {}.", hotfixId);

        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI buildUri = ub.path(hotfixId).build();
        hotfixBean = hotfixDAO.getByHotfixId(hotfixId);
        return Response.created(buildUri).entity(hotfixBean).build();
    }
}
