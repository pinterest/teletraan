package com.pinterest.teletraan.resource;

import javax.annotation.security.PermitAll;

import io.swagger.jaxrs.listing.ApiListingResource;

@PermitAll
public class SecureApiListingResource extends ApiListingResource {}
