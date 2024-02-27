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
import com.pinterest.deployservice.bean.RatingBean;
import com.pinterest.deployservice.handler.RatingsHandler;
import com.pinterest.teletraan.TeletraanServiceContext;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

@Path("/v1/ratings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Ratings {
    private final static int DEFAULT_INDEX = 1;
    private final static int DEFAULT_SIZE = 30;
    private RatingsHandler ratingsHandler;

    public Ratings(@Context TeletraanServiceContext context) {
        ratingsHandler = new RatingsHandler(context);
    }

    @GET
    public List<RatingBean> getAll(@QueryParam("pageIndex") Optional<Integer> pageIndex,
        @QueryParam("pageSize") Optional<Integer> pageSize) throws Exception {
        return ratingsHandler.getRatingDAO().getRatingsInfos(pageIndex.or(DEFAULT_INDEX), pageSize.or(DEFAULT_SIZE));
    }

    @POST
    public Response create(@Valid RatingBean bean,
                           @Context SecurityContext sc,
                           @Context UriInfo uriInfo) throws Exception {
        bean.setAuthor(sc.getUserPrincipal().getName());
        bean.setTimestamp(System.currentTimeMillis());
        String id = ratingsHandler.createRating(bean);
        UriBuilder ub = uriInfo.getAbsolutePathBuilder();
        URI tokenUri = ub.path(id).build();
        String content = String.format("{\"id\": \"%s\"}", id);
        return Response.created(tokenUri).entity(content).build();
    }

    @GET
    @Path("/{userName : [a-zA-Z0-9\\-_.]+}/is_eligible")
    public Boolean checkUserFeedbackStatus(@PathParam("userName") String userName) throws Exception {
        return ratingsHandler.checkUserFeebackStatus(userName);
    }

    @DELETE
    @Path("/{id : [a-zA-Z0-9\\-_]+}")
    public void delete(@PathParam("id") String id, @Context SecurityContext sc) throws Exception {
        ratingsHandler.getRatingDAO().delete(id);
    }
}
