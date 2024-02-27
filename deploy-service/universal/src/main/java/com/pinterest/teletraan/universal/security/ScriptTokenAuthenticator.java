/**
 * Copyright (c) 2024, Pinterest Inc. All rights reserved.
 */
package com.pinterest.teletraan.universal.security;

import com.pinterest.teletraan.universal.security.bean.Role;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptTokenAuthenticator<R extends Role<R>>
        implements Authenticator<String, ServicePrincipal<R>> {
    private static final Logger LOG = LoggerFactory.getLogger(ScriptTokenAuthenticator.class);

    private ScriptTokenProvider<R> tokenProvider;

    public ScriptTokenAuthenticator(ScriptTokenProvider<R> tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Optional<ServicePrincipal<R>> authenticate(String credentials)
            throws AuthenticationException {
        LOG.debug("Authenticating...");
        return Optional.ofNullable(tokenProvider.getPrincipal(credentials));
    }
}
