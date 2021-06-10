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
import com.pinterest.deployservice.bean.ServiceBean;
import com.pinterest.deployservice.bean.Resource;
import com.pinterest.deployservice.bean.Role;
import com.pinterest.deployservice.bean.UserRolesBean;
import com.pinterest.deployservice.dao.ServiceDAO;
import com.pinterest.deployservice.dao.UserRolesDAO;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.exception.TeletaanInternalException;
import com.pinterest.teletraan.security.Authorizer;
import com.pinterest.teletraan.security.OpenAuthorizer;
import io.swagger.annotations.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Path("/v1/services")
@Api(tags = "Services")
@SwaggerDefinition(
        tags = {
                @Tag(name = "Services", description = "Service info APIs"),
        }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Services {
    public enum ActionType {
        ENABLE,
        DISABLE
    }

    private static final Logger LOG = LoggerFactory.getLogger(Services.class);
    private final static int DEFAULT_INDEX = 1;
    private final static int DEFAULT_SIZE = 30;
    private ServiceDAO serviceDAO;
    private UserRolesDAO userRolesDAO;
    private final Authorizer authorizer;

    @Context
    UriInfo uriInfo;

    public Services(TeletraanServiceContext context) throws Exception {
        serviceDAO = context.getServiceDAO();
        userRolesDAO = context.getUserRolesDAO();
        authorizer = context.getAuthorizer();
    }

    @GET
    @Path("/{name : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get service object",
            notes = "Returns an service object given an service id",
            response = ServiceBean.class)
    public ServiceBean get(
            @ApiParam(value = "Service name", required = true)@PathParam("name") String name) throws Exception {
                ServiceBean serviceBean = serviceDAO.get(name);
        if (serviceBean == null) {
            throw new TeletaanInternalException(Response.Status.NOT_FOUND,
                String.format("Service %s does not exist.", name));
        }
        return serviceBean;
    }

    @GET
    @Path("/names")
    public List<String> get(@QueryParam("nameFilter") Optional<String> nameFilter,
        @QueryParam("pageIndex") Optional<Integer> pageIndex,
        @QueryParam("pageSize") Optional<Integer> pageSize) throws Exception {
        return serviceDAO.getAll(nameFilter.or(""), pageIndex.or(DEFAULT_INDEX),
            pageSize.or(DEFAULT_SIZE));
    }

    @PUT
    @ApiOperation(
            value = "Update a service",
            notes = "Update a service given service name with a service object")
    public void update(@Context SecurityContext sc,
                       @ApiParam(value = "Service name", required = true)@PathParam("name") String svcName,
                       @ApiParam(value = "Desired Service object with updates", required = true)
                           ServiceBean serviceBean) throws Exception {
        ServiceBean origBean = serviceDAO.get(svcName);
        // Only System admin can modify
        authorizer.authorize(sc, new Resource(Resource.ALL, Resource.Type.SYSTEM), Role.ADMIN);

        return serviceDAO.insertOrUpdate(serviceBean);

}
