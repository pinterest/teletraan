package com.pinterest.teletraan.universal.security.bean;

public class MaskBasedRole implements Role<MaskBasedRole> {
    private long mask;

    public MaskBasedRole(long mask) {
        this.mask = mask;
      }

    public long getMask() {
        return mask;
    }

    public String getName() {
        return String.valueOf(mask);
    }

    @Override
    public boolean isEqualOrSuperior(MaskBasedRole requiredRole) {
        return (this.mask & requiredRole.mask) == mask;
    }
}
