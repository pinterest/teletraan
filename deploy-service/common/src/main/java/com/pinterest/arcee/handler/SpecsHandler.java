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
package com.pinterest.arcee.handler;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.pinterest.arcee.bean.ImageBean;
import com.pinterest.arcee.bean.SpecBean;
import com.pinterest.arcee.dao.ImageDAO;
import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.common.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class SpecsHandler {
    private static final Logger LOG = LoggerFactory.getLogger(SpecsHandler.class);
    private static final String VPC_ID = "vpc-4ea63725";
    private static final String OWNER_ID = "998131032990";
    private ImageDAO imageDAO;
    private AmazonEC2Client client;

    public SpecsHandler(ServiceContext context) {
        client = context.getEc2Client();
        imageDAO = context.getImageDAO();
    }

    public List<SpecBean> getSecurityGroupsInfo() {
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest();
        List<String> values = new LinkedList<>();
        values.add(OWNER_ID);
        request.setFilters(Arrays.asList(new Filter("owner-id", values)));
        DescribeSecurityGroupsResult result = client.describeSecurityGroups(request);
        List<SecurityGroup> groups = result.getSecurityGroups();
        List<SpecBean> securityGroups = new ArrayList<>();
        for (SecurityGroup group : groups) {
            SpecBean specBean = new SpecBean();
            specBean.setSpecId(group.getGroupId());
            specBean.addInfo("name", group.getGroupName());
            securityGroups.add(specBean);
        }
        return securityGroups;
    }

    public void updateAllImages(Long after) {
        DescribeImagesRequest request = new DescribeImagesRequest();
        request.setOwners(Arrays.asList(OWNER_ID));
        Filter envFilter = new Filter("tag:environment", Arrays.asList("prod"));
        Filter archFilter = new Filter("tag:release", Arrays.asList("precise"));
        Filter vtypeFilter = new Filter("virtualization-type", Arrays.asList("hvm"));
        Filter rootType = new Filter("root-device-type", Arrays.asList("instance-store"));
        Filter archType = new Filter("architecture", Arrays.asList("x86_64"));
        request.setFilters(Arrays.asList(envFilter, archFilter, vtypeFilter, rootType, archType));
        DescribeImagesResult result = client.describeImages(request);
        List<Image> images = result.getImages();
        for (Image image : images) {
            try {
                ImageBean imageBean = new ImageBean();
                imageBean.setId(image.getImageId());
                Long createTime = getDateTime(image.getCreationDate());
                if (createTime <= after) {
                    continue;
                }
                imageBean.setPublish_date(getDateTime(image.getCreationDate()));
                List<Tag> tags = image.getTags();
                for (Tag tag : tags) {
                    if (tag.getKey().equals("application")) {
                        String app = tag.getValue();
                        imageBean.setApp_name(app);
                        break;
                    }
                }

                imageDAO.insertOrUpdate(imageBean);
            } catch (Exception ex) {
                LOG.error("Failed to get image info:", ex);
            }
        }
    }

    public List<SpecBean> getSubnets() {
        DescribeSubnetsRequest request = new DescribeSubnetsRequest();
        request.setFilters(Arrays.asList(new Filter[] {new Filter("VpcId", Arrays.asList(new String[] {VPC_ID}))}));
        DescribeSubnetsResult result = client.describeSubnets();
        List<Subnet> subnets = result.getSubnets();
        ArrayList<SpecBean> subnetsName = new ArrayList<>();
        for (Subnet subnet : subnets) {
            SpecBean specBean = new SpecBean();
            specBean.setSpecId(subnet.getSubnetId());

            specBean.addInfo("zone", subnet.getAvailabilityZone());

            if (subnet.getTags().isEmpty()) {
                specBean.addInfo("tag", "");
            } else {
                specBean.addInfo("tag", subnet.getTags().get(0).getValue());
            }
            subnetsName.add(specBean);
        }
        return subnetsName;
    }

    public List<String> getInstanceTypes() {
        List<String> instanceTypes = new LinkedList<>();
        for (InstanceType type : InstanceType.values()) {
            instanceTypes.add(type.toString());
        }
        return instanceTypes;
    }

    private long getDateTime(String dateString) {
        try {
            return CommonUtils.convertDateStringToMilliseconds(dateString);
        } catch (Exception ex) {
            return 0l;
        }
    }

}
