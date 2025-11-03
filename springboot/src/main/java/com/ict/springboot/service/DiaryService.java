package com.ict.springboot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.ict.springboot.dto.DiaryDto;
import com.ict.springboot.entity.DiaryEntity;
import com.ict.springboot.entity.ScheduleEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.DiaryRepository;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaryService {

    @Value("${fastapi.server-url}")
    private String fastapiServerUrl;

    private static final String VERIFY_TICKET_PATH = "/verify_ticket_url";
    private static final String ANALYZE_PHOTO_PATH = "/analyze_photo";
    private static final String GENERATE_DIARY_PATH = "/generate_diary";

    private final RestTemplate restTemplate;
    private final R2UploadService r2UploadService;
    private final DiaryRepository diaryRepository;

    public Map<String, Object> verifyTicket(String base64Ticket) {
        if (base64Ticket == null || base64Ticket.isBlank()) {
            throw new IllegalArgumentException("티켓 base64가 없습니다.");
        }

        try {
            String base64Data = base64Ticket.contains(",") ? base64Ticket.split(",")[1] : base64Ticket;
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            String uploadedUrl = r2UploadService.uploadBase64(decodedBytes, "ticket.png", "image/png");

            Map<String, String> request = new HashMap<>();
            request.put("image_url", uploadedUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);
            String verifyUrl = fastapiServerUrl + VERIFY_TICKET_PATH;
            Map<String, Object> response = restTemplate.postForObject(verifyUrl, entity, Map.class);

            if (response == null) throw new RuntimeException("FastAPI 응답이 없습니다.");
            response.put("ticket_url", uploadedUrl);
            return response;
        } catch (Exception e) {
            log.error("티켓 검증 실패: {}", e.getMessage(), e);
            throw new RuntimeException("티켓 검증 실패: " + e.getMessage(), e);
        }
    }

    public DiaryDto createDiary(Map<String, Object> ticketData, List<String> photoBase64List, UsersEntity user) {
        try {
            List<Map<String,Object>> photoAnalysisList = new ArrayList<>();
            List<String> photoUrls = new ArrayList<>();

            if (photoBase64List != null) {
                int i = 1;
                for (String base64 : photoBase64List) {
                    if (base64 == null || base64.isBlank()) continue;
                    String base64Data = base64.contains(",") ? base64.split(",")[1] : base64;
                    byte[] bytes = Base64.getDecoder().decode(base64Data);
                    String photoUrl = r2UploadService.uploadBase64(bytes, "photo_" + i + ".png", "image/png");
                    photoUrls.add(photoUrl);
                    Map<String, String> photoReq = Map.of("image_url", photoUrl);
                    String analyzeUrl = fastapiServerUrl + ANALYZE_PHOTO_PATH;
                    Map<String, Object> analysis = restTemplate.postForObject(analyzeUrl, photoReq, Map.class);
                    if (analysis != null) photoAnalysisList.add(Map.of("photo_url", photoUrl, "analysis", analysis));
                    i++;
                }
            }

            Map<String,Object> diaryReq = new HashMap<>();
            diaryReq.put("ticket_data", ticketData);
            diaryReq.put("photo_analysis", photoAnalysisList);
            diaryReq.put("game_info", ticketData.get("game_info"));
            String generateUrl = fastapiServerUrl + GENERATE_DIARY_PATH;
            Map<String,Object> diaryResp = restTemplate.postForObject(generateUrl, diaryReq, Map.class);
            String diaryText = (String) diaryResp.get("diary");
            Map<String, Object> gameInfo = (Map<String, Object>) ticketData.get("game_info");
            ScheduleEntity schedule = null;
            if (gameInfo != null && gameInfo.get("id") != null) {
                Long scheduleId = ((Number) gameInfo.get("id")).longValue();
                schedule = ScheduleEntity.builder().id(scheduleId).build();
            }

            String homeTeam = (String) gameInfo.get("home_team");
            String awayTeam = (String) gameInfo.get("away_team");
            String victoryTeam = (String) gameInfo.get("victory_team");
            String ticketUrl = (String) ticketData.get("ticket_url");
            String ticketDateStr = (String) ticketData.get("date");
            LocalDate diaryDate = LocalDate.parse(ticketDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd[ HH:mm]"));

            DiaryEntity diary = DiaryEntity.builder()
                    .diaryDate(diaryDate)
                    .ticketUrl(ticketUrl)
                    .photoUrls(photoUrls)
                    .content(diaryText)
                    .schedule(schedule)
                    .user(user)
                    .build();
            if (user.getTeam() != null) {
                String myTeam = user.getTeam().getName().trim().split(" ")[0];
                boolean isMyTeamGame = homeTeam.contains(myTeam) || awayTeam.contains(myTeam);
                diary.setTotalGames(isMyTeamGame ? 1 : 0);
                boolean isMyTeamWin = isMyTeamGame && victoryTeam.contains(myTeam);
                diary.setTotalWins(isMyTeamWin ? 1 : 0);
                log.info("myTeam:'{}', homeTeam: '{}', awayTeam: '{}', victoryTeam: '{}'", myTeam, homeTeam, awayTeam, victoryTeam);
            } else {
                diary.setTotalGames(0);
                diary.setTotalWins(0);
            }

            DiaryEntity saved = diaryRepository.save(diary);

            return DiaryDto.builder()
                    .id(saved.getId())
                    .ticketUrl(saved.getTicketUrl())
                    .photoUrls(saved.getPhotoUrls())
                    .content(saved.getContent())
                    .totalGames(saved.getTotalGames())
                    .totalWins(saved.getTotalWins())
                    .scheduleId(saved.getSchedule() != null ? saved.getSchedule().getId() : null)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("일기 생성 실패: " + e.getMessage(), e);
        }
    }
        public void deleteDiary(Long id) {
        DiaryEntity diary = diaryRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 일기입니다."));

        List<String> urlsToDelete = new ArrayList<>();
        if (diary.getTicketUrl() != null) urlsToDelete.add(diary.getTicketUrl());
        if (diary.getPhotoUrls() != null) urlsToDelete.addAll(diary.getPhotoUrls());

        diaryRepository.deleteById(id);

        for (String url : urlsToDelete) {
            try {
                String key = extractKeyFromUrl(url);
                r2UploadService.deleteFileByUrl(key);
            } catch (Exception e) {
                log.warn("⚠️ R2 파일 삭제 실패: {} ({})", url, e.getMessage());
            }
        }
    }
    private String extractKeyFromUrl(String url) {
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path.startsWith("/")) path = path.substring(1);
            return path;
        } catch (Exception e) {
            return url;
        }
    }
}