package com.ict.springboot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.ict.springboot.dto.CheerSongDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.CheerSongService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "응원곡", description = "응원곡 생성 및 관리 API")
@RestController
@RequestMapping("/api/cheer-songs")
@RequiredArgsConstructor
public class CheerSongController {

    private final CheerSongService cheerSongService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${fastapi.server-url:http://localhost:8020}")
    private String fastapiUrl;

    // =============================================
    // YouTube 검색 (FastAPI 프록시)
    // =============================================

    @Operation(summary = "YouTube 음악 검색", description = "YouTube에서 음악을 검색합니다.")
    @ApiResponse(responseCode = "200", description = "검색 성공")
    @PostMapping("/youtube/search")
    public ResponseEntity<?> searchYoutubeMusic(@RequestBody Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                fastapiUrl + "/api/youtube/search",
                entity,
                Map.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "YouTube 첫 번째 검색 결과", description = "YouTube에서 첫 번째 검색 결과를 반환합니다.")
    @ApiResponse(responseCode = "200", description = "검색 성공")
    @PostMapping("/youtube/search-first")
    public ResponseEntity<?> searchFirstYoutubeMusic(@RequestBody Map<String, Object> request) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                fastapiUrl + "/api/youtube/search-first",
                entity,
                Map.class
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // =============================================
    // 응원곡 관리
    // =============================================

    @Operation(summary = "응원곡 저장", description = "AI로 생성한 응원곡을 저장합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "저장 성공",
            content = @Content(schema = @Schema(implementation = CheerSongDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "JWT")
    @PostMapping
    public ResponseEntity<CheerSongDto> saveCheerSong(
            @Parameter(description = "응원곡 정보", required = true) @RequestBody CheerSongDto dto,
            HttpServletRequest request) {
        UsersDto user = (UsersDto) request.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        CheerSongDto saved = cheerSongService.saveCheerSong(dto, user.getId());
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "공유된 응원곡 목록 조회", description = "모든 사용자가 공유한 응원곡 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "조회 성공",
        content = @Content(schema = @Schema(implementation = CheerSongDto.class)))
    @GetMapping("/shared")
    public ResponseEntity<List<CheerSongDto>> getSharedCheerSongs() {
        List<CheerSongDto> songs = cheerSongService.getSharedCheerSongs();
        return ResponseEntity.ok(songs);
    }

    @Operation(summary = "내 응원곡 목록 조회", description = "로그인한 사용자의 응원곡 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CheerSongDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "JWT")
    @GetMapping("/my")
    public ResponseEntity<List<CheerSongDto>> getMyCheerSongs(HttpServletRequest request) {
        UsersDto user = (UsersDto) request.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<CheerSongDto> songs = cheerSongService.getUserCheerSongs(user.getId());
        return ResponseEntity.ok(songs);
    }

    @Operation(summary = "응원곡 삭제", description = "응원곡을 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @SecurityRequirement(name = "JWT")
    @DeleteMapping("/{songId}")
    public ResponseEntity<Void> deleteCheerSong(
            @Parameter(description = "응원곡 ID", required = true) @PathVariable Long songId,
            HttpServletRequest request) {
        UsersDto user = (UsersDto) request.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        cheerSongService.deleteCheerSong(songId, user.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "응원곡 공유 토글", description = "응원곡의 공유 상태를 변경합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "변경 성공",
            content = @Content(schema = @Schema(implementation = CheerSongDto.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @SecurityRequirement(name = "JWT")
    @PostMapping("/{songId}/share")
    public ResponseEntity<CheerSongDto> toggleShare(
            @Parameter(description = "응원곡 ID", required = true) @PathVariable Long songId,
            HttpServletRequest request) {
        UsersDto user = (UsersDto) request.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        CheerSongDto updated = cheerSongService.toggleShare(songId, user.getId());
        return ResponseEntity.ok(updated);
    }
}
