package com.pinterest.deployservice.allowlists;

public interface Allowlist {
    Boolean approved(String attempt);
    Boolean trusted(String attempt);
}