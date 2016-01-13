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

import com.pinterest.arcee.bean.SpecBean;
import com.pinterest.arcee.handler.SpecsHandler;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.TokenRolesBean;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.List;

@Path("/v1/specs")
@Api(tags = "Specs")
@SwaggerDefinition(
        tags = {
                @Tag(name = "Specs", description = "Spec info APIs"),
        }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Specs {
    private static final Logger LOG = LoggerFactory.getLogger(Specs.class);

    private SpecsHandler specsHandler;

    public Specs(ServiceContext context) {
        specsHandler = new SpecsHandler(context);
    }

    @GET
    @Path("/instance_types")
    @ApiOperation(
            value = "Get instance types",
            notes = "Returns a list of all instance types",
            response = String.class, responseContainer = "List")
    public List<String> getInstanceTypes() throws Exception {
        return specsHandler.getInstanceTypes();
    }

    @GET
    @Path("/security_groups")
    @ApiOperation(
            value = "Get all security groups",
            notes = "Returns a list of all security groups",
            response = String.class, responseContainer = "List")
    public List<SpecBean> getSecurityGroups() throws Exception {
        return specsHandler.getSecurityGroupsInfo();
    }

    @GET
    @Path("/subnets")
    @ApiOperation(
            value = "Get all subnets",
            notes = "Returns a list of all spec subnet info objects",
            response = SpecBean.class, responseContainer = "List")
    public List<SpecBean> getSubnets() throws Exception {
        return specsHandler.getSubnets();
    }
}
