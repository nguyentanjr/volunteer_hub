package com.example.demo.controller;

import com.example.demo.dto.auth.JwtAuthenticationResponse;
import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.user.UserLoginResponseDTO;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.CookieUtils;
import com.example.demo.security.JWTUtils;
import com.example.demo.service.AuthService;
import com.example.demo.service.Impl.CustomOauth2User;
import com.example.demo.service.Impl.OAuth2UserServiceImpl;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.service.UserService;
import io.jsonwebtoken.Jwt;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
public class OAuth2Controller {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CookieUtils cookieUtils;

    @GetMapping("/token")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> getToken(@AuthenticationPrincipal OAuth2User oAuth2User,
                                                                           HttpServletResponse response) {
        if(oAuth2User == null) {
            throw new OAuth2AuthenticationException("User is not authenticated");
        }

        try {
            User user = extractUserFromOauth2User(oAuth2User);
            if(user == null) {
                log.error("Extract user failed");
                return ResponseEntity.ok(ApiResponse.error("Extract user failed", HttpStatus.BAD_REQUEST));
            }

            String accessToken = jwtUtils.generateJwtTokenFromUsername(user.getUsername());
            String refreshToken = refreshTokenService.createRefreshToken(user).getToken();
            UserLoginResponseDTO userResponse = userMapper.toUserLoginResponseDTO(user);
            JwtAuthenticationResponse authResponse = new JwtAuthenticationResponse(accessToken, refreshToken, userResponse);
            cookieUtils.createRefreshTokenCookie(response,refreshToken);

            ApiResponse<JwtAuthenticationResponse> apiResponse =  ApiResponse.created(authResponse);
            return ResponseEntity.ok(apiResponse);
        } catch (Exception e) {
            log.warn("Failed to generate JWT token for user");
            return ResponseEntity.ok(ApiResponse.error("Failed to generate JWT Token", HttpStatus.BAD_REQUEST));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public User extractUserFromOauth2User(OAuth2User oAuth2User) throws Throwable {
        String username =(String) oAuth2User.getAttribute("email");
        username = username.split("@")[0];
       return userRepository.findUserByUsername(username).orElseThrow(null);

    }

}
