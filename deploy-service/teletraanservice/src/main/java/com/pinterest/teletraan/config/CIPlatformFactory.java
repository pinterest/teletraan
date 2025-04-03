package com.pinterest.teletraan.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.pinterest.deployservice.ci.CIPlatformManager;

import io.dropwizard.jackson.Discoverable;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = JenkinsFactory.class, name = "jenkins"),
    @JsonSubTypes.Type(value = BuildkiteFactory.class, name = "buildkite")
})
public interface CIPlatformFactory extends Discoverable {
    CIPlatformManager create() throws Exception;
    
}
