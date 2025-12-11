package com.example.demo.config;

import com.example.demo.security.JWTUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

/**
 * WebSocket authentication interceptor
 * Validate JWT token và set username làm Principal
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JWTUtils jwtUtils;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            
            if (authHeaders != null && !authHeaders.isEmpty()) {
                String authHeader = authHeaders.get(0);
                
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    
                    try {
                        if (jwtUtils.validateJwtToken(token)) {
                            String username = jwtUtils.getUsernameFromJwtToken(token);
                            
                            // Set Principal với username (Spring sẽ dùng để route messages)
                            final String finalUsername = username;
                            accessor.setUser(new Principal() {
                                @Override
                                public String getName() {
                                    return finalUsername;
                                }
                            });
                            
                            log.info("WebSocket authenticated user: {}", username);
                        }
                    } catch (Exception e) {
                        log.error("Error authenticating WebSocket", e);
                    }
                }
            }
        }
        
        return message;
    }
}

