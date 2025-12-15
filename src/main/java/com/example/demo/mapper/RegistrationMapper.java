package com.example.demo.mapper;

import com.example.demo.dto.registration.RegistrationDTO;
import com.example.demo.model.Registration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface RegistrationMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "userResponse", source = "user")
    RegistrationDTO toRegistrationDTO(Registration registration);
}
