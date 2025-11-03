package com.ict.springboot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ict.springboot.dto.NewsDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.UsersRepository;
import com.ict.springboot.service.NewsService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {
    
    private final NewsService newsService;
    private final UsersRepository usersRepository;

    @Value("${fastapi.server-url}")
    private String fastapiServerUrl;
    
    /**
     * 사용자의 응원 팀 뉴스 조회
     * @param limit 조회할 뉴스 개수 (기본 20개)
     * @param offset 건너뛸 뉴스 개수 (기본 0개)
     * @return 응원 팀의 최신 뉴스 리스트
     */
    @GetMapping("/my-team")
    public ResponseEntity<?> getMyTeamNews(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request) {
        
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            // 로그인하지 않은 경우 최신 뉴스 반환
            List<NewsDto> news = newsService.getLatestNews(limit, offset);
            return ResponseEntity.ok(news);
        }
        
        // 사용자의 팀 정보 조회
        UsersEntity userEntity = usersRepository.findByAccount(loginUser.getAccount())
                .orElse(null);
        
        if (userEntity == null || userEntity.getTeam() == null) {
            // 팀 정보가 없으면 최신 뉴스 반환
            List<NewsDto> news = newsService.getLatestNews(limit, offset);
            return ResponseEntity.ok(news);
        }
        
        String teamId = userEntity.getTeam().getIdKey();
        List<NewsDto> news = newsService.getNewsByTeam(teamId, limit, offset);
        
        return ResponseEntity.ok(news);
    }
    
    /**
     * 특정 팀의 뉴스 조회
     * @param teamId 팀 ID
     * @param limit 조회할 뉴스 개수 (기본 20개)
     * @param offset 건너뛸 뉴스 개수 (기본 0개)
     * @return 팀의 최신 뉴스 리스트
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<NewsDto>> getNewsByTeam(
            @PathVariable String teamId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        List<NewsDto> news = newsService.getNewsByTeam(teamId, limit, offset);
        return ResponseEntity.ok(news);
    }
    
    /**
     * 전체 최신 뉴스 조회
     * @param limit 조회할 뉴스 개수 (기본 20개)
     * @param offset 건너뛸 뉴스 개수 (기본 0개)
     * @return 전체 최신 뉴스 리스트
     */
    @GetMapping("/latest")
    public ResponseEntity<List<NewsDto>> getLatestNews(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        
        List<NewsDto> news = newsService.getLatestNews(limit, offset);
        return ResponseEntity.ok(news);
    }
    
    /**
     * 특정 뉴스 상세 조회
     * @param id 뉴스 ID
     * @return 뉴스 상세 정보
     */
    @GetMapping("/{id}")
    public ResponseEntity<NewsDto> getNewsById(@PathVariable Long id) {
        NewsDto news = newsService.getNewsById(id);
        if (news == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(news);
    }

    /**
     * 단일 뉴스 AI 요약 생성
     * @param requestBody 뉴스 데이터 { news: { title, content, team_name } }
     * @return 요약 결과
     */
    @PostMapping("/summarize")
    public ResponseEntity<?> summarizeNews(@RequestBody Map<String, Object> requestBody) {
        try {
            Map<String, Object> newsData = (Map<String, Object>) requestBody.get("news");
            if (newsData == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "뉴스 데이터가 없습니다."
                ));
            }

            Map<String, Object> result = newsService.generateNewsSummary(newsData);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "서버 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    /**
     * 뉴스 ID로 AI 요약 생성
     * @param id 뉴스 ID
     * @return 요약 결과
     */
    @PostMapping("/{id}/summarize")
    public ResponseEntity<?> summarizeNewsById(@PathVariable Long id) {
        try {
            Map<String, Object> result = newsService.generateNewsSummaryById(id);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "서버 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}

