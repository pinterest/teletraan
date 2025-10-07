package com.pinterest.deployservice.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.Map;

@With
@AllArgsConstructor
@Data
@NoArgsConstructor
public class RodimusClusterBody {
  private String clusterName;
  private String cellName;
  private String archName;
  private int capacity;
  private String provider;
  private String baseImageId;
  private String baseImageName;
  private String hostType;
  private String securityZone;
  private String placement;
  private Map<String, String> configs;
  private String state;
  private String launchConfig;
  private boolean useLaunchTemplate;
  private String launchTemplateName;
  private boolean autoUpdateBaseImage;
  private Boolean useIdForBaseImageLookUp;
  private Boolean statefulStatus;
  private Boolean isManagedResource;
  private String managedResourceVersion;
  private long replacementTimeout;
}

