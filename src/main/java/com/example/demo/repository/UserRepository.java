package com.example.demo.repository;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findUserByUsername(String username);

    Optional<User> findUserByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findAll(Pageable pageable);

    @Query("""
    SELECT u FROM User u
    WHERE :search IS NULL OR :search = '' OR 
          LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR 
          LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR 
          LOWER(u.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR 
          LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR 
          LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
""")

    Page<User> findUsersWithSearch(String search, Pageable pageable);

    @Query("SELECT u from User u WHERE u.enabled = :enabled")
    Page<User> findALlByEnabled(boolean enabled, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'ADMIN'")
    List<User> findAllAdmin();

    Optional<User> getUserByUsername(String username);

    User getUserById(Long id);
    
    // Admin Dashboard queries
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleName")
    Long countByRoleName(@Param("roleName") Role.RoleName roleName);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.enabled = :enabled")
    Long countByEnabled(@Param("enabled") boolean enabled);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :startDate")
    Long countUsersCreatedAfter(@Param("startDate") java.time.LocalDateTime startDate);
    
    @Query("SELECT u FROM User u ORDER BY u.createdAt DESC")
    List<User> findRecentUsers(Pageable pageable);


}
