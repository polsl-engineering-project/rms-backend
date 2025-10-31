package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.security.UserPrincipal;

enum Role {
    ADMIN("ADMIN"),
    MANAGER("MANAGER"),
    WAITER("WAITER"),
    COOK("COOK");

    private final String name;

    Role(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public UserPrincipal.Role toUserPrincipalRole() {
        return switch(this) {
            case ADMIN -> UserPrincipal.Role.ADMIN;
            case MANAGER -> UserPrincipal.Role.MANAGER;
            case WAITER -> UserPrincipal.Role.WAITER;
            case COOK -> UserPrincipal.Role.COOK;
        };
    }

}
