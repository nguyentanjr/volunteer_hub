package com.example.demo.config;

import ch.qos.logback.core.pattern.util.AlmostAsIsEscapeUtil;
import com.example.demo.security.AuthTokenFilter;
import com.example.demo.security.CustomAuthenticationToken;
import com.example.demo.service.Impl.CustomOauth2User;
import com.example.demo.service.Impl.OAuth2UserServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Autowired
    private OAuth2UserServiceImpl oAuth2UserService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        http
                  .cors(cors -> {})
//                .csrf(csrf -> csrf
//                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                        .csrfTokenRequestHandler(requestHandler)
//                        .ignoringRequestMatchers("/api/v1/auth/login", "/api/v1/auth/register","/api/v1/auth/logout")
//                )

                .csrf(csrf->csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/dashboard/manager/**").hasRole("ADMIN")
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/").permitAll()
                        .anyRequest().permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2Login(oauth2 -> oauth2
                                .userInfoEndpoint(userInfo -> userInfo
                                        .userService(oAuth2UserService)
                                )
                                .successHandler((request, response, authentication) -> {
                                    // Ensure the principal is explicitly set as CustomOauth2User
                                    Object principal = authentication.getPrincipal();
                                    if (principal instanceof DefaultOidcUser) {
                                        CustomOauth2User customOauth2User = oAuth2UserService.processOauth2User(null, (DefaultOidcUser) principal);
                                        authentication.setAuthenticated(false); // Invalidate the current authentication
                                        authentication = new CustomAuthenticationToken(customOauth2User, authentication.getAuthorities());
                                    }
                                    if (!(authentication.getPrincipal() instanceof CustomOauth2User)) {
                                        throw new IllegalStateException("Authentication principal is not a CustomOauth2User. Found: " + principal.getClass().getName());
                                    }
                                    log.info("Authentication successful with CustomOauth2User: {}", ((CustomOauth2User) authentication.getPrincipal()).getName());
                                    // Redirect to frontend with success
                                    response.sendRedirect("http://localhost:5173/auth/success");
                                })
                                .failureHandler((request, response, exception) -> {
                                    // Redirect to frontend with error
                                    response.sendRedirect("http://localhost:5173/auth/failure" + "?error=" + exception.getMessage());
                                })
                )
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);;

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:5173"));

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
