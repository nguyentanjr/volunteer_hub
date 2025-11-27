package com.example.demo.mapper;

import com.example.demo.dto.notification.NotificationDTO;
import com.example.demo.model.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDTO toNotificationDTO(Notification notification);
}
