package com.pinterest.teletraan.universal.security.bean;

public class ValueBasedRole implements Role<ValueBasedRole> {
    private int value;

    ValueBasedRole(int value) {
        this.value = value;
    }

    @Override
    public boolean isEqualOrSuperior(ValueBasedRole requiredRole) {
        return this.value >= requiredRole.value;
    }
}
