package com.polsl.engineering.project.rms.user;

public enum Role {
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
}
