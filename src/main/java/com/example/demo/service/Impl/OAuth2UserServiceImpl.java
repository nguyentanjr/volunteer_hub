package com.example.demo.service.Impl;

import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("Loading OAuth2 user for provider: {}", userRequest.getClientRegistration().getRegistrationId());

        OAuth2User oAuth2User = super.loadUser(userRequest);
        try {
            // Ensure the returned user is always a CustomOauth2User
            CustomOauth2User customOauth2User = processOauth2User(userRequest, oAuth2User);
            log.info("Successfully loaded CustomOauth2User: {}", customOauth2User.getName());
            return customOauth2User;
        } catch (Exception e) {
            log.error("Error processing OAuth2 user: {}", e.getMessage());
            throw new OAuth2AuthenticationException("Error processing OAuth2 user: " + e.getMessage());
        }
    }

    public CustomOauth2User processOauth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {

        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String googleId = (String) attributes.get("sub");
        String firstName = (String) attributes.get("given_name");
        String lastName = (String) attributes.get("family_name");
        String imageUrl = (String) attributes.get("picture");

        if(email == null || email.isEmpty()) {
            throw new OAuth2AuthenticationException("Email not found from Google");
        }

        User user = userRepository.findUserByEmail(email)
                .orElse(null);

        if(user != null) {
            if(user.getAuthProvider() != User.AuthProvider.GOOGLE) {
                throw new OAuth2AuthenticationException("Please use Google to login");
            }
            user = updateExistingUser(user, firstName, lastName, imageUrl);
        }
        else {
            user = registerNewUser(email, googleId, firstName, lastName, googleId, imageUrl);
        }
        return new CustomOauth2User(oAuth2User, user);
    }

    public User registerNewUser(String email, String googleId, String firstName, String lastName, String providerId, String imageUrl) {

        log.info("Registering new user with email: {}", email);
        User user = User.createOauthUser(email, firstName, lastName, User.AuthProvider.GOOGLE, providerId, imageUrl);

        String username = email.split("@")[0];
        int count = 1;
        while(userRepository.findUserByUsername(username).isPresent()) {
            username = username + count;
            count++;
        }

        user.setUsername(username);
        user.setPassword("$2a$12$gb1ZqhtwGlccwjgLQH/NwOpXBx8mFMmT3cIRcQjZrUqdVulfjZj3a");
        
        // Assign default VOLUNTEER role
        Role volunteerRole = roleRepository.findByName(Role.RoleName.VOLUNTEER)
                .orElseThrow(() -> new ResourceNotFoundException("Role VOLUNTEER not found"));
        user.addRole(volunteerRole);
        
        User savedUser = userRepository.save(user);

        log.info("Registering new user successfully: {}", username);

        return savedUser;
    }

    public User updateExistingUser(User existUser, String firstName, String lastName, String imageUrl) {

        log.info("Update existing user: {}", existUser.getUsername());

        existUser.setFirstName(firstName);
        existUser.setLastName(lastName);
        existUser.setImageUrl(imageUrl);

        log.info("Update existing user successfully: {}", existUser.getUsername());

        return userRepository.save(existUser);
    }

}
