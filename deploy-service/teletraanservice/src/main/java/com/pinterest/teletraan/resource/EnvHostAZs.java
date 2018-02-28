package com.pinterest.teletraan.resource;

import com.google.common.base.Optional;
import com.pinterest.deployservice.bean.*;
import com.pinterest.deployservice.dao.EnvironDAO;
import com.pinterest.deployservice.dao.HostDAO;
import com.pinterest.deployservice.dao.HostTagDAO;
import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.security.Authorizer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.*;


@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/host_availability_zones")
@Api(tags = "Hosts Availability Zones")
@SwaggerDefinition(
    tags = {
        @Tag(name = "Host Availability Zones", description = "Host Availability Zones related APIs"),
    }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvHostAZs {
    public static final String AVAILABILITY_ZONE_RESERVED_TAG_NAME = "availability_zone";

    private static final Logger LOG = LoggerFactory.getLogger(EnvHostAZs.class);
    private HostTagDAO hostTagDAO;
    private EnvironDAO environDAO;
    private HostDAO hostDAO;
    private final RodimusManager rodimusManager;
    private Authorizer authorizer;

    public EnvHostAZs(TeletraanServiceContext context) {
        hostDAO = context.getHostDAO();
        hostTagDAO = context.getHostTagDAO();
        environDAO = context.getEnvironDAO();
        authorizer = context.getAuthorizer();
        rodimusManager = context.getRodimusManager();
    }
    @GET
    @ApiOperation(
        value = "List all the hosts tags",
        notes = "Returns a map group by tagValue and hosts tagged with tagName:tagValue in an environment",
        response = HostTagInfo.class, responseContainer = "Collection")
    public Collection<HostTagInfo> get(@PathParam("envName") String envName,
                                       @PathParam("stageName") String stageName,
                                       @QueryParam("ec2AZs") Optional<Boolean> ec2AZs,
                                       @Context SecurityContext sc) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);

        Boolean loadEC2AZs = ec2AZs.or(false);
        if (loadEC2AZs) {
            // read ec2 tags from CMDB
            return remoteQueryHostAZs(envBean);
        }
        return hostTagDAO.getHostsByEnvId(envBean.getEnv_id());
    }


    private Collection<HostTagInfo> remoteQueryHostAZs(EnvironBean environBean) throws Exception {
        Collection<HostBean> hostsByEnvId = hostDAO.getHostsByEnvId(environBean.getEnv_id());
        Map<String, String> hostId2HostName = new HashMap<String, String>();
        for (HostBean hostBean : hostsByEnvId) {
            if (hostBean.getHost_id() != null) {
                hostId2HostName.put(hostBean.getHost_id(), hostBean.getHost_name());
            }
        }
        Set<String> hostIds = hostId2HostName.keySet();
        LOG.info(String.format("Env %s start get all availability zone tags for host_ids %s", environBean.getEnv_id(), hostIds));
        Map<String, List<String>> az2HostIdsMap = rodimusManager.getAvailabilityZones(hostIds);
        LOG.info(String.format("Env %s host availability zone tags results: %s", environBean.getEnv_id(), az2HostIdsMap));

        Collection<HostTagInfo> rs = new ArrayList<>();
        if (az2HostIdsMap != null) {
            for (String az : az2HostIdsMap.keySet()) {
                List<String> sameAZHostIds = az2HostIdsMap.get(az);
                for(String hostId: sameAZHostIds) {
                    HostTagInfo hostTagInfo = new HostTagInfo();
                    hostTagInfo.setHostId(hostId);
                    hostTagInfo.setHostName(hostId2HostName.get(hostId));
                    hostTagInfo.setTagValue(az);
                    hostTagInfo.setTagName(AVAILABILITY_ZONE_RESERVED_TAG_NAME);
                    rs.add(hostTagInfo);
                }
            }
        }
        return rs;

    }
}
