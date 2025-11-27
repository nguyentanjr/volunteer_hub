package com.example.demo.repository;

import com.example.demo.dto.event.EventDTO;
import com.example.demo.model.Event;
import com.example.demo.model.Registration;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistrationRepository extends JpaRepository<Registration, Long> {
    Integer countByEventIdAndStatus(Long eventId, Registration.RegistrationStatus status);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    Page<Registration> findAll(Pageable pageable);

    @Query("SELECT r.event FROM Registration r WHERE r.user.id = :userId")
    Page<Event> findEventRegistered(@Param("userId") Long userId, Pageable pageable);

    Optional<Registration> findRegistrationByUserIdAndEventId(Long userId, Long eventId);

    Optional<Registration> findRegistrationById(Long registrationId);


    Integer countByEventId(Long eventId);
    
    Integer countByUserId(Long userId);

    @Query("SELECT r FROM Registration r WHERE r.event.id = :eventId AND r.status = 'WAITING' " +
            "ORDER BY r.registeredAt ASC ")
    List<Registration> findEarliestWaitingRegistration(Long eventId);

    @Modifying
    @Query("DELETE FROM Registration r WHERE r.event.id = :eventId AND r.user.id = :userId")
    void deleteByUserIdAndEventId(@Param("userId") Long userId, @Param("eventId") Long eventId);

    // Dashboard queries
    @Query("SELECT COUNT(r) FROM Registration r " +
           "WHERE r.event.creator.id = :managerId")
    Integer countAllRegistrationsByManager(@Param("managerId") Long managerId);

    @Query("SELECT COUNT(r) FROM Registration r " +
           "WHERE r.event.creator.id = :managerId AND r.status = :status")
    Integer countRegistrationsByManagerAndStatus(@Param("managerId") Long managerId,
                                                 @Param("status") Registration.RegistrationStatus status);

    @Query("SELECT COUNT(r) FROM Registration r " +
                  "WHERE r.event.id = :eventId AND r.status = :status")
    Integer countRegistrationsByEventIdAndStatus(@Param("eventId") Long eventId,
                                                 @Param("status") Registration.RegistrationStatus status);

    @Query("SELECT r FROM Registration r " +
           "WHERE r.event.creator.id = :managerId " +
           "ORDER BY r.registeredAt DESC")
    List<Registration> findRecentRegistrationsByManager(@Param("managerId") Long managerId, 
                                                        Pageable pageable);

    @Query("SELECT COUNT(r) FROM Registration r " +
           "WHERE r.event.id = :eventId AND r.status = 'PENDING'")
    Integer countPendingByEventId(@Param("eventId") Long eventId);

    @Query("SELECT r FROM Registration r WHERE r.user.id = :userId AND r.status = :registrationStatus")
    List<Registration> findRecentRegistrations(@Param("userId") Long userId, Registration.RegistrationStatus registrationStatus,
                                                        Pageable pageable);
    
    // Admin Dashboard queries
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.status = :status")
    Long countByStatus(@Param("status") Registration.RegistrationStatus status);
    
    @Query("SELECT COUNT(r) FROM Registration r WHERE r.registeredAt >= :startDate")
    Long countRegistrationsCreatedAfter(@Param("startDate") java.time.LocalDateTime startDate);
    
    @Query("SELECT r FROM Registration r ORDER BY r.registeredAt DESC")
    List<Registration> findRecentRegistrations(Pageable pageable);

}
