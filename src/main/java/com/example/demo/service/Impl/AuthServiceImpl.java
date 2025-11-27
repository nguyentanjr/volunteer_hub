package com.example.demo.service.Impl;


import com.example.demo.dto.auth.JwtAuthenticationResponse;
import com.example.demo.dto.auth.LoginRequest;
import com.example.demo.dto.auth.RegisterRequest;
import com.example.demo.dto.user.UserLoginResponseDTO;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.exception.EmailAlreadyExistsException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.exception.UsernameAlreadyExistsException;
import com.example.demo.exception.UsernameNotFoundException;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.PasswordResetToken;
import com.example.demo.model.RefreshToken;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JWTUtils;
import com.example.demo.service.AuthService;
import com.example.demo.service.PasswordResetTokenService;
import com.example.demo.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JWTUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final JavaMailSender javaMailSender;
    private final PasswordResetTokenService passwordResetTokenService;
    private final RoleRepository roleRepository;


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
        
        Role volunteerRole = roleRepository.findByName(Role.RoleName.VOLUNTEER)
                .orElseThrow(() -> new ResourceNotFoundException("Role VOLUNTEER not found"));
        user.addRole(volunteerRole);
        user.setActiveRole(Role.RoleName.VOLUNTEER);

        User savedUser = userRepository.save(user);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(
                        savedUser, null, savedUser.getAuthorities()
                );
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        log.info("User '{}' registered successfully",registerRequest.getUsername());

        String accessToken = jwtUtils.generateJwtToken(usernamePasswordAuthenticationToken);
        String refreshToken = refreshTokenService.createRefreshToken(savedUser).getToken();
        UserLoginResponseDTO userResponse = userMapper.toUserLoginResponseDTO(savedUser);

        return new JwtAuthenticationResponse(accessToken, refreshToken, userResponse);
    }

    @Override
    public JwtAuthenticationResponse login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("User login: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken
                        (loginRequest.getUsername(), loginRequest.getPassword()));
        log.info("Set authentication in security context");
        SecurityContextHolder.getContext().setAuthentication(authentication);


        User user = (User) authentication.getPrincipal();
        String accessToken = jwtUtils.generateJwtToken(authentication);
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();
        UserLoginResponseDTO userResponse = userMapper.toUserLoginResponseDTO(user);

        return new JwtAuthenticationResponse(accessToken, refreshToken, userResponse);
    }

    @Override
    public void logout(String refreshTokenString) {

        log.info("Logout request");

        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenString)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh Token not found"));

        refreshTokenService.revokeToken(refreshToken);

        log.info("User logout successfully: {}", refreshToken.getUser().getUsername());
    }

    @Override
    public JwtAuthenticationResponse refreshToken(String refreshTokenString) {
        log.info("Refresh Token request");

        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenString)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh Token not found!"));

        refreshToken = refreshTokenService.isRefreshTokenValid(refreshToken);

        String accessToken = jwtUtils.generateJwtTokenFromUsername(refreshToken.getUser().getUsername());
        String responseRefreshToken = refreshTokenService.rotateToken(refreshToken).getToken();
        UserLoginResponseDTO userResponse = userMapper.toUserLoginResponseDTO(refreshToken.getUser());

        return new JwtAuthenticationResponse(accessToken, responseRefreshToken, userResponse);
    }

    @Override
    public void sendResetPasswordRequest(String email) {
        Optional<User> userOptional = userRepository.findUserByEmail(email);

        if (userOptional.isEmpty()) {
            throw new UsernameNotFoundException("Email not found: " + email);
        }

        User user = userOptional.get();

        PasswordResetToken passwordResetToken = passwordResetTokenService.generatePasswordResetToken(user);
        String token = passwordResetToken.getToken();

        String url = "http://localhost:8080/api/v1/auth/password/validate?token=" + token;
        String subject = "Reset your password";
        String body = "Hi " + user.getUsername() + ",\n\n" +
                "Click the link below to reset your password:\n" +
                url + "\n\n" +
                "If you did not request this, please ignore this email.";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject(subject);
        message.setText(body);

        javaMailSender.send(message);
    }

}
