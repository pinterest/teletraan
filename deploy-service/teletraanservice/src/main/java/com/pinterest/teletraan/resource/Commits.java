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

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;

import com.google.common.base.Optional;
import com.pinterest.deployservice.bean.CommitBean;
import com.pinterest.deployservice.scm.SourceControlManager;
import com.pinterest.teletraan.TeletraanServiceContext;
import io.swagger.annotations.*;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/v1/commits")
@Api(tags="Commits")
@SwaggerDefinition(
        tags = {
                @Tag(name = "Commits", description = "Commit info APIs"),
        }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class Commits {
    private final static String DEFAULT_PATH = "";
    private final static int DEFAULT_SIZE = 30;
    private SourceControlManager sourceControlManager;

    public Commits(TeletraanServiceContext context) throws Exception {
        sourceControlManager = context.getSourceControlManager();
    }

    public class CustomThrowable extends Throwable {
        public CustomThrowable(String message, Throwable cause) {
            super(message, cause, true, true);
        }
    }

    @GET
    @Timed
    @ExceptionMetered
    @Path("{repo : [a-zA-Z0-9\\-_]+}/{sha : [a-zA-Z0-9\\-_]+}")
    @ApiOperation(
            value = "Get commit infos",
            notes = "Returns a commit object given a repo and commit sha",
            response = CommitBean.class)
    public CommitBean getCommit(
            @ApiParam(value = "Commit's repo", required = true)@PathParam("repo") String repo,
            @ApiParam(value = "Commit SHA", required = true)@PathParam("sha") String sha) throws Throwable {
            try {
                return sourceControlManager.getCommit(repo, sha);
            } catch(Exception e) {
                CustomThrowable customThrowable = new CustomThrowable(e.getMessage(), e);
                throw customThrowable.fillInStackTrace();
            }
    }

    /**
     * Returns a list of CommitInfo from startSha inclusive to endSha exclusive,
     * or up to the specified size, whichever happens first;
     * if size == 0, then will return the full list until endSha
     * if endSha == null, then will return up to size, max_size = 500
     * It is recommended to call multiple times (pagination) with size < 30 to avoid timeout
     */
    @GET
    @Timed
    @ExceptionMetered
    public List<CommitBean> getCommits(@QueryParam("repo") String repo,
        @QueryParam("startSha") String startSha, @QueryParam("endSha") String endSha,
        @QueryParam("size") Optional<Integer> size, @QueryParam("path") Optional<String> path) throws Throwable {
        try {
            return sourceControlManager.getCommits(repo, startSha, endSha, size.or(DEFAULT_SIZE), path.or(DEFAULT_PATH));
        } catch(Exception e) {
            CustomThrowable customThrowable = new CustomThrowable(e.getMessage(), e);
            throw customThrowable.fillInStackTrace();
        }
    }
}
