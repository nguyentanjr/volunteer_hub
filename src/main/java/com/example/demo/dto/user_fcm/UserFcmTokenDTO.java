package com.example.demo.dto.user_fcm;

import com.example.demo.model.UserFcmToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFcmTokenDTO {

    private String token;

    private UserFcmToken.DeviceType deviceType;

    private String deviceId;


}
