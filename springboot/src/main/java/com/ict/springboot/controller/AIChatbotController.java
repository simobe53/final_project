package com.ict.springboot.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.ChatbotDto;
import com.ict.springboot.dto.ChatbotPlaceDto;
import com.ict.springboot.dto.ChatbotPlayerDataDto;
import com.ict.springboot.service.AIChatbotService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ict.springboot.dto.UsersDto;
import jakarta.servlet.http.HttpServletRequest;


@RestController
@RequestMapping("/api/ai/chat")
@RequiredArgsConstructor
public class AIChatbotController {

    private final AIChatbotService aiChatbotService;

    // 챗봇에게 메시지를 보내고 받을 때 사용하는 컨트롤러
    @PostMapping("/question")
    public ResponseEntity<ChatbotDto> chat(@RequestBody ChatbotDto request, HttpServletRequest httpRequest) {
        try {
            // JWT 필터에서 설정한 사용자 정보 가져오기
            UsersDto user = (UsersDto) httpRequest.getAttribute("user");

            // 로그인 체크
            if (user == null) {
                ChatbotDto errorResponse = ChatbotDto.builder()
                    .message("로그인이 필요합니다.")
                    .build();
                return ResponseEntity.status(401).body(errorResponse);
            }

            ChatbotDto response = aiChatbotService.getAIResponse(request);

            // 성공한 경우 보내는 메시지
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.out.println("에러");
            e.printStackTrace();
            // 실패한 경우 보내는 메시지
            ChatbotDto errorResponse = ChatbotDto.builder()
            .message("서버 처리 중 오류가 발생했습니다.")
            .build();
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // 선수 정보를 찾아올 때 사용하는 컨트롤러
    @GetMapping("/player")
    public ChatbotPlayerDataDto getPlayerData(@RequestParam(required=true) String playerName) {
    
            return aiChatbotService.playerDataSearch(playerName);
    }

    // 선수 정보를 찾아올 때 사용하는 컨트롤러
    @GetMapping("/news")
    public ChatbotPlayerDataDto getNewsData(@RequestParam(required=true) String newsId) {
    
            return aiChatbotService.playerDataSearch(newsId);
    }

    // 맛집 정보를 찾아올 때 사용하는 컨트롤러
    @GetMapping("/places")
    public ResponseEntity<?> getPlayceByKeyword(@RequestParam String place_name) {
        try {
            List<ChatbotPlaceDto> places = aiChatbotService.searchByKeyword(place_name);
            return ResponseEntity.ok(places);
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("장소 검색 중 오류가 발생했습니다.");
        }
    }
}
