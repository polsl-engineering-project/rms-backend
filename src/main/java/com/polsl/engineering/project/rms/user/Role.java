package com.polsl.engineering.project.rms.user;

import com.polsl.engineering.project.rms.security.UserPrincipal;

import java.util.List;

enum Role {
    ADMIN("ADMIN"),
    MANAGER("MANAGER"),
    WAITER("WAITER"),
    COOK("COOK"),
    DRIVER("DRIVER");

    private final String name;

    Role(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    UserPrincipal.Role toUserPrincipalRole() {
        return switch(this) {
            case ADMIN -> UserPrincipal.Role.ADMIN;
            case MANAGER -> UserPrincipal.Role.MANAGER;
            case WAITER -> UserPrincipal.Role.WAITER;
            case COOK -> UserPrincipal.Role.COOK;
            case DRIVER -> UserPrincipal.Role.DRIVER;
        };
    }

    private static Role fromUserPrincipal(UserPrincipal.Role role) {
        return switch (role) {
            case ADMIN -> ADMIN;
            case MANAGER -> MANAGER;
            case WAITER -> WAITER;
            case COOK -> COOK;
            case DRIVER -> DRIVER;
        };
    }

    static List<Role> fromUserPrincipalRoles(List<UserPrincipal.Role> roles) {
        return roles.stream()
                .map(Role::fromUserPrincipal)
                .toList();
    }

}
