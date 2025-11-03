package com.ict.springboot.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict.springboot.dto.ChatbotDto;
import com.ict.springboot.dto.ChatbotPlaceDto;
import com.ict.springboot.dto.ChatbotPlayerDataDto;
import com.ict.springboot.entity.PlayerEntity;
import com.ict.springboot.repository.PlayerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AIChatbotService {
    private final PlayerRepository playerRepository;

    @Autowired
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${fastapi.server-url}")
    private String fastapiServerUrl;

    @Value("${kakao.rest_api_key}")
    private String kakaoApiKey;

    private static final String KAKAO_SEARCH_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";

    //FastAPI와 연결해서 채팅을 받는 서비스
    public ChatbotDto getAIResponse(ChatbotDto requestDto) throws Exception {
        // FastAPI 서버 URL
        String url = fastapiServerUrl + "/ai/chat";

        // FastAPI 서버에 요청을 보내고, 응답을 String으로 받음
        String rawResponse = restTemplate.postForObject(url, requestDto, String.class);

        // 받은 String 응답을 ChatbotDto 객체로 변환하여 반환
        return objectMapper.readValue(rawResponse, ChatbotDto.class);
    }

    //챗봇이 선수 정보를 검색할 때 사용하는 서비스
    public ChatbotPlayerDataDto playerDataSearch(String playerName) {
        List<PlayerEntity> playerEntities = playerRepository.findByPlayerName(playerName);
        
        // 검색 결과가 없으면 예외를 던진다.
        if (playerEntities.isEmpty()) {
            throw new RuntimeException("선수를 찾을 수 없습니다.");
        }

        // 무조건 첫 번째 선수만 선택한다. (동명이인 방지)
        PlayerEntity player = playerEntities.get(0);
        
        // 바로 DTO 빌더를 생성한다.
        ChatbotPlayerDataDto.ChatbotPlayerDataDtoBuilder builder = ChatbotPlayerDataDto.builder()
                .playerName(player.getPlayerName())
                .teamName(player.getTeam().getName())
                .position(player.getPosition());

        // 포지션을 구분하여 스탯을 빌더에 추가한다. 투수면 바로 넣고 타자라면 밑에서 넣는다.
        if ("pitcher".equalsIgnoreCase(player.getPlayerType())) {
            builder.pitcherStats(ChatbotPlayerDataDto.PitcherStats.builder()
                    .era(player.getPEra())
                    .wins(player.getPW())
                    .losses(player.getPL())
                    .strikeouts(player.getPSo())
                    .inningsPitched(player.getPIp())
                    .whip(player.getPWhip())
                    .fip(player.getPFip())
                    .hitsAllowed(player.getPH())
                    .homeRunsAllowed(player.getPHr())
                    .walksAllowed(player.getPBb())
                    .build());
        } else {
            builder.batterStats(ChatbotPlayerDataDto.BatterStats.builder()
                    .battingAverage(player.getBAvg())
                    .homeRuns(player.getBHr())
                    .rbi(player.getBRbi())
                    .hits(player.getBH())
                    .atBats(player.getBAb())
                    .ops(player.getBOps())
                    .doubles(player.getB2B())
                    .triples(player.getB3B())
                    .stolenBases(player.getBSb())
                    .doublePlays(player.getBGdp())
                    .sacrificeFly(player.getBSf())
                    .strikeouts(player.getBSo())
                    .walks(player.getBBb())
                    .runs(player.getBR())
                    .build());
        }

        // 최종적으로 빌드된 단일 DTO 객체를 반환한다.
        return builder.build();
    }

    

    // 플레이어가 입력한 정보로 맛집을 찾는 서비스.
    public List<ChatbotPlaceDto> searchByKeyword(String place_name) throws Exception {
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(KAKAO_SEARCH_URL)
            .queryParam("query", place_name)
            .queryParam("size", 15);

        URI targetUrl = builder.encode(StandardCharsets.UTF_8).build().toUri();

        ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode documents = root.path("documents");

        List<ChatbotPlaceDto> placeList = new ArrayList<>();
        
        // 카카오맵 api에서 가져온 값을 빌드
        for (JsonNode doc : documents) {
            placeList.add(ChatbotPlaceDto.builder()
            .placeName(doc.path("place_name").asText())
            .roadAddressName(doc.path("road_address_name").asText())
            .placeUrl(doc.path("place_url").asText())
            .build());
        }

        // 빌드한 리스트를 셔플
        Collections.shuffle(placeList);

        // 최대 3개까지만 반환
        return placeList.stream()
            .limit(3)
            .collect(Collectors.toList());
    }
}
