package com.ict.springboot.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict.springboot.dto.NewsDto;
import com.ict.springboot.entity.NewsEntity;
import com.ict.springboot.repository.NewsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NewsService {

    @Value("${fastapi.server-url}")
    private String fastapiServerUrl;

    private static final String NEWS_SUMMARIZE_PATH = "/api/news/summarize";
    private static final String NEWS_SUMMARIZE_MULTIPLE_PATH = "/api/news/summarize-multiple";

    private final NewsRepository newsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Timeout 설정된 RestTemplate 생성
     */
    private RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);  // 연결 타임아웃: 30초
        factory.setReadTimeout(120000);    // 읽기 타임아웃: 120초 (AI 요약 생성 시간 고려)
        return new RestTemplate(factory);
    }
    
    /**
     * 팀별 최신 뉴스 조회
     * @param teamId 팀 ID (예: LG, HH, SK 등)
     * @param limit 조회할 뉴스 개수
     * @param offset 건너뛸 뉴스 개수
     * @return 뉴스 리스트
     */
    public List<NewsDto> getNewsByTeam(String teamId, int limit, int offset) {
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);
        List<NewsEntity> newsEntities = newsRepository.findByTeamIdOrderByIdAsc(teamId, pageable);
        return newsEntities.stream()
                .map(NewsDto::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * 전체 최신 뉴스 조회
     * @param limit 조회할 뉴스 개수
     * @param offset 건너뛸 뉴스 개수
     * @return 뉴스 리스트
     */
    public List<NewsDto> getLatestNews(int limit, int offset) {
        int page = offset / limit;
        Pageable pageable = PageRequest.of(page, limit);
        List<NewsEntity> newsEntities = newsRepository.findAllByOrderByIdAsc(pageable);
        return newsEntities.stream()
                .map(NewsDto::toDto)
                .collect(Collectors.toList());
    }
    
    /**
     * ID로 뉴스 상세 조회
     * @param id 뉴스 ID
     * @return 뉴스 DTO
     */
    public NewsDto getNewsById(Long id) {
        return newsRepository.findById(id)
                .map(NewsDto::toDto)
                .orElse(null);
    }

    /**
     * 단일 뉴스 AI 요약 생성
     * @param newsData 뉴스 데이터 (title, content, team_name)
     * @return 요약 결과
     */
    public Map<String, Object> generateNewsSummary(Map<String, Object> newsData) {
        try {
            RestTemplate restTemplate = createRestTemplate();
            String fastapiUrl = fastapiServerUrl + NEWS_SUMMARIZE_PATH;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("news", newsData);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String response = restTemplate.postForObject(fastapiUrl, entity, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            Map<String, Object> result = new HashMap<>();
            result.put("success", jsonNode.has("success") ? jsonNode.get("success").asBoolean() : false);

            if (jsonNode.has("summary")) {
                result.put("summary", jsonNode.get("summary").asText());
            }
            if (jsonNode.has("error")) {
                result.put("error", jsonNode.get("error").asText());
            }

            return result;

        } catch (HttpClientErrorException e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "FastAPI 서버 오류: " + e.getStatusCode() + " - " + e.getMessage());
            return errorResult;
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "뉴스 요약 생성 중 오류 발생: " + e.getMessage());
            return errorResult;
        }
    }
    /**
     * 뉴스 ID로 AI 요약 생성
     * @param newsId 뉴스 ID
     * @return 요약 결과
     */
    public Map<String, Object> generateNewsSummaryById(Long newsId) {
        try {
            NewsEntity newsEntity = newsRepository.findById(newsId)
                    .orElseThrow(() -> new RuntimeException("뉴스를 찾을 수 없습니다."));

            Map<String, Object> newsData = new HashMap<>();
            newsData.put("title", newsEntity.getTitle());
            newsData.put("content", newsEntity.getContent());
            newsData.put("team_name", newsEntity.getTeamName() != null ? newsEntity.getTeamName() : "");

            return generateNewsSummary(newsData);

        } catch (RuntimeException e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return errorResult;
        } catch (Exception e) {
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", "뉴스 요약 생성 중 오류 발생: " + e.getMessage());
            return errorResult;
        }
    }
}

