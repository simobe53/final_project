package com.ict.springboot.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict.springboot.dto.ChatbotDto;
import com.ict.springboot.dto.PlayerDto;
import com.ict.springboot.entity.PlayerEntity;
import com.ict.springboot.repository.PlayerRepository;
import com.ict.springboot.repository.SimulationGameStateRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatbotBiasService {
    private final PlayerRepository playerRepository;
    private final SimulationGameStateRepository simulationGameStateRepository;

    @Autowired
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fastapi.server-url}")
    private String fastapiServerUrl;

    //PlayerDto batter = playerRepository.findBypNo(null);

    public void getData(Long simuationId) {

    }

    // //FastAPI와 연결해서 채팅을 받는 서비스
    // public ChatbotDto getAIResponse(ChatbotDto requestDto) throws Exception {
    //     // FastAPI 서버 URL
    //     String url = fastapiServerUrl + "/ai/chat";

    //     // FastAPI 서버에 요청을 보내고, 응답을 String으로 받음
    //     String rawResponse = restTemplate.postForObject(url, requestDto, String.class);
        
    //     // 받은 String 응답을 ChatbotDto 객체로 변환하여 반환
    //     return objectMapper.readValue(rawResponse, ChatbotDto.class);
    // }
}