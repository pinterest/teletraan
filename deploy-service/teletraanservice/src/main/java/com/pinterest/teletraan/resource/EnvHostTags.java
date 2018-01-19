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


@Path("/v1/envs/{envName : [a-zA-Z0-9\\-_]+}/{stageName : [a-zA-Z0-9\\-_]+}/host_tags")
@Api(tags = "Hosts Tags")
@SwaggerDefinition(
    tags = {
        @Tag(name = "Host Tags", description = "Host Tags related APIs"),
    }
)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EnvHostTags {
    private static final Logger LOG = LoggerFactory.getLogger(EnvHostTags.class);
    private HostTagDAO hostTagDAO;
    private EnvironDAO environDAO;
    private HostDAO hostDAO;
    private final RodimusManager rodimusManager;
    private Authorizer authorizer;


    public EnvHostTags(TeletraanServiceContext context) {
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
        response = HostTagInfo.class)
    public Collection<HostTagInfo> get(@PathParam("envName") String envName,
                                                    @PathParam("stageName") String stageName,
                                                    @QueryParam("ec2Tags") Optional<Boolean> ecTags,
                                                    @Context SecurityContext sc) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);

        Boolean loadEc2Tags = ecTags.or(false);
        if (loadEc2Tags) {
            // read ec2 tags from CMDB
            return remoteQueryHostEc2Tags(envBean);
        }
        List<HostTagInfo> hostTagInfos = hostTagDAO.getHostsByEnvId(envBean.getEnv_id());
        return hostTagInfos;
    }


    @GET
    @Path("/{tagName : [a-zA-Z0-9\\-:_]+}")
    @ApiOperation(
        value = "List all the hosts that are tagged with tagName in an environment, and group by tagValue",
        notes = "Returns a map group by tagValue and hosts tagged with tagName:tagValue in an environment",
        response = HostTagInfo.class)
    public Map<String, Collection<HostTagInfo>> get(@PathParam("envName") String envName,
                                                    @PathParam("stageName") String stageName,
                                                    @PathParam("tagName") String tagName,
                                                    @QueryParam("ec2Tags") Optional<Boolean> ecTags,
                                                    @Context SecurityContext sc) throws Exception {
        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);

        Boolean loadEc2Tags = ecTags.or(false);
        if (loadEc2Tags) {
            // read ec2 tags from CMDB
            return remoteQueryHostEc2Tags(envBean, tagName);
        }
        List<HostTagInfo> hostTagInfos = hostTagDAO.getHostsByEnvIdAndTagName(envBean.getEnv_id(), tagName);
        Map<String, Collection<HostTagInfo>> rs = new HashMap<String, Collection<HostTagInfo>>();
        for (HostTagInfo hostTagInfo : hostTagInfos) {
            String tagValue = hostTagInfo.getTagValue();
            if (tagValue == null) {
                continue;
            }
            addToHostTagMap(hostTagInfo, rs);
        }
        return rs;
    }


    @DELETE
    @Path("/{tagName : [a-zA-Z0-9\\-:_]+}")
    public void removeHostTags(@PathParam("envName") String envName,
                               @PathParam("stageName") String stageName,
                               @PathParam("tagName") String tagName,
                               @Context SecurityContext sc) throws Exception {

        EnvironBean envBean = Utils.getEnvStage(environDAO, envName, stageName);
        authorizer.authorize(sc, new Resource(envBean.getEnv_name(), Resource.Type.ENV), Role.OPERATOR);
        String envId = envBean.getEnv_id();
        hostTagDAO.deleteAllByEnvId(envId, tagName);
        LOG.info("Successfully removed all host tagged by {} in env {}", tagName, envId);
    }


    private Collection<HostTagInfo> remoteQueryHostEc2Tags(EnvironBean environBean) throws Exception {
        Collection<HostBean> hostsByEnvId = hostDAO.getHostsByEnvId(environBean.getEnv_id());
        Map<String, String> hostId2HostName = new HashMap<String, String>();
        for (HostBean hostBean : hostsByEnvId) {
            if (hostBean.getHost_id() != null) {
                hostId2HostName.put(hostBean.getHost_id(), hostBean.getHost_name());
            }
        }
        Set<String> hostIds = hostId2HostName.keySet();
        LOG.info(String.format("Env %s start get all ec2 tags for host_ids %s", environBean.getEnv_id(), hostIds));
        Map<String, Map<String, String>> hostEc2Tags = rodimusManager.getEc2Tags(hostIds);
        LOG.info(String.format("Env %s host ec2 tags results: %s", environBean.getEnv_id(), hostEc2Tags));

        Collection<HostTagInfo> rs = new ArrayList<HostTagInfo>();
        if (hostEc2Tags != null) {
            for (String hostId : hostEc2Tags.keySet()) {
                Map<String, String> ec2Tags = hostEc2Tags.get(hostId);
                for(Map.Entry<String, String> entry: ec2Tags.entrySet()) {
                    HostTagInfo hostTagInfo = new HostTagInfo();
                    hostTagInfo.setHostId(hostId);
                    hostTagInfo.setHostName(hostId2HostName.get(hostId));
                    hostTagInfo.setTagValue(entry.getValue());
                    hostTagInfo.setTagName(entry.getKey());
                    rs.add(hostTagInfo);
                }
            }
        }
        return rs;
    }

    private Map<String, Collection<HostTagInfo>> remoteQueryHostEc2Tags(EnvironBean environBean, String tagName) throws Exception {
        Collection<HostBean> hostsByEnvId = hostDAO.getHostsByEnvId(environBean.getEnv_id());
        Map<String, String> hostId2HostName = new HashMap<String, String>();
        for (HostBean hostBean : hostsByEnvId) {
            if (hostBean.getHost_id() != null) {
                hostId2HostName.put(hostBean.getHost_id(), hostBean.getHost_name());
            }
        }
        Set<String> hostIds = hostId2HostName.keySet();
        LOG.info(String.format("Env %s start get ec2 tags %s for host_ids %s", environBean.getEnv_id(), tagName, hostIds));
        Map<String, Map<String, String>> hostEc2Tags = rodimusManager.getEc2Tags(hostIds);
        LOG.info(String.format("Env %s host ec2 tags %s results: %s", environBean.getEnv_id(), tagName, hostEc2Tags));

        Map<String, Collection<HostTagInfo>> rs = new HashMap<String, Collection<HostTagInfo>>();

        if (hostEc2Tags == null) {
            return rs;
        }
        for (String hostId : hostEc2Tags.keySet()) {
            Map<String, String> ec2Tags = hostEc2Tags.get(hostId);
            if (ec2Tags == null) {
                continue;
            }
            if (ec2Tags.containsKey(tagName)) {
                String tagValue = ec2Tags.get(tagName);
                if (tagValue == null) {
                    continue;
                }
                HostTagInfo hostTagInfo = new HostTagInfo();
                hostTagInfo.setHostId(hostId);
                hostTagInfo.setHostName(hostId2HostName.get(hostId));
                hostTagInfo.setTagValue(tagValue);
                hostTagInfo.setTagName(tagName);
                addToHostTagMap(hostTagInfo, rs);
            }
        }
        LOG.info(String.format("Env %s successfully found matched ec2 tag %s result: %s", environBean.getEnv_id(), tagName, rs));
        return rs;
    }


    private Map<String, Collection<HostTagInfo>> addToHostTagMap(HostTagInfo hostTagInfo, Map<String, Collection<HostTagInfo>> rs) {
        Collection<HostTagInfo> hostsTaggedWithValue;
        String tagValue = hostTagInfo.getTagValue();
        if (rs.containsKey(tagValue)) {
            hostsTaggedWithValue = rs.get(tagValue);
        } else {
            hostsTaggedWithValue = new ArrayList<HostTagInfo>();
        }
        hostsTaggedWithValue.add(hostTagInfo);
        rs.put(tagValue, hostsTaggedWithValue);
        return rs;
    }
}
