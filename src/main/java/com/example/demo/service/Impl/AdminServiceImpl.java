package com.example.demo.service.Impl;

import com.example.demo.dto.dashboard_manager.AdminDashboardDTO;
import com.example.demo.dto.dashboard_manager.PendingEventDTO;
import com.example.demo.dto.dashboard_manager.SystemActivityDTO;
import com.example.demo.dto.dashboard_manager.TimeStatisticsDTO;
import com.example.demo.dto.dashboard_manager.UserManagementDTO;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.dto.user.ChangeUserRoleDTO;
import com.example.demo.dto.user.EnableUserDTO;
import com.example.demo.dto.user.ResetPasswordDTO;
import com.example.demo.dto.user.UserDetailDTO;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.EventMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.service.*;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final RegistrationRepository registrationRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;
    private final FirebaseService firebaseService;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;


    @CacheEvict(value = "dashboard", allEntries = true)
    public Event approveEvent(Long eventId) throws FirebaseMessagingException {
        log.info("Approve event with ID: {} (clearing dashboard cache)", eventId);

        Event event = eventRepository.getEventById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        event.setStatus(Event.EventStatus.ONGOING);

        notificationService.notifyManagerOnEventApproved(event, "Your request of creating new event has been approved");
        sendApprovalNotification(event);

        return eventRepository.save(event);
    }

    @CacheEvict(value = "dashboard", allEntries = true)
    public Event rejectEvent(Long eventId, String reason) throws FirebaseMessagingException {
        log.info("Reject event with ID: {} (clearing dashboard cache)", eventId);
        Event event = eventRepository.getEventById(eventId)

                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        event.setStatus(Event.EventStatus.CANCELLED);
        event.setRejectReason(reason);

        notificationService.notifyManagerOnEventRejected(event, reason);
        sendRejectNotification(event);

        return eventRepository.save(event);
    }

    private void sendApprovalNotification(Event event) throws FirebaseMessagingException {
        User creator = event.getCreator();

        UserFcmToken userFcmToken = userFcmTokenRepository.findByUser(creator)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found for user"));

        firebaseService.sendToToken(
                userFcmToken.getToken(),
                "Event Approved",
                "Your event '" + event.getTitle() + "' has been approved!"
        );
    }

    private void sendRejectNotification(Event event) throws FirebaseMessagingException {
        User creator = event.getCreator();

        UserFcmToken userFcmToken = userFcmTokenRepository.findByUser(creator)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found for user"));

        firebaseService.sendToToken(
                userFcmToken.getToken(),
                "Event Rejected",
                "Your event '" + event.getTitle() + "' has been rejected!"
        );
    }

    public EventDTO getEventDetails(Long eventId) {
        log.info("Get event details with ID : {}", eventId);
        Event event = eventRepository.getEventById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return eventMapper.toEventDTO(event);
    }

    @Override
    @Cacheable(value = "dashboard", key = "'admin:' + #root.target.getCurrentUser().id")
    public AdminDashboardDTO getAdminDashboard() {
        log.info("Fetching admin dashboard data");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last7Days = now.minusDays(7);
        LocalDateTime last30Days = now.minusDays(30);
        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime startOfLastMonth = YearMonth.now().minusMonths(1).atDay(1).atStartOfDay();
        LocalDateTime endOfLastMonth = YearMonth.now().atDay(1).atStartOfDay().minusSeconds(1);
        
        Long totalUsers = userRepository.count();
        Long totalEvents = (long) eventRepository.countAllEvents();
        Long totalRegistrations = registrationRepository.count();
        
        Long totalVolunteers = userRepository.countByRoleName(Role.RoleName.VOLUNTEER);
        Long totalEventManagers = userRepository.countByRoleName(Role.RoleName.EVENT_MANAGER);
        Long totalAdmins = userRepository.countByRoleName(Role.RoleName.ADMIN);
        
        Long plannedEvents = eventRepository.countByStatus(Event.EventStatus.PLANNED);
        Long ongoingEvents = eventRepository.countByStatus(Event.EventStatus.ONGOING);
        Long completedEvents = eventRepository.countByStatus(Event.EventStatus.COMPLETED);
        Long cancelledEvents = eventRepository.countByStatus(Event.EventStatus.CANCELLED);
        
        Long pendingRegistrations = registrationRepository.countByStatus(Registration.RegistrationStatus.PENDING);
        Long approvedRegistrations = registrationRepository.countByStatus(Registration.RegistrationStatus.APPROVED);
        Long rejectedRegistrations = registrationRepository.countByStatus(Registration.RegistrationStatus.REJECTED);
        Long cancelledRegistrations = registrationRepository.countByStatus(Registration.RegistrationStatus.CANCELLED);
        
        Long enabledUsers = userRepository.countByEnabled(true);
        Long disabledUsers = userRepository.countByEnabled(false);
        
        Pageable pendingPageable = PageRequest.of(0, 10);
        List<Event> pendingEventsList = eventRepository.findPendingEventsForApproval(pendingPageable);
        List<PendingEventDTO> pendingEvents = pendingEventsList.stream()
            .map(event -> PendingEventDTO.builder()
                .eventId(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .date(event.getDate())
                .maxParticipants(event.getMaxParticipants())
                .createdAt(event.getCreatedAt())
                .creatorName(event.getCreator().getFirstName() + " " + event.getCreator().getLastName())
                .creatorEmail(event.getCreator().getEmail())
                .build())
            .collect(Collectors.toList());
        
        List<SystemActivityDTO> recentActivities = buildSystemActivities(now);
        
        Long newUsersLast7Days = userRepository.countUsersCreatedAfter(last7Days);
        Long newEventsLast7Days = eventRepository.countEventsCreatedAfter(last7Days);
        Long newRegistrationsLast7Days = registrationRepository.countRegistrationsCreatedAfter(last7Days);
        
        Long newUsersLast30Days = userRepository.countUsersCreatedAfter(last30Days);
        Long newEventsLast30Days = eventRepository.countEventsCreatedAfter(last30Days);
        Long newRegistrationsLast30Days = registrationRepository.countRegistrationsCreatedAfter(last30Days);
        
        Long usersThisMonth = userRepository.countUsersCreatedAfter(startOfMonth);
        Long eventsThisMonth = eventRepository.countEventsCreatedAfter(startOfMonth);
        Long registrationsThisMonth = registrationRepository.countRegistrationsCreatedAfter(startOfMonth);
        
        Long usersLastMonth = userRepository.countUsersCreatedAfter(startOfLastMonth) - usersThisMonth;
        Long eventsLastMonth = eventRepository.countEventsCreatedAfter(startOfLastMonth) - eventsThisMonth;
        Long registrationsLastMonth = registrationRepository.countRegistrationsCreatedAfter(startOfLastMonth) - registrationsThisMonth;
        
        Double userGrowthRate = calculateGrowthRate(usersThisMonth, usersLastMonth);
        Double eventGrowthRate = calculateGrowthRate(eventsThisMonth, eventsLastMonth);
        Double registrationGrowthRate = calculateGrowthRate(registrationsThisMonth, registrationsLastMonth);
        
        TimeStatisticsDTO timeStatistics = TimeStatisticsDTO.builder()
            .newUsersLast7Days(newUsersLast7Days)
            .newEventsLast7Days(newEventsLast7Days)
            .newRegistrationsLast7Days(newRegistrationsLast7Days)
            .newUsersLast30Days(newUsersLast30Days)
            .newEventsLast30Days(newEventsLast30Days)
            .newRegistrationsLast30Days(newRegistrationsLast30Days)
            .usersThisMonth(usersThisMonth)
            .eventsThisMonth(eventsThisMonth)
            .registrationsThisMonth(registrationsThisMonth)
            .userGrowthRate(userGrowthRate)
            .eventGrowthRate(eventGrowthRate)
            .registrationGrowthRate(registrationGrowthRate)
            .build();
        
        Pageable userPageable = PageRequest.of(0, 20);
        List<User> recentUsersList = userRepository.findRecentUsers(userPageable);
        List<UserManagementDTO> recentUsers = recentUsersList.stream()
            .map(user -> {
                int eventsCreated = user.getEvents() != null ? user.getEvents().size() : 0;
                int registrationsCount = user.getId() != null ? 
                    registrationRepository.countByUserId(user.getId()) : 0;
                
                return UserManagementDTO.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .fullName(user.getFirstName() + " " + user.getLastName())
                    .roles(user.getRoles().stream().map(role -> role.getName().toString()).toList())
                    .enabled(user.isEnabled())
                    .createdAt(user.getCreatedAt())
                    .lastActivity(user.getUpdatedAt())
                    .eventsCreated(eventsCreated)
                    .registrationsCount(registrationsCount)
                    .build();
            })
            .collect(Collectors.toList());
        
        return AdminDashboardDTO.builder()
            .totalUsers(totalUsers)
            .totalEvents(totalEvents)
            .totalRegistrations(totalRegistrations)
            .totalVolunteers(totalVolunteers)
            .totalEventManagers(totalEventManagers)
            .totalAdmins(totalAdmins)
            .plannedEvents(plannedEvents)
            .ongoingEvents(ongoingEvents)
            .completedEvents(completedEvents)
            .cancelledEvents(cancelledEvents)
            .pendingRegistrations(pendingRegistrations)
            .approvedRegistrations(approvedRegistrations)
            .rejectedRegistrations(rejectedRegistrations)
            .cancelledRegistrations(cancelledRegistrations)
            .enabledUsers(enabledUsers)
            .disabledUsers(disabledUsers)
            .pendingEvents(pendingEvents)
            .recentActivities(recentActivities)
            .timeStatistics(timeStatistics)
            .recentUsers(recentUsers)
            .build();
    }
    
    private List<SystemActivityDTO> buildSystemActivities(LocalDateTime now) {
        List<SystemActivityDTO> activities = new ArrayList<>();
        Pageable activityPageable = PageRequest.of(0, 20);
        
        List<Registration> recentRegistrations = registrationRepository.findRecentRegistrations(activityPageable);
        for (Registration reg : recentRegistrations) {
            activities.add(SystemActivityDTO.builder()
                .activityType("NEW_REGISTRATION")
                .description(reg.getUser().getUsername() + " registered for " + reg.getEvent().getTitle())
                .timestamp(reg.getRegisteredAt())
                .userName(reg.getUser().getUsername())
                .relatedId(reg.getId())
                .build());
        }
        
        List<User> recentUsers = userRepository.findRecentUsers(activityPageable);
        for (User user : recentUsers) {
            activities.add(SystemActivityDTO.builder()
                .activityType("NEW_USER")
                .description("New user joined: " + user.getUsername())
                .timestamp(user.getCreatedAt())
                .userName(user.getUsername())
                .relatedId(user.getId())
                .build());
        }
        System.out.println(activities);

        return activities.stream()
                .filter(a -> a.getTimestamp() != null)
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(30)
                .collect(Collectors.toList());

    }
    
    private Double calculateGrowthRate(Long currentPeriod, Long previousPeriod) {
        if (previousPeriod == null || previousPeriod == 0) {
            return currentPeriod > 0 ? 100.0 : 0.0;
        }
        return ((currentPeriod - previousPeriod) * 100.0) / previousPeriod;
    }
    
    
    @Override
    public UserDetailDTO getUserDetail(Long userId) {
        log.info("Getting user detail for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        int eventsCreatedCount = user.getEvents() != null ? user.getEvents().size() : 0;
        int registrationsCount = registrationRepository.countByUserId(userId);
        int postsCount = user.getPosts() != null ? user.getPosts().size() : 0;
        int commentsCount = user.getComments() != null ? user.getComments().size() : 0;
        
        List<String> tagNames = user.getTags().stream()
                .map(Tag::getName)
                .collect(Collectors.toList());
        
        return UserDetailDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .authProvider(user.getAuthProvider().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .eventsCreatedCount(eventsCreatedCount)
                .registrationsCount(registrationsCount)
                .postsCount(postsCount)
                .commentsCount(commentsCount)
                .tags(tagNames)
                .build();
    }
    
    @Override
    @Transactional
    public UserResponse enableOrDisableUser(EnableUserDTO enableUserDTO, User admin) {
        log.info("Admin {} {} user ID: {}", 
                admin.getUsername(), 
                enableUserDTO.getEnabled() ? "enabling" : "disabling", 
                enableUserDTO.getUserId());
        
        User user = userRepository.findById(enableUserDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + enableUserDTO.getUserId()));
        
        if (user.getId().equals(admin.getId())) {
            throw new BadRequestException("You cannot disable your own account");
        }
        
        if (user.hasRole(Role.RoleName.ADMIN) && !admin.hasRole(Role.RoleName.ADMIN)) {
            throw new BadRequestException("Only admins can disable other admin accounts");
        }
        
        String oldValue = user.isEnabled() ? "enabled" : "disabled";
        user.setEnabled(enableUserDTO.getEnabled());
        User savedUser = userRepository.save(user);
        
        String newValue = enableUserDTO.getEnabled() ? "enabled" : "disabled";
        auditLogService.logUserAction(
                enableUserDTO.getEnabled() ? AuditLog.ActionType.USER_ENABLED : AuditLog.ActionType.USER_DISABLED,
                user.getId(),
                oldValue,
                newValue,
                enableUserDTO.getReason(),
                admin
        );
        
        if (!enableUserDTO.getEnabled()) {
            notificationService.notifyUserAccountDisabled(user, enableUserDTO.getReason());
        } else {
            notificationService.notifyUserAccountEnabled(user);
        }
        
        return userMapper.toUserResponse(savedUser);
    }
    
    @Override
    @Transactional
    public UserResponse changeUserRole(ChangeUserRoleDTO changeUserRoleDTO, User admin) {
        log.info("Admin {} changing role for user ID: {} to {}", 
                admin.getUsername(), 
                changeUserRoleDTO.getUserId(), 
                changeUserRoleDTO.getNewRole());
        
        User user = userRepository.findById(changeUserRoleDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + changeUserRoleDTO.getUserId()));
        
        if (user.getId().equals(admin.getId())) {
            throw new BadRequestException("You cannot change your own role");
        }
        
        if (user.hasRole(Role.RoleName.ADMIN) && !admin.hasRole(Role.RoleName.ADMIN)) {
            throw new BadRequestException("Only admins can change admin roles");
        }
        
        String oldRole = user.getRole() != null ? user.getRole().name() : "NONE";
        Role newRoleEntity = roleRepository.findByName(changeUserRoleDTO.getNewRole())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + changeUserRoleDTO.getNewRole()));
        user.setRole(newRoleEntity);
        User savedUser = userRepository.save(user);
        
        auditLogService.logUserAction(
                AuditLog.ActionType.USER_ROLE_CHANGED,
                user.getId(),
                oldRole,
                changeUserRoleDTO.getNewRole().name(),
                changeUserRoleDTO.getReason(),
                admin
        );
        
        notificationService.notifyUserRoleChanged(user, oldRole, changeUserRoleDTO.getNewRole().name());
        
        return userMapper.toUserResponse(savedUser);
    }
    
    @Override
    @Transactional
    public void deleteUser(Long userId, String reason, User admin) {
        log.info("Admin {} deleting user ID: {}", admin.getUsername(), userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (user.getId().equals(admin.getId())) {
            throw new BadRequestException("You cannot delete your own account");
        }
        
        if (user.hasRole(Role.RoleName.ADMIN)) {
            throw new BadRequestException("Admin accounts cannot be deleted");
        }
        
        auditLogService.logUserAction(
                AuditLog.ActionType.USER_DELETED,
                userId,
                user.getUsername(),
                "DELETED",
                reason,
                admin
        );
        
        userRepository.delete(user);
        
        log.info("User ID: {} deleted successfully by admin: {}", userId, admin.getUsername());
    }
    
    @Override
    @Transactional
    public void resetUserPassword(ResetPasswordDTO resetPasswordDTO, User admin) {
        log.info("Admin {} resetting password for user ID: {}", 
                admin.getUsername(), 
                resetPasswordDTO.getUserId());
        
        User user = userRepository.findById(resetPasswordDTO.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + resetPasswordDTO.getUserId()));
        
        String encodedPassword = passwordEncoder.encode(resetPasswordDTO.getNewPassword());
        user.setPassword(encodedPassword);
        User savedUser = userRepository.save(user);
        
        auditLogService.logUserAction(
                AuditLog.ActionType.USER_PASSWORD_RESET,
                user.getId(),
                "PASSWORD_RESET",
                "PASSWORD_CHANGED",
                "Password reset by admin",
                admin
        );
        
        notificationService.notifyUserPasswordReset(user, resetPasswordDTO.getNewPassword());
        
    }

    public UserResponse promoteToEventManager(Long userId) {
        log.info("Promoting user ID {} to EVENT_MANAGER", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.hasRole(Role.RoleName.EVENT_MANAGER)) {
            log.warn("User {} already has EVENT_MANAGER role", userId);
            return userMapper.toUserResponse(user);
        }

        Role eventManagerRole = roleRepository.findByName(Role.RoleName.EVENT_MANAGER)
                .orElseThrow(() -> new ResourceNotFoundException("Role EVENT_MANAGER not found"));

        user.addRole(eventManagerRole);
        User savedUser = userRepository.save(user);
        log.info("User {} promoted to EVENT_MANAGER successfully", userId);
        return userMapper.toUserResponse(savedUser);
    }

    public UserResponse demoteFromEventManager(Long userId) {
        log.info("Demoting user ID {} from EVENT_MANAGER", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.hasRole(Role.RoleName.EVENT_MANAGER)) {
            log.warn("User {} doesn't have EVENT_MANAGER role", userId);
            return userMapper.toUserResponse(user);
        }

        Role eventManagerRole = roleRepository.findByName(Role.RoleName.EVENT_MANAGER)
                .orElseThrow(() -> new ResourceNotFoundException("Role EVENT_MANAGER not found"));

        user.removeRole(eventManagerRole);
        User savedUser = userRepository.save(user);
        log.info("User {} demoted from EVENT_MANAGER successfully", userId);
        return userMapper.toUserResponse(savedUser);
    }

    public User getCurrentUser() {
        return userService.getCurrentUser();
    }
}
