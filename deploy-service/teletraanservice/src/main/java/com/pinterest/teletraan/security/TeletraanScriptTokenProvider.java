package com.pinterest.teletraan.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ScriptTokenProvider;
import com.pinterest.teletraan.universal.security.bean.PrincipalRoles;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;

@Deprecated
public class TeletraanScriptTokenProvider implements ScriptTokenProvider {
    private static final Logger LOG = LoggerFactory.getLogger(TeletraanScriptTokenProvider.class);

    private TeletraanServiceContext context;

    public TeletraanScriptTokenProvider(TeletraanServiceContext context) {
        this.context = context;
    }

    @Override
    public ServicePrincipal getPrincipal(String token) {
        try {
            TokenRolesBean tokenRolesBean = context.getTokenRolesDAO().getByToken(token);

            if (tokenRolesBean != null) {
                PrincipalRoles role = PrincipalRoles.valueOf(tokenRolesBean.getRole().name());
                return new ServicePrincipal(tokenRolesBean.getScript_name(), role, null);
            }
        } catch (Exception e) {
            LOG.error("failed to get Script token principal", e);
        }
        return null;
    }
}
