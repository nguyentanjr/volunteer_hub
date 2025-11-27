package com.example.demo.controller;

import com.example.demo.dto.common.ApiResponse;
import com.example.demo.dto.user_fcm.UserFcmTokenDTO;
import com.example.demo.model.UserFcmToken;
import com.example.demo.service.FirebaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/firebase")
public class FirebaseController {

    private final FirebaseService firebaseService;

    public ResponseEntity<ApiResponse<UserFcmToken>> registerToken(@RequestBody UserFcmTokenDTO userFcmTokenDTO) {
        return ResponseEntity.ok(ApiResponse.success(firebaseService.registerTokenForUser(userFcmTokenDTO)));
    }
}
