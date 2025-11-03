package com.ict.springboot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.ChatDto;
import com.ict.springboot.websocket.WebSocketServer;

import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/chat/notice")
public class AIChatbotBiasController {

    private final WebSocketServer webSocketServer;
    
    @PostMapping("/{sid}")
    public ResponseEntity<Void> sendAiMessage(@PathVariable("sid") String simulationId, @RequestBody ChatDto chatDto) {
        try {
            // 방 찾기
            webSocketServer.broadcastToAll(
                chatDto.getType(),
                chatDto.getMessage(),
                chatDto.getIsHome(),
                Long.valueOf(simulationId),
                chatDto.getAudioUrl()  // audioUrl 추가
            );
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

}
