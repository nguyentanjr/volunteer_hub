package com.example.demo.mapper;

import com.example.demo.dto.dashboard_volunteer.EventParticipationHistoryDTO;
import com.example.demo.dto.dashboard_volunteer.RecommendEventDTO;
import com.example.demo.dto.dashboard_volunteer.RegisteredEventDTO;
import com.example.demo.dto.dashboard_volunteer.UpcomingEventDTO;
import com.example.demo.dto.event.CreateEventDTO;
import com.example.demo.dto.event.EventApprovedDTO;
import com.example.demo.dto.event.EventDTO;
import com.example.demo.model.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "registrations", ignore = true)
    @Mapping(target = "currentRegistrationCount", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "creator", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "totalScore", ignore = true)
    Event toEvent(CreateEventDTO createEventDTO);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "registrations", ignore = true)
    @Mapping(target = "posts", ignore = true)
    @Mapping(target = "currentRegistrationCount", ignore = true)
    @Mapping(target = "rejectReason", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "totalScore", ignore = true)
    Event toEvent(EventDTO eventDTO);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "creatorUsername", source = "event.creator.username")
    EventDTO toEventDTO(Event event);

    EventApprovedDTO toEventApprovedDTO(Event event);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "creatorId", source = "event.creator.id")
    @Mapping(target = "creatorName", expression = "java(event.getCreator().getFirstName() + \" \" + event.getCreator().getLastName())")
    EventParticipationHistoryDTO toEventParticipationHistoryDTO(Event event);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "eventDate", source = "event.date")
    @Mapping(target = "daysUntilEvent", ignore = true)
    UpcomingEventDTO toUpComingEventDTO(Event event);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "creatorId", source = "event.creator.id")
    @Mapping(target = "creatorName", expression = "java(event.getCreator().getFirstName() + \" \" + event.getCreator().getLastName())")
    RecommendEventDTO toRecommendEventDTO(Event event);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "creatorId", source = "event.creator.id")
    @Mapping(target = "creatorName", expression = "java(event.getCreator().getFirstName() + \" \" + event.getCreator().getLastName())")
    RegisteredEventDTO toRegisteredEventDTO(Event event);
}
