package com.ict.springboot.controller;

import com.ict.springboot.dto.CheerSongDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.CheerSongService;
import com.ict.springboot.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@Slf4j
public class AIController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CheerSongService cheerSongService;

    @Autowired
    private NotificationService notificationService;

    @Value("${fastapi.server-url}")
    private String fastapiServerUrl;

    @PostMapping("/generate-image")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> generateImage(@RequestBody Map<String, Object> request) {
        try {
            String url = fastapiServerUrl + "/generate-image";
            Map<String, Object> response = (Map<String, Object>) restTemplate.postForObject(url, request, Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "AI 서버 연결 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    @PostMapping("/suno/generate")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> sunoGenerate(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        UsersDto user = (UsersDto) httpRequest.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다"));
        }

        try {
            // 사용자 정보 추가
            request.put("user_id", String.valueOf(user.getId()));

            // FastAPI로 응원곡 생성 요청
            String url = fastapiServerUrl + "/suno/generate";
            Map<String, Object> response = (Map<String, Object>) restTemplate.postForObject(url, request, Map.class);

            // 생성 성공 시 DB에 자동 저장
            try {
                CheerSongDto dto = new CheerSongDto();
                dto.setTeamId(request.getOrDefault("team_id", "SK"));
                dto.setPlayerName(request.getOrDefault("player_name", "선수"));
                dto.setMood(request.getOrDefault("mood", ""));
                dto.setTitle((String) response.get("Title"));
                dto.setLyrics((String) response.get("Lyrics"));
                dto.setAudioUrl((String) response.get("Audio URL"));
                dto.setDuration(((Number) response.get("Duration")).intValue());
                dto.setIsShared(false);

                CheerSongDto saved = cheerSongService.saveCheerSong(dto, user.getId());
                log.info("✅ 응원곡 저장 완료: songId={}, userId={}", saved.getSongId(), user.getId());

                // 성공 알림 전송
                try {
                    notificationService.sendToUser(user.getId(),
                        com.ict.springboot.dto.NotificationDto.builder()
                            .notificationType("SUNO_SUCCESS")
                            .title("✅ 응원곡 생성 완료")
                            .message(dto.getPlayerName() + "님의 응원곡이 성공적으로 생성되었습니다!")
                            .link("/my/cheer-song")
                            .isUrgent(true)
                            .build()
                    );
                } catch (Exception notifError) {
                    log.error("⚠️ 성공 알림 전송 실패: {}", notifError.getMessage());
                }

                // 응답에 저장된 songId 추가
                response.put("songId", saved.getSongId());
                response.put("saved", true);

            } catch (Exception saveError) {
                log.error("❌ 응원곡 저장 실패: userId={}, error={}", user.getId(), saveError.getMessage());

                // 에러 원인 분석
                String errorReason = saveError.getMessage();
                String userMessage;

                if (errorReason.contains("사용자를 찾을 수 없습니다") || errorReason.contains("User") || errorReason.contains("권한")) {
                    userMessage = "권한 문제로 저장에 실패했습니다. 로그인 상태를 확인해주세요.";
                } else if (errorReason.contains("팀을 찾을 수 없습니다") || errorReason.contains("Team")) {
                    userMessage = "팀 정보를 찾을 수 없어 저장에 실패했습니다. 팀을 다시 선택해주세요.";
                } else if (errorReason.contains("ConstraintViolation") || errorReason.contains("duplicate") || errorReason.contains("Duplicate")) {
                    userMessage = "이미 동일한 응원곡이 존재합니다.";
                } else if (errorReason.contains("DataIntegrity") || errorReason.contains("SQL")) {
                    userMessage = "데이터베이스 오류로 저장에 실패했습니다. 잠시 후 다시 시도해주세요.";
                } else {
                    userMessage = "응원곡은 생성되었지만 저장에 실패했습니다: " + errorReason;
                }

                // 저장 실패 알림 전송
                try {
                    notificationService.sendToUser(user.getId(),
                        com.ict.springboot.dto.NotificationDto.builder()
                            .notificationType("SUNO_SAVE_ERROR")
                            .title("⚠️ 응원곡 저장 실패")
                            .message(userMessage)
                            .link("/my/cheer-song")
                            .isUrgent(true)
                            .build()
                    );
                } catch (Exception notifError) {
                    log.error("⚠️ 알림 전송 실패: {}", notifError.getMessage());
                }

                response.put("saved", false);
                response.put("saveError", userMessage);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ AI 서버 연결 실패: {}", e.getMessage());
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "AI 서버 연결 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    @PostMapping("/suno/upload-cover")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> sunoUploadCover(@RequestBody Map<String, Object> request) {
        try {
            String url = fastapiServerUrl + "/api/ai/suno/upload-cover";
            Map<String, Object> response = (Map<String, Object>) restTemplate.postForObject(url, request, Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "AI 서버 연결 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    @PostMapping("/suno/youtube-cover")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> sunoYoutubeCover(@RequestBody Map<String, Object> request, HttpServletRequest httpRequest) {
        UsersDto user = (UsersDto) httpRequest.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "로그인이 필요합니다"));
        }

        try {
            // 사용자 정보 추가
            request.put("user_id", String.valueOf(user.getId()));

            // FastAPI로 YouTube 커버 생성 요청
            String url = fastapiServerUrl + "/api/ai/suno/youtube-cover";
            Map<String, Object> response = (Map<String, Object>) restTemplate.postForObject(url, request, Map.class);

            // 생성 성공 시 DB에 자동 저장
            try {
                CheerSongDto dto = new CheerSongDto();
                dto.setTeamId(String.valueOf(request.getOrDefault("team_id", "SK")));
                dto.setPlayerName((String) request.getOrDefault("player_name", "선수"));
                dto.setMood((String) request.getOrDefault("mood", ""));
                dto.setTitle((String) response.get("Title"));
                dto.setLyrics((String) response.get("New Lyrics"));
                dto.setAudioUrl((String) response.get("Audio URL"));
                dto.setDuration(((Number) response.get("Duration")).intValue());
                dto.setIsShared(false);
                dto.setSourceType("YOUTUBE_COVER");

                CheerSongDto saved = cheerSongService.saveCheerSong(dto, user.getId());
                log.info("✅ YouTube 커버 저장 완료: songId={}, userId={}", saved.getSongId(), user.getId());

                // 성공 알림 전송
                try {
                    notificationService.sendToUser(user.getId(),
                        com.ict.springboot.dto.NotificationDto.builder()
                            .notificationType("SUNO_SUCCESS")
                            .title("✅ YouTube 응원가 생성 완료")
                            .message(dto.getPlayerName() + "님의 YouTube 커버 응원곡이 성공적으로 생성되었습니다!")
                            .link("/my/cheer-song")
                            .isUrgent(true)
                            .build()
                    );
                } catch (Exception notifError) {
                    log.error("⚠️ 성공 알림 전송 실패: {}", notifError.getMessage());
                }

                // 응답에 저장된 songId 추가
                response.put("songId", saved.getSongId());
                response.put("saved", true);

            } catch (Exception saveError) {
                log.error("❌ YouTube 커버 저장 실패: userId={}, error={}", user.getId(), saveError.getMessage());

                // 에러 원인 분석
                String errorReason = saveError.getMessage();
                String userMessage;

                if (errorReason.contains("사용자를 찾을 수 없습니다") || errorReason.contains("User") || errorReason.contains("권한")) {
                    userMessage = "권한 문제로 저장에 실패했습니다. 로그인 상태를 확인해주세요.";
                } else if (errorReason.contains("팀을 찾을 수 없습니다") || errorReason.contains("Team")) {
                    userMessage = "팀 정보를 찾을 수 없어 저장에 실패했습니다. 팀을 다시 선택해주세요.";
                } else if (errorReason.contains("ConstraintViolation") || errorReason.contains("duplicate") || errorReason.contains("Duplicate")) {
                    userMessage = "이미 동일한 응원곡이 존재합니다.";
                } else if (errorReason.contains("DataIntegrity") || errorReason.contains("SQL")) {
                    userMessage = "데이터베이스 오류로 저장에 실패했습니다. 잠시 후 다시 시도해주세요.";
                } else {
                    userMessage = "YouTube 응원곡은 생성되었지만 저장에 실패했습니다: " + errorReason;
                }

                // 저장 실패 알림 전송
                try {
                    notificationService.sendToUser(user.getId(),
                        com.ict.springboot.dto.NotificationDto.builder()
                            .notificationType("SUNO_SAVE_ERROR")
                            .title("⚠️ YouTube 응원곡 저장 실패")
                            .message(userMessage)
                            .link("/my/cheer-song")
                            .isUrgent(true)
                            .build()
                    );
                } catch (Exception notifError) {
                    log.error("⚠️ 알림 전송 실패: {}", notifError.getMessage());
                }

                response.put("saved", false);
                response.put("saveError", userMessage);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ YouTube 응원가 생성 실패: {}", e.getMessage());

            // 생성 실패 알림 전송
            try {
                String errorMessage = e.getMessage();
                String userMessage;

                if (errorMessage.contains("SSL") || errorMessage.contains("ssl")) {
                    userMessage = "네트워크 연결 오류로 YouTube 다운로드에 실패했습니다. 잠시 후 다시 시도해주세요.";
                } else if (errorMessage.contains("proxy") || errorMessage.contains("Proxy")) {
                    userMessage = "프록시 연결 오류가 발생했습니다. 네트워크 설정을 확인해주세요.";
                } else if (errorMessage.contains("YouTube") || errorMessage.contains("youtube") || errorMessage.contains("yt-dlp")) {
                    userMessage = "YouTube 다운로드 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                } else if (errorMessage.contains("timeout") || errorMessage.contains("Timeout")) {
                    userMessage = "처리 시간이 초과되었습니다. 다시 시도해주세요.";
                } else {
                    userMessage = "YouTube 응원가 생성 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                }

                notificationService.sendToUser(user.getId(),
                    com.ict.springboot.dto.NotificationDto.builder()
                        .notificationType("SUNO_ERROR")
                        .title("❌ YouTube 응원가 생성 실패")
                        .message(userMessage)
                        .link("/my/cheer-song")
                        .isUrgent(true)
                        .build()
                );
            } catch (Exception notifError) {
                log.error("⚠️ 실패 알림 전송 실패: {}", notifError.getMessage());
            }

            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "YouTube 응원가 생성 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    @PostMapping("/translate-prompt")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> translatePrompt(@RequestBody Map<String, Object> request) {
        try {
            String url = fastapiServerUrl + "/translate-prompt";
            Map<String, Object> response = (Map<String, Object>) restTemplate.postForObject(url, request, Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "AI 서버 연결 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

    @GetMapping("/articles")
    @SuppressWarnings("unchecked")
    public ResponseEntity<Map<String, Object>> getArticles(@RequestParam(required = false) Long simulation_id) {
        try {
            String url = fastapiServerUrl + "/articles";
            if (simulation_id != null) {
                url += "?simulation_id=" + simulation_id;
            }
            Map<String, Object> response = (Map<String, Object>) restTemplate.getForObject(url, Map.class);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "AI 서버 연결 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorMap);
        }
    }

}
