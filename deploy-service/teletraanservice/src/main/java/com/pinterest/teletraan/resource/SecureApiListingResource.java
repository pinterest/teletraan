package com.pinterest.teletraan.resource;

import javax.annotation.security.RolesAllowed;

import com.pinterest.deployservice.bean.TeletraanPrincipalRole;
import io.swagger.jaxrs.listing.ApiListingResource;

@RolesAllowed({TeletraanPrincipalRole.Names.READ, TeletraanPrincipalRole.Names.READER})
public class SecureApiListingResource extends ApiListingResource {}
