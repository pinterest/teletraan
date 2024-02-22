package com.pinterest.teletraan.universal.security;

import com.pinterest.teletraan.universal.security.bean.Role;
import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;

public interface ScriptTokenProvider<R extends Role<R>> {
    ServicePrincipal<R> getPrincipal(String token);
}