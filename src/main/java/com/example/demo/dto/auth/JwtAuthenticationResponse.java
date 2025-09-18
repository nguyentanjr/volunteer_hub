package com.example.demo.dto.auth;

import com.example.demo.dto.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JwtAuthenticationResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private UserResponse userResponse;

    public JwtAuthenticationResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public JwtAuthenticationResponse(String accessToken, String refreshToken, UserResponse userResponse) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.userResponse = userResponse;
    }

}
