package com.example.demo.repository;

import com.example.demo.model.Event;
import com.example.demo.model.Post;
import com.example.demo.model.Tag;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EventRepository extends JpaRepository<Event,Long> {
    Optional<Event> findByTitleAndDescriptionAndDateAndLocationAndMaxParticipants(
            String title,
            String description,
            LocalDateTime date,
            String location,
            Integer maxParticipants
    );

    @Query("SELECT e.title FROM Event e")
    List<String> findAllTitles();

    @Query("SELECT e FROM Event e")
    Page<Event> findAllEvent(Pageable pageable);

    Optional<Event> getEventById(Long id);

    @Query("SELECT e FROM Event e WHERE e.status = 'PENDING'")
    List<Event> getPendingEvents();

    @Query("SELECT e FROM Event e WHERE e.creator.id = :userId")
    Page<Event> getMyEvents(Long userId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.creator.id = :userId")
    List<Event> getMyEvents(Long userId);

    // Dashboard queries
    @Query("SELECT COUNT(e) FROM Event e WHERE e.creator.id = :managerId")
    Integer countEventsByManager(@Param("managerId") Long managerId);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.creator.id = :managerId AND e.status = :status")
    Integer countEventsByManagerAndStatus(@Param("managerId") Long managerId, 
                                          @Param("status") Event.EventStatus status);

        @Query("SELECT e FROM Event e WHERE e.creator.id = :managerId " +
               "AND e.date BETWEEN :startDate AND :endDate " +
               "ORDER BY e.date ASC")
        List<Event> findUpcomingEventsByManager(@Param("managerId") Long managerId,
                                                @Param("startDate") java.time.LocalDateTime startDate,
                                                @Param("endDate") java.time.LocalDateTime endDate);

    @Query("SELECT e FROM Event e JOIN Registration r ON r.event.id = e.id " +
            "WHERE r.user.id = :userId AND e.date BETWEEN :startDate AND :endDate AND e.status = 'ONGOING'")
    List<Event> findUpcomingEvents(LocalDateTime startDate, LocalDateTime endDate, Long userId);

    @Query("SELECT e FROM Event e " +
           "WHERE e.creator.id = :managerId " +
           "AND EXISTS (SELECT r FROM Registration r WHERE r.event = e AND r.status = 'PENDING') " +
           "ORDER BY e.date ASC")
    List<Event> findEventsWithPendingRegistrations(@Param("managerId") Long managerId);

    @Query("SELECT e FROM Event e WHERE e.status = :eventStatus " +
            "AND EXISTS (SELECT r FROM Registration r WHERE r.user.id = :volunteerId AND r.event = e) " +
            "ORDER BY e.date DESC")
    List<Event> findEventsByVolunteerAndStatus(Long volunteerId,Event.EventStatus eventStatus);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = 'COMPLETED'")
    long countApprovedEvents();

    @Query("SELECT COUNT(e) FROM Event e")
    int countAllEvents();

    @Query("SELECT e.tags FROM Event e WHERE e.id = :eventId")
    Set<Tag> findTagsByEventId(@Param("eventId") Long eventId);
    
    // Admin Dashboard queries
    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = :status")
    Long countByStatus(@Param("status") Event.EventStatus status);
    
    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdAt >= :startDate")
    Long countEventsCreatedAfter(@Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT e FROM Event e WHERE e.status = 'PLANNED' ORDER BY e.createdAt DESC")
    List<Event> findPendingEventsForApproval(Pageable pageable);

    List<Event> findTop5ByTitleContainingIgnoreCase(String Title);

    @Query("""
    SELECT e FROM Event e
    WHERE e.date > CURRENT_TIMESTAMP\s
      AND e.maxParticipants > e.currentRegistrationCount
    ORDER BY e.date
   \s""")
    List<Event> findCandidateEvents(Pageable pageable);

}
