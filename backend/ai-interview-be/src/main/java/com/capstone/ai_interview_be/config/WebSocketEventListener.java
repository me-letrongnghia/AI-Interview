package com.capstone.ai_interview_be.config;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// Cấu hình lắng nghe sự kiện WebSocket
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener implements ApplicationListener<SessionDisconnectEvent> {

    // phương thức xử lý sự kiện kết nối WebSocket
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println("🟢 CONNECTED: " + accessor.getSessionId());
    }

    // phương thức xử lý sự kiện ngắt kết nối WebSocket
    @Override
    public void onApplicationEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println("🔴 DISCONNECTED: " + accessor.getSessionId());
    }

    // phương thức xử lý sự kiện ngắt kết nối WebSocket
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        log.info("WebSocket disconnected, sessionId={}", sessionId);
    }
   
    // phương thức xử lý sự kiện subscribe WebSocket
    @EventListener
    public void handleSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        String sessionId = headerAccessor.getSessionId();
        
        log.info("User subscribed - SessionId: {}, Destination: {}", sessionId, destination);
    }

    //Hàm giả để phát sóng trạng thái người dùng (kết nối/ngắt kết nối)
    // private void broadcastUserStatus(Long userId, String username, String status) {
    //    log.info("Broadcasting user status - UserId: {}, Username: {}, Status: {}", userId, username, status);
    // }
}
