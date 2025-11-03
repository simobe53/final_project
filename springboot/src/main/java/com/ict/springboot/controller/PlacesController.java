package com.ict.springboot.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.ict.springboot.dto.PlacesDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.PlacesService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlacesController {

    private final PlacesService placesService;
    private final RestTemplate restTemplate;

    @Value("${fastapi.server-url}")
    private String fastapiServerUrl;

    //전체 조회 (로그인 유저 지역만)
    @GetMapping("")
    public ResponseEntity<?> getAllPlaces(@RequestParam Map<String, String> params) {
        return ResponseEntity.ok(placesService.getAll(params));
    }

    //상세 조회
    @GetMapping("/{id}")
    public PlacesDto getPlacesById(@PathVariable Long id) {
        return placesService.getById(id);
    }

    //핫한 플레이스 조회 (메인용)
    @GetMapping("/top")
    public ResponseEntity<?> getPlacesForMain(@RequestParam(defaultValue = "5") int limit, HttpServletRequest request) {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(placesService.getPlacesByRanksScraps(limit, loginUser, null));
    }

    //등록
    @PostMapping("")
    public PlacesDto createPlaces(@RequestBody PlacesDto dto) {
        return placesService.create(dto);
    } 

    //수정
    @PutMapping("/{id}")
    public PlacesDto updatePlace(@PathVariable Long id, @RequestBody PlacesDto dto) {
        return placesService.update(dto, id);
    } 

    //삭제
    @DeleteMapping("/{id}")
    public PlacesDto deletePlaces(@PathVariable Long id) throws Exception {
        return placesService.delete(id);
    }

    //리뷰 요약 (FastAPI 프록시)
    @PostMapping("/reviews/summarize")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> summarizeReviews(@RequestBody Map<String, Object> request) {
        try {
            String url = fastapiServerUrl + "/api/reviews/summarize";
            Map<String, Object> response = (Map<String, Object>) restTemplate.postForObject(url, request, Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorMap = new java.util.HashMap<>();
            errorMap.put("error", "FastAPI 서버 연결 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

}
