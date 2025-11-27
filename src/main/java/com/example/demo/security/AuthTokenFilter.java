package com.example.demo.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwtToken = parseJwt(request);
            if(jwtToken != null && jwtUtils.validateJwtToken(jwtToken)) {
                String username = jwtUtils.getUsernameFromJwtToken(jwtToken);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Can not set user authentication: " + e.getMessage());
        }
            filterChain.doFilter(request, response); // Bỏ qua các endpoint public
    }

    public String parseJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if(StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
