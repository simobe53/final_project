package com.ict.springboot.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict.springboot.dto.UsersDto;

/**
 * Meet 위치공유 전용 WebSocket Handler
 * 엔드포인트: /api/location
 * 파라미터: meetId
 */
@Component
public class LocationWebSocketHandler extends TextWebSocketHandler {

    // Meet별로 연결된 클라이언트 관리: meetId -> (userId -> WebSocketSession)
    private Map<Long, Map<Long, WebSocketSession>> meetClients = new ConcurrentHashMap<>();

    // 세션에서 유저 정보 가져오기
    private UsersDto getUserFromSession(@NonNull WebSocketSession session) {
        Object userObj = session.getAttributes().get("user");
        if (userObj == null) throw new IllegalStateException("세션에 유저 정보가 없습니다.");
        return (UsersDto) userObj;
    }

    // meetId 파라미터 파싱
    private Long getMeetId(@NonNull WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query == null || !query.contains("meetId=")) {
            throw new IllegalArgumentException("meetId 파라미터가 없습니다.");
        }
        return Long.valueOf(query.substring(query.lastIndexOf("meetId=")).replace("meetId=", ""));
    }

    // 특정 Meet의 클라이언트 목록 가져오기
    private Map<Long, WebSocketSession> getClientsInMeet(Long meetId) {
        return meetClients.get(meetId);
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        try {
            UsersDto user = getUserFromSession(session);
            Long meetId = getMeetId(session);
            Long userId = user.getId();

            // Meet 방이 없으면 생성
            meetClients.putIfAbsent(meetId, new ConcurrentHashMap<>());
            Map<Long, WebSocketSession> clients = getClientsInMeet(meetId);

            // 클라이언트 추가 (같은 userId면 덮어쓰기)
            clients.put(userId, session);

            System.out.println("✅ LocationWebSocket 연결됨: meetId=" + meetId + ", userId=" + userId);
        } catch (Exception e) {
            System.err.println("❌ LocationWebSocket 연결 실패: " + e.getMessage());
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try {
            UsersDto user = getUserFromSession(session);
            Long meetId = getMeetId(session);
            Map<Long, WebSocketSession> clients = getClientsInMeet(meetId);

            if (clients == null) {
                System.err.println("❌ meetId=" + meetId + "의 클라이언트 목록이 없습니다.");
                return;
            }

            // JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(message.getPayload());

            // type이 location인지 확인
            String type = json.has("type") ? json.get("type").asText() : "";
            if (!"location".equals(type)) {
                System.out.println("⚠️ location 메시지가 아닙니다: type=" + type);
                return;
            }

            // 위치 정보 추출
            Long userId = json.has("userId") ? json.get("userId").asLong() : user.getId();
            String userName = json.has("userName") ? json.get("userName").asText() : user.getName();
            double latitude = json.get("latitude").asDouble();
            double longitude = json.get("longitude").asDouble();
            String profileImage = json.has("profileImage") ? json.get("profileImage").asText() : null;

            // 위치 메시지 브로드캐스트
            String locationMessage = String.format(
                "{\"type\":\"location\",\"userId\":%d,\"userName\":\"%s\",\"latitude\":%f,\"longitude\":%f,\"profileImage\":\"%s\"}",
                userId, userName, latitude, longitude, profileImage != null ? profileImage : ""
            );

            TextMessage responseMessage = new TextMessage(locationMessage);

            // 같은 Meet의 모든 클라이언트에게 전송
            for (Map.Entry<Long, WebSocketSession> entry : clients.entrySet()) {
                WebSocketSession clientSession = entry.getValue();
                if (clientSession != null && clientSession.isOpen()) {
                    try {
                        clientSession.sendMessage(responseMessage);
                    } catch (IOException e) {
                        System.err.println("❌ 메시지 전송 실패: userId=" + entry.getKey());
                    }
                }
            }

            System.out.println("📍 위치 전송: meetId=" + meetId + ", userId=" + userId + ", lat=" + latitude + ", lng=" + longitude);

        } catch (Exception e) {
            System.err.println("❌ 위치 메시지 처리 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        try {
            UsersDto user = getUserFromSession(session);
            Long meetId = getMeetId(session);
            Long userId = user.getId();

            Map<Long, WebSocketSession> clients = getClientsInMeet(meetId);
            if (clients != null) {
                clients.remove(userId);
                System.out.println("✅ LocationWebSocket 종료: meetId=" + meetId + ", userId=" + userId);

                // 방에 아무도 없으면 방 삭제
                if (clients.isEmpty()) {
                    meetClients.remove(meetId);
                    System.out.println("🗑️ Meet 방 삭제: meetId=" + meetId);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ LocationWebSocket 종료 처리 실패: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable e) throws Exception {
        System.err.println("❌ LocationWebSocket 전송 오류: " + e.getMessage());
        e.printStackTrace();
    }
}

