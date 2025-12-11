package com.example.demo.mapper;

import com.example.demo.dto.registration.RegistrationDTO;
import com.example.demo.model.Registration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface RegistrationMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "eventLocation", source = "event.location")
    @Mapping(target = "eventStartTime", source = "event.date")
    @Mapping(target = "userResponse", source = "user")
    @Mapping(target = "eventCompleted", source = "eventCompleted")
    RegistrationDTO toRegistrationDTO(Registration registration);
}
