package com.ict.springboot.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.DiaryDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.DiaryEntity;
import com.ict.springboot.repository.DiaryRepository;
import com.ict.springboot.service.DiaryService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/diary")
@RequiredArgsConstructor
public class DiaryController {

    private final DiaryService diaryService;
    private final DiaryRepository diaryRepository;

    // 티켓 검증
    @PostMapping("/verify-ticket")
    public ResponseEntity<?> verifyTicket(@RequestBody Map<String, String> body) {
        try {
            String base64Ticket = body.get("ticket_base64");
            if (base64Ticket == null || base64Ticket.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "ticket_base64 값이 비어 있습니다."));
            }

            Map<String, Object> result = diaryService.verifyTicket(base64Ticket);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // 일기 생성
    @PostMapping("/create")
    public ResponseEntity<?> createDiary(@RequestBody Map<String,Object> body, HttpServletRequest request){
        try{
            UsersDto user = (UsersDto) request.getAttribute("user");
            if (user == null) return ResponseEntity.status(401).body(Map.of("error", "로그인 필요"));

            Map<String,Object> ticketData = (Map<String,Object>) body.get("ticket_data");
            List<String> photos = (List<String>) body.get("photo_base64_list");

            DiaryDto saved = diaryService.createDiary(ticketData, photos, user.toEntity());
            return ResponseEntity.ok(saved);
        }catch(Exception e){
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping
    public ResponseEntity<?> getDiaryByDate(
            @RequestParam("date") String dateStr,
            HttpServletRequest request) {

        try {
            UsersDto user = (UsersDto) request.getAttribute("user");
            if (user == null) {
                return ResponseEntity.status(401).body(Map.of("error", "로그인 필요"));
            }

            LocalDate date = LocalDate.parse(dateStr);

            List<DiaryEntity> allDiaries = diaryRepository.findByUserIdOrderByDiaryDateAsc(user.getId());

            int cumulativeGames = 0;
            int cumulativeWins = 0;
            DiaryEntity diary = null;

            for (DiaryEntity d : allDiaries) {
                int game = d.getTotalGames() != null ? d.getTotalGames() : 0;
                int win = d.getTotalWins() != null ? d.getTotalWins() : 0;
                cumulativeGames += game;
                cumulativeWins += win;

                if (d.getDiaryDate().equals(date)) {
                    diary = d;
                }
            }

            if (allDiaries.isEmpty() || diary == null) {
                return ResponseEntity.ok(Map.of("message", "no_diary"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("id", diary.getId());
            response.put("date", diary.getDiaryDate());
            response.put("ticket_url", diary.getTicketUrl());
            response.put("photo_urls", diary.getPhotoUrls());
            response.put("content", diary.getContent());
            response.put("schedule_id", diary.getSchedule().getId());
            response.put("totalGames", cumulativeGames);
            response.put("totalWins", cumulativeWins);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDiary(@PathVariable Long id) {
        try {
            diaryService.deleteDiary(id);
            return ResponseEntity.ok(Map.of("message", "deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}