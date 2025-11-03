package com.ict.springboot.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
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

import com.ict.springboot.dto.SimulationDto;
import com.ict.springboot.dto.SimulationRequestDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.SimulationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "시뮬레이션", description = "야구 시뮬레이션 관리 API")
@RestController
@RequestMapping("/api/simulations")
@RequiredArgsConstructor
public class SimulationController {

    private final SimulationService simulationService;
    
    @Operation(summary = "시뮬레이션 목록 조회", description = "모든 시뮬레이션 조회 (파라미터로 필터링 가능)")
    @GetMapping("")
    public ResponseEntity<?> getAllSimulations(@RequestParam Map<String, String> params, HttpServletRequest request) {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        // if (loginUser == null) {
        //     return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        // }
        return ResponseEntity.ok(simulationService.getAll(loginUser, params));
    }
    
    @Operation(summary = "시뮬레이션 시작", description = "선수 선택 후 시뮬레이션 시작")
    @PostMapping("/start")
    public ResponseEntity<?> startSimulation(@RequestBody SimulationRequestDto request, HttpServletRequest httpRequest) {
        UsersDto loginUser = (UsersDto) httpRequest.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(simulationService.startSimulationWithLineup(request, loginUser));
    }
    
    @Operation(summary = "타석 결과 조회", description = "특정 시뮬레이션의 모든 타석 결과 조회")
    @GetMapping("/{simulationId}/at-bats")
    public List<Map<String, Object>> getSimulationAtBats(@PathVariable Long simulationId) {
        return simulationService.getSimulationAtBats(simulationId);
    }

    @Operation(summary = "실시간 게임 시작", description = "실시간 시뮬레이션 게임 시작")
    @PostMapping("/{simulationId}/start-game")
    public Map<String, Object> startGame(@PathVariable Long simulationId) {
        return simulationService.startRealtimeGame(simulationId);
    }

    // 백그라운드 스케줄러가 Service를 직접 호출하므로 HTTP 엔드포인트는 불필요
    // @Operation(summary = "다음 타석 진행", description = "실시간 게임 다음 타석 진행")
    // @PostMapping("/{simulationId}/next-at-bat")
    // public Map<String, Object> getNextAtBat(@PathVariable Long simulationId) {
    //     return simulationService.processNextAtBat(simulationId);
    // }

    @Operation(summary = "게임 상태 조회", description = "현재 진행 중인 게임 상태 조회")
    @GetMapping("/{simulationId}/game-state")
    public Map<String, Object> getGameState(@PathVariable Long simulationId) {
        return simulationService.getCurrentGameState(simulationId);
    }
    
    // 특정 팀이 참여한 시뮬레이션 조회
    @GetMapping("/team/{teamId}")
    public List<SimulationDto> getSimulationsByTeam(@PathVariable Long teamId) {
        return simulationService.getByTeamParticipation(teamId);
    }
    
    // 홈팀 vs 어웨이팀 매치업 조회
    @GetMapping("/matchup")
    public List<SimulationDto> getSimulationsByMatchup(@RequestParam Long hometeam, @RequestParam Long awayteam) {
        return simulationService.getByMatchup(hometeam, awayteam);
    }
    
    // 특정 기간의 시뮬레이션 조회
    @GetMapping("/date-range")
    public List<SimulationDto> getSimulationsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return simulationService.getByDateRange(startDate, endDate);
    }
    
    // 매치 ID 중복 확인
    @GetMapping("/check-match")
    public boolean checkMatchIdExists(@RequestParam String matchId) {
        return simulationService.checkExists(matchId);
    }
    
    @Operation(summary = "시뮬레이션 상세 조회")
    @GetMapping("/{id}")
    public SimulationDto getSimulationById(@PathVariable Long id) {
        return simulationService.getById(id);
    }
    
    @Operation(summary = "시뮬레이션 등록")
    @PostMapping("")
    public ResponseEntity<?> createSimulation(@RequestBody SimulationDto dto, HttpServletRequest httpRequest) {
        UsersDto loginUser = (UsersDto) httpRequest.getAttribute("user");
        return ResponseEntity.ok(simulationService.create(dto, loginUser));
    }
    
    @Operation(summary = "시뮬레이션 수정")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSimulation(@PathVariable Long id, @RequestBody SimulationDto dto, HttpServletRequest httpRequest) {
        UsersDto loginUser = (UsersDto) httpRequest.getAttribute("user");
        return ResponseEntity.ok(simulationService.update(dto, id, loginUser));
    }
    
    @Operation(summary = "시뮬레이션 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSimulation(@PathVariable Long id, HttpServletRequest httpRequest) throws Exception {
        UsersDto loginUser = (UsersDto) httpRequest.getAttribute("user");
        return ResponseEntity.ok(simulationService.delete(id, loginUser));
    }
}