package com.pinterest.teletraan.universal.security.bean;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EnvoyCredentials {
    String user;
    String spiffeId;
    List<String> groups;
}
