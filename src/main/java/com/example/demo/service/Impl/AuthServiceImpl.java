package com.example.demo.service.Impl;


import com.example.demo.dto.auth.JwtAuthenticationResponse;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.RegisterRequest;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.exception.EmailAlreadyExistsException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UsernameAlreadyExistsException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.RefreshToken;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JWTUtils;
import com.example.demo.service.AuthService;
import com.example.demo.service.RefreshTokenService;
import io.micrometer.common.util.StringUtils;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Override
    public JwtAuthenticationResponse register(@Valid @RequestBody RegisterRequest registerRequest) {

        log.info("Registering new user: {}", registerRequest.getUsername());
        if(userRepository.existsByUsername(registerRequest.getUsername())) {
            log.warn("Registration failed: username '{}' already exists", registerRequest.getUsername());
            throw new UsernameAlreadyExistsException("Username has already exists");
        }
        if(userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration failed: email '{}' already exists", registerRequest.getEmail());
            throw new EmailAlreadyExistsException("Email has already exists");
        }

        User user = userMapper.toUser(registerRequest);
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        User savedUser = userRepository.save(user);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(
                        savedUser, null, savedUser.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        log.info("User '{}' registered successfully",registerRequest.getUsername());

        String accessToken = jwtUtils.generateJwtToken(usernamePasswordAuthenticationToken);
        String refreshToken = refreshTokenService.createRefreshToken(savedUser).getToken();
        UserResponse userResponse = userMapper.toUserResponse(savedUser);

        return new JwtAuthenticationResponse(accessToken, refreshToken, userResponse);
    }

    @Override
    public JwtAuthenticationResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("User login: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken
                        (loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        
        User user = (User) authentication.getPrincipal();
        String accessToken = jwtUtils.generateJwtToken(authentication);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();
        UserResponse userResponse = userMapper.toUserResponse(user);

        return new JwtAuthenticationResponse(accessToken, refreshToken, userResponse);
    }

    @Override
    public JwtAuthenticationResponse refreshToken(String refreshTokenString) {
        log.info("Refresh Token request");

        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenString)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh Token not found!"));

        refreshToken = refreshTokenService.isRefreshTokenValid(refreshToken);

        String accessToken = jwtUtils.generateJwtTokenFromUsername(refreshToken.getUser().getUsername());
        String responseRefreshToken = refreshTokenService.rotateToken(refreshToken).getToken();
        UserResponse userResponse = userMapper.toUserResponse(refreshToken.getUser());

        return new JwtAuthenticationResponse(accessToken, responseRefreshToken, userResponse);
    }
}
