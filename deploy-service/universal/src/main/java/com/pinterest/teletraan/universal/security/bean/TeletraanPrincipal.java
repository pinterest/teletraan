package com.pinterest.teletraan.universal.security.bean;

import java.security.Principal;
import java.util.List;

public interface TeletraanPrincipal extends Principal {
    List<String> getGroups();
}
