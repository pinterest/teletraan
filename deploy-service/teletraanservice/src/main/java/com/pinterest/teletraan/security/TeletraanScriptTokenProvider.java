package com.pinterest.teletraan.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.bean.TokenRolesBean;
import com.pinterest.teletraan.TeletraanServiceContext;
import com.pinterest.teletraan.universal.security.ScriptTokenProvider;
import com.pinterest.teletraan.universal.security.bean.ValueBasedRole;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;

@Deprecated
public class TeletraanScriptTokenProvider implements ScriptTokenProvider<ValueBasedRole> {
    private static final Logger LOG = LoggerFactory.getLogger(TeletraanScriptTokenProvider.class);

    private TeletraanServiceContext context;

    public TeletraanScriptTokenProvider(TeletraanServiceContext context) {
        this.context = context;
    }

    @Override
    public ServicePrincipal<ValueBasedRole> getPrincipal(String token) {
        try {
            TokenRolesBean tokenRolesBean = context.getTokenRolesDAO().getByToken(token);

            if (tokenRolesBean != null) {
                return new ServicePrincipal<ValueBasedRole>(tokenRolesBean.getScript_name(), tokenRolesBean.getRole().getRole(), null);
            }
        } catch (Exception e) {
            LOG.error("failed to get Script token principal", e);
        }
        return null;
    }
}
