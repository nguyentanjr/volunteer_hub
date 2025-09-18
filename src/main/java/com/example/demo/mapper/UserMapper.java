package com.example.demo.mapper;
import org.mapstruct.factory.Mappers;

import com.example.demo.dto.auth.RegisterRequest;
import com.example.demo.dto.user.UserResponse;
import com.example.demo.model.User;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.WARN
)
public interface UserMapper {

    @Mapping(target = "id", ignore = true)                    // ID is generated
    @Mapping(target = "role", constant = "USER")              // Default role
    @Mapping(target = "enabled", constant = "true")           // Default enabled
    @Mapping(target = "createdAt", ignore = true)             // Set by @PrePersist
    @Mapping(target = "updatedAt", ignore = true)             // Set by @PrePersist
    @Mapping(target = "authorities", ignore = true)           // UserDetails method
    @Mapping(target = "accountNonExpired", ignore = true)     // UserDetails method
    @Mapping(target = "accountNonLocked", ignore = true)      // UserDetails method
    @Mapping(target = "credentialsNonExpired", ignore = true) // UserDetails method
    User toUser(RegisterRequest registerRequest);

    @Mapping(target = "role", expression = "java(user.getRole().name())")  // Convert enum to string
    UserResponse toUserResponse(User user);

}
