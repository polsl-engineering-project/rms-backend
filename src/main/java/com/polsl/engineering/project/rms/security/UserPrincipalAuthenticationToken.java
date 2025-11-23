package com.polsl.engineering.project.rms.security;

import com.polsl.engineering.project.rms.general.exception.IllegalActionException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public record UserPrincipalAuthenticationToken(UserPrincipal userPrincipal) implements Authentication {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return userPrincipal.roles();
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userPrincipal;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        throw new IllegalActionException("UserPrincipal is immutable");
    }

    @Override
    public String getName() {
        return userPrincipal.id().toString();
    }
}
