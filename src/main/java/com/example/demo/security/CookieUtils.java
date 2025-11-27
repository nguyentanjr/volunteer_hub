package com.example.demo.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
@Component
public class CookieUtils {

    public void createRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", refreshToken)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());

        log.debug("Created refresh token cookie using ResponseCookie");
    }

    public Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        if(request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals("RefreshToken"))
                .map(Cookie::getValue)
                .findFirst();
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("RefreshToken", "")
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());

        log.debug("Deleted refresh token cookie using ResponseCookie");
    }


}


