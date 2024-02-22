package com.pinterest.teletraan.universal.security;

import com.pinterest.teletraan.universal.security.bean.ServicePrincipal;

public interface ScriptTokenProvider {
    ServicePrincipal getPrincipal(String token);
}