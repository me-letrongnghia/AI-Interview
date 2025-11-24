package com.capstone.ai_interview_be.config;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    /**
     * Sự kiện khi người dùng kết nối WebSocket
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        log.info("Received a new web socket connection");
    }

    /**
     * Sự kiện khi người dùng ngắt kết nối WebSocket
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        log.info("Web socket connection disconnected");
    }

    /**
     * Sự kiện khi người dùng subscribe vào một topic
     */
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        
        log.info("User subscribed - SessionId: {}, Destination: {}", sessionId, destination);
    }

    /**
     * Broadcast trạng thái user đến tất cả clients
     */
    private void broadcastUserStatus(Long userId, String username, String status) {
       log.info("Broadcasting user status - UserId: {}, Username: {}, Status: {}", userId, username, status);
    }
}
