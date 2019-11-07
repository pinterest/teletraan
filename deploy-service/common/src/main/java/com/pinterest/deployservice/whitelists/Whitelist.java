package com.pinterest.deployservice.whitelists;

public interface Whitelist {
    Boolean approved(String attempt);
}