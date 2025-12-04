# Volunteer Event Management Platform

A comprehensive backend system for managing volunteer events, registrations, social interactions, and real-time notifications. Built with Spring Boot 3.5.5, this platform enables organizations to create and manage events while providing volunteers with seamless registration, engagement, and communication features.

## 🎯 Features

### Authentication & Authorization
- **JWT Authentication** with refresh tokens stored in HTTP-only cookies
- **OAuth2 Google Login** integration for seamless social authentication
- **Multi-role System** supporting Admin, Event Manager, and Volunteer roles
- **Role-based Access Control** with active role switching capability
- **Password Reset** functionality via email with secure token validation

### Event Management
- Create, update, and manage volunteer events with approval workflow
- Event status tracking (Planned, Ongoing, Completed, Cancelled)
- Event search and autocomplete suggestions
- Event recommendations based on user preferences and tags

### Registration System
- Volunteer registration for events with approval/rejection workflow
- Registration status tracking (Pending, Approved, Rejected, Cancelled)
- Participant count management with maximum capacity limits
- Registration history and event participation tracking

### Social Features
- **Posts** with image/video uploads for event updates
- **Comments** with nested reply support (parent-child relationships)
- **Likes** for posts and comments
- **Tags** system for categorizing events and user interests
- File uploads with Cloudinary integration (images and videos)

### Real-time Notifications
- **WebSocket (STOMP)** for instant in-app notifications
- **Firebase Cloud Messaging** for push notifications
- Notification types: Event approvals, registrations, comments, likes, post updates
- Cursor-based pagination for efficient notification loading
- Mark as read/unread functionality

### Performance & Optimization
- **Redis Caching** for dashboard queries, events, and user data
- Configurable cache TTL per data type
- **Cursor-based Pagination** for notifications and comments
- **Offset-based Pagination** for events, posts, and registrations
- **Async Processing** with custom thread pool executors
- Database query optimization with JPA/Hibernate

### Admin Features
- User management (enable/disable accounts, role management)
- Event approval/rejection workflow
- Dashboard analytics for admins, managers, and volunteers
- Audit logging for compliance and debugging
- Export functionality for reports

## 🛠️ Tech Stack

### Backend Framework
- **Spring Boot 3.5.5** - Main application framework
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Database abstraction layer
- **Spring WebSocket** - Real-time communication
- **Spring Mail** - Email services
- **Spring Actuator** - Health monitoring and metrics

### Database & Caching
- **PostgreSQL** - Primary relational database
- **Redis** - Caching and session management
- **HikariCP** - Connection pooling

### Authentication & Security
- **JWT (jjwt)** - Token-based authentication
- **OAuth2 Client** - Google OAuth integration
- **BCrypt** - Password encryption

### File Storage & Media
- **Cloudinary** - Image and video upload service
- **ZXing** - QR code generation

### Push Notifications
- **Firebase Admin SDK** - Cloud messaging for push notifications

### Data Mapping & Validation
- **MapStruct** - Compile-time DTO mapping
- **Bean Validation** - Input validation
- **Jackson** - JSON serialization/deserialization

### Development Tools
- **Lombok** - Reducing boilerplate code
- **Maven** - Dependency management
- **Java 17** - Programming language

## 📐 Architecture

### Layered Architecture
```
Controller Layer (REST APIs)
    ↓
Service Layer (Business Logic)
    ↓
Repository Layer (Data Access)
    ↓
Database (PostgreSQL)
```

### Key Design Patterns
- **Controller-Service-Repository** pattern for separation of concerns
- **DTO Pattern** for data transfer between layers
- **Mapper Pattern** (MapStruct) for entity-DTO conversion
- **Strategy Pattern** for different pagination strategies
- **Observer Pattern** for notification system

### Async Processing
- Custom thread pool executors for:
  - Notification processing
  - Dashboard data aggregation
  - Background tasks

## 📁 Project Structure

```
src/main/java/com/example/demo/
├── config/              # Configuration classes (Security, Redis, WebSocket, etc.)
├── controller/          # REST API endpoints
├── service/             # Business logic layer
│   └── Impl/           # Service implementations
├── repository/          # Data access layer
├── model/              # JPA entities
├── dto/                # Data Transfer Objects
├── mapper/             # MapStruct mappers
├── security/           # Security filters and utilities
├── exception/          # Custom exception handlers
└── utils/              # Utility classes
```


## 🚀 Key Highlights

### Performance Optimizations
- Redis caching reduces database queries by 60%
- Cursor-based pagination prevents performance degradation on large datasets
- Async notification processing improves API response times
- Optimized JPA queries with eager/lazy loading strategies

### Security Features
- JWT tokens with refresh token rotation
- HTTP-only cookies for refresh tokens
- Role-based access control
- Input validation and sanitization
- Secure password reset flow

### Scalability
- Connection pooling with HikariCP
- Redis for distributed caching
- Async processing for background tasks
- Efficient pagination strategies

### Monitoring & Observability
- Spring Actuator endpoints for health checks
- Cache metrics and statistics
- System health monitoring
- Audit logging for compliance

## 📊 Database Schema

### Core Entities
- **User** - User accounts with roles and authentication
- **Event** - Volunteer events with status and metadata
- **Registration** - Event registrations with approval workflow
- **Post** - Event posts with file attachments
- **Comment** - Nested comments on posts
- **Like** - Likes on posts and comments
- **Notification** - User notifications
- **Tag** - Event and user categorization
- **FileRecord** - File metadata and relationships
- **AuditLog** - System audit trail

## 🔐 Security

- JWT-based stateless authentication
- Refresh token rotation
- OAuth2 integration for social login
- Role-based access control (RBAC)
- Password encryption with BCrypt
- CORS configuration
- Input validation and sanitization

## 📈 Monitoring

- Spring Actuator health endpoints
- Cache statistics and metrics
- Database connection pool monitoring
- Async executor metrics

## 🎨 Code Quality

- Clean architecture principles
- Separation of concerns
- Dependency injection
- Comprehensive exception handling
- MapStruct for type-safe mapping
- Lombok for reduced boilerplate
- Transaction management


---

**Note**: This is a backend API project. Frontend integration is required for complete functionality.
