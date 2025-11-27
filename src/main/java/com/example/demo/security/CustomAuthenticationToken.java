package com.example.demo.security;

import com.example.demo.service.Impl.CustomOauth2User;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class CustomAuthenticationToken extends AbstractAuthenticationToken {

    private final CustomOauth2User principal;

    public CustomAuthenticationToken(CustomOauth2User principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true); // Mark as authenticated
    }

    @Override
    public Object getCredentials() {
        return null; // No credentials are used in this token
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
