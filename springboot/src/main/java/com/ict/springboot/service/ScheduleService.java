package com.ict.springboot.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict.springboot.dto.ScheduleDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.ScheduleEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.ScheduleRepository;
import com.ict.springboot.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScheduleService {
    
    @Value("${fastapi.server-url}")
    private String fastapiServerUrl;

    private static final String HIGHLIGHTS_SUMMARIZE_PATH = "/highlights/summarize";

    private final ScheduleRepository scheRepo;
    private final UsersRepository userRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Timeout 설정된 RestTemplate 생성
     */
    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);  // 연결 타임아웃: 60초
        factory.setReadTimeout(300000);    // 읽기 타임아웃: 300초 (5분) - 하이라이트 요약 시간 고려
        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }

    public List<ScheduleDto> getListByDate(LocalDate dt) {
        List<ScheduleDto> list = scheRepo.findByGameDateOrderByGameTimeDesc(dt).stream().map(entity -> ScheduleDto.toDto(entity)).toList();
        return list;
    }

    public List<ScheduleDto> getFutureList() {
        List<ScheduleDto> list = scheRepo.findWithStadiumOrderByGameTimeDesc().stream().map(entity -> ScheduleDto.toDto(entity)).toList();
        return list;
    }

    public ScheduleDto getById(Long id) {
        return ScheduleDto.toDto(scheRepo.findById(id).orElseGet(()->null));
    }

    public List<ScheduleDto> getLatestSchedules(int limit, boolean highlightOnly, UsersDto loginUser) {
        // 로그인하지 않은 경우 최신 일정 반환
        if (loginUser == null) {
            if (highlightOnly) return scheRepo.findLatestWithHighlightUrl(PageRequest.of(0, limit)).stream().map(entity->ScheduleDto.toDto(entity)).toList();
            return scheRepo.findLatest(PageRequest.of(0, limit)).stream().map(entity->ScheduleDto.toDto(entity)).toList();
        }

        // 사용자의 팀 정보 조회
        UsersEntity userEntity = userRepo.findByAccount(loginUser.getAccount()).orElse(null);
        
        // 팀 정보가 없으면 최신 일정 반환
        if (userEntity == null || userEntity.getTeam() == null) {
            if (highlightOnly) return scheRepo.findLatestWithHighlightUrl(PageRequest.of(0, limit)).stream().map(entity->ScheduleDto.toDto(entity)).toList();
            return scheRepo.findLatest(PageRequest.of(0, limit)).stream().map(entity->ScheduleDto.toDto(entity)).toList();
        }

        String teamName = userEntity.getTeam().getName().split(" ")[0];
        if (highlightOnly) return scheRepo.findLatestByTeamWithHighlightUrl(teamName, PageRequest.of(0, limit)).stream().map(entity->ScheduleDto.toDto(entity)).toList();
        return scheRepo.findLatestByTeam(teamName, PageRequest.of(0, limit)).stream().map(entity->ScheduleDto.toDto(entity)).toList();
    }

    /**
     * 하이라이트 영상 AI 요약 생성
     * @param scheduleId 일정 ID
     * @return 생성된 요약 텍스트
     * @throws RuntimeException 요약 생성 실패 시
     */
    public String generateHighlightSummary(Long scheduleId) {
        try {
            ScheduleEntity schedule = scheRepo.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("일정을 찾을 수 없습니다."));

            if (schedule.getHighlightUrl() == null || schedule.getHighlightUrl().isEmpty()) {
                throw new RuntimeException("하이라이트 영상 URL이 없습니다.");
            }

            if (schedule.getHighlightSummary() != null && !schedule.getHighlightSummary().isEmpty()) {
                return schedule.getHighlightSummary();
            }

            String summary = callFastApiForSummary(schedule.getHighlightUrl());
            schedule.setHighlightSummary(summary);
            scheRepo.save(schedule);
            
            return summary;

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("하이라이트 요약 생성 중 오류 발생: " + e.getMessage(), e);
        }
    }

    /**
     * FastAPI에 요약 생성 요청
     */
    private String callFastApiForSummary(String highlightUrl) {
        try {
            RestTemplate restTemplate = createRestTemplate();
            String fastapiUrl = fastapiServerUrl + HIGHLIGHTS_SUMMARIZE_PATH;
            
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("video_url", highlightUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(fastapiUrl, entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);
            
            if (!jsonNode.has("success") || !jsonNode.get("success").asBoolean()) {
                String error = jsonNode.has("error") ? jsonNode.get("error").asText() : "알 수 없는 오류";
                
                if (error.contains("자막이 없") || error.contains("No transcript") || 
                    error.contains("Subtitles are disabled")) {
                    throw new RuntimeException("이 영상에는 자막이 없어 요약을 생성할 수 없습니다.");
                }
                
                throw new RuntimeException("FastAPI 요약 생성 실패: " + error);
            }

            if (!jsonNode.has("summary")) {
                throw new RuntimeException("FastAPI 응답에 요약 텍스트가 없습니다.");
            }

            String summary = jsonNode.get("summary").asText();
            
            if (summary == null || summary.isEmpty()) {
                throw new RuntimeException("생성된 요약이 비어있습니다.");
            }

            return summary;

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("FastAPI 서버 오류: " + e.getStatusCode() + " - " + e.getMessage());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("FastAPI 통신 중 오류 발생: " + e.getMessage(), e);
        }
    }

}
