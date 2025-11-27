package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Data
@Accessors(chain = true)
@ToString(exclude = {"likes", "comments", "posts", "events", "notifications", "fileRecords"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class User implements UserDetails, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Column(unique = true)
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    private String phoneNumber;
    private String address;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Like> likes;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Comment> comments;

    @OneToMany(mappedBy = "postCreator", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Post> posts;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Event> events;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Notification> notifications;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<FileRecord> fileRecords;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<RefreshToken> refreshTokens;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Registration> registrations;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "user_tags",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserFcmToken> userFcmTokens;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "active_role")
    private Role.RoleName activeRole;

    private boolean enabled = true;

    // Oauth 2.0 field

    @Enumerated(EnumType.STRING)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    private String providerId;

    private String imageUrl;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AuthProvider {
        LOCAL, GOOGLE
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Return all roles the user has - they have permissions for all their roles
        // The activeRole is just for UI context, not permission restriction
        if (roles == null || roles.isEmpty()) {
            return new ArrayList<>();
        }
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .collect(Collectors.toList());
    }



    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }


    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    // Helper methods for role management
    
    /**
     * Get the current active role (for UI context and permissions)
     * Falls back to highest priority role if not set
     */
    public Role.RoleName getRole() {
        if (activeRole != null && hasRole(activeRole)) {
            return activeRole;
        }
        // Fallback to highest priority role
        if (roles == null || roles.isEmpty()) {
            return null;
        }
        if (hasRole(Role.RoleName.ADMIN)) {
            return Role.RoleName.ADMIN;
        } else if (hasRole(Role.RoleName.EVENT_MANAGER)) {
            return Role.RoleName.EVENT_MANAGER;
        } else if (hasRole(Role.RoleName.VOLUNTEER)) {
            return Role.RoleName.VOLUNTEER;
        }
        return null;
    }

    /**
     * Add a role to the user (for promotions/grants)
     * Does NOT remove existing roles
     */
    public void addRole(Role roleEntity) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        if (roleEntity != null) {
            roles.add(roleEntity);
            // Set as active if it's the first role
            if (activeRole == null) {
                activeRole = roleEntity.getName();
            }
        }
    }

    /**
     * Remove a role from the user (for demotions/revocations)
     */
    public void removeRole(Role roleEntity) {
        if (roles != null && roleEntity != null) {
            roles.remove(roleEntity);
            // If removed role was active, switch to another available role
            if (activeRole == roleEntity.getName()) {
                activeRole = getRole(); // Will auto-select highest priority remaining role
            }
        }
    }

    /**
     * Replace all roles with a single role (admin complete role change)
     * Use addRole() for promotions that keep existing roles
     */
    public void setRole(Role roleEntity) {
        if (roles == null) {
            roles = new HashSet<>();
        }
        roles.clear();
        if (roleEntity != null) {
            roles.add(roleEntity);
            activeRole = roleEntity.getName();
        } else {
            activeRole = null;
        }
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(Role.RoleName roleName) {
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream()
                .anyMatch(role -> role.getName() == roleName);
    }

    /**
     * Switch the active role (user action in UI)
     * Validates that user actually has the role
     */
    public void switchActiveRole(Role.RoleName roleName) {
        if (!hasRole(roleName)) {
            throw new IllegalArgumentException("User does not have role: " + roleName);
        }
        this.activeRole = roleName;
    }

    /**
     * Get all role names the user has
     */
    public Set<Role.RoleName> getRoleNames() {
        if (roles == null || roles.isEmpty()) {
            return new HashSet<>();
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    public Set<String> getRoleNamesString() {
        if (roles == null || roles.isEmpty()) {
            return new HashSet<>();
        }
        return roles.stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());
    }


    public static User createOauthUser(String email, String firstName, String lastName, AuthProvider authProvider,
                                       String providerId, String imageUrl) {
        String username = email.split("@")[0];

        return new User()
                .setEmail(email)
                .setUsername(username)
                .setEnabled(true)
                .setFirstName(firstName)
                .setLastName(lastName)
                .setAuthProvider(authProvider)
                .setProviderId(providerId)
                .setImageUrl(imageUrl);
    }




}
