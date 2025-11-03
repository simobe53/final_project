package com.ict.springboot.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.ict.springboot.dto.SimulationDto;
import com.ict.springboot.dto.SimulationRequestDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.PlayerEntity;
import com.ict.springboot.entity.SimulationEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.entity.AtBatEntity;
import com.ict.springboot.entity.SimulationGameStateEntity;
import com.ict.springboot.repository.PlayerRepository;
import com.ict.springboot.repository.AtBatRepository;
import com.ict.springboot.repository.SimulationRepository;
import com.ict.springboot.repository.UsersRepository;
import com.ict.springboot.repository.SimulationGameStateRepository;
import com.ict.springboot.service.QuartzSimulationScheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationService {

    private final SimulationRepository simulationRepo;
    private final UsersRepository usersRepo;
    private final PlayerRepository playerRepo;
    private final AtBatRepository atBatRepo;
    private final SimulationGameStateRepository gameStateRepo;
    private final RestTemplate restTemplate;
    private final NotificationService notificationService;
    private final QuartzSimulationScheduler quartzScheduler;

    @Value("${fastapi.server-url}")
    private String fastapiServerUrl;
    
    // 전체 조회
    public List<SimulationDto> getAll(UsersDto user, Map<String, String> params) {
        
        if (!params.isEmpty()) {
            // 특정 사용자의 시뮬레이션 조회
            if (params.containsKey("userId")) {
                List<SimulationEntity> simulationEntities = simulationRepo.findByUserIdOrderByCreatedAtDesc(Long.valueOf(params.get("userId")));
                return simulationEntities.stream().map(entity -> SimulationDto.toDto(entity)).collect(Collectors.toList());
            }
            // 홈팀별 조회
            if (params.containsKey("hometeam")) {
                List<SimulationEntity> simulationEntities = simulationRepo.findByHometeamOrderByCreatedAtDesc(Long.valueOf(params.get("hometeam")));
                return simulationEntities.stream().map(entity -> SimulationDto.toDto(entity)).collect(Collectors.toList());
            }
            // 어웨이팀별 조회
            if (params.containsKey("awayteam")) {
                List<SimulationEntity> simulationEntities = simulationRepo.findByAwayteamOrderByCreatedAtDesc(Long.valueOf(params.get("awayteam")));
                return simulationEntities.stream().map(entity -> SimulationDto.toDto(entity)).collect(Collectors.toList());
            }
            // 특정 팀이 참여한 모든 시뮬레이션 조회
            if (params.containsKey("teamId")) {
                List<SimulationEntity> simulationEntities = simulationRepo.findByTeamParticipation(Long.valueOf(params.get("teamId")));
                return simulationEntities.stream().map(entity -> SimulationDto.toDto(entity)).collect(Collectors.toList());
            }
            // 매치 ID로 조회
            if (params.containsKey("matchId")) {
                List<SimulationEntity> simulationEntities = simulationRepo.findByMatchIdOrderByCreatedAtDesc(params.get("matchId"));
                return simulationEntities.stream().map(entity -> SimulationDto.toDto(entity)).collect(Collectors.toList());
            }

            // 시작 날짜별 조회
            if (params.containsKey("showAt")) {
                String showAt = params.get("showAt");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime start = LocalDateTime.parse(showAt+" 00:00:00", formatter);
                LocalDateTime end = LocalDateTime.parse(showAt+" 23:59:59", formatter);
                List<SimulationEntity> simulationEntities = simulationRepo.findByShowAtBetween(start, end);
                return simulationEntities.stream().map(entity -> SimulationDto.toDto(entity)).collect(Collectors.toList());
            }
        }
        
        List<SimulationEntity> simulationEntities = simulationRepo.findAll();
        return simulationEntities.stream().map(entity -> SimulationDto.toDto(entity)).collect(Collectors.toList());
    }
    
    // 상세 조회
    public SimulationDto getById(Long id) {
        Optional<SimulationEntity> simulationEntity = simulationRepo.findById(id);
        return SimulationDto.toDto(simulationEntity.orElseGet(() -> null));
    }
    
    // 등록
    public SimulationDto create(SimulationDto dto, UsersDto loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        // 매치 ID 중복 체크
        if (dto.getMatchId() != null && simulationRepo.existsByMatchId(dto.getMatchId())) {
            return null;
        }
        
        // user
        Long userId = dto.getUser().getId();
        UsersEntity userEntity = usersRepo.findById(userId).orElseGet(() -> null);
        if (userEntity == null) return null;
        
        SimulationEntity simulationEntity = SimulationEntity.builder()
            .hometeam(dto.getHometeam())
            .awayteam(dto.getAwayteam())
            .homeLineup(dto.getHomeLineup())
            .awayLineup(dto.getAwayLineup())
            .matchId(dto.getMatchId())
            .user(userEntity)
            .showAt(dto.getShowAt())
            .build();
        
        simulationEntity = simulationRepo.save(simulationEntity);
        
        // Quartz로 시뮬레이션 스케줄링
        try {
            log.info("새 시뮬레이션 스케줄링 시작: simulationId={}, showAt={}", 
                    simulationEntity.getId(), simulationEntity.getShowAt());
            quartzScheduler.scheduleSimulation(simulationEntity);
            log.info("새 시뮬레이션 스케줄링 완료: simulationId={}, showAt={}", 
                    simulationEntity.getId(), simulationEntity.getShowAt());
        } catch (Exception e) {
            log.error("새 시뮬레이션 스케줄링 실패: simulationId={}", simulationEntity.getId(), e);
        }
        
        return SimulationDto.toDto(simulationEntity);
    }
    
    // 수정
    public SimulationDto update(SimulationDto dto, Long id, UsersDto loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        SimulationDto simulation = SimulationDto.toDto(simulationRepo.findById(id).orElseGet(() -> null));
        if (!"ADMIN".equals(loginUser.getRole()) && (loginUser.getId() != dto.getUser().getId())) {
            throw new RuntimeException("수정할 수 없는 시뮬레이션입니다.");
        }
        if (simulation == null) return null;
        
        // 내용이 있는 경우에만 수정
        if (dto.getHometeam() != null) simulation.setHometeam(dto.getHometeam());
        if (dto.getAwayteam() != null) simulation.setAwayteam(dto.getAwayteam());
        if (dto.getHomeLineup() != null) simulation.setHomeLineup(dto.getHomeLineup());
        if (dto.getAwayLineup() != null) simulation.setAwayLineup(dto.getAwayLineup());
        if (dto.getMatchId() != null) simulation.setMatchId(dto.getMatchId());
        if (dto.getShowAt() != null) simulation.setShowAt(dto.getShowAt());
        
        simulation.setUpdatedAt(LocalDateTime.now());
        
        SimulationEntity simulationEntity = simulationRepo.save(simulation.toEntity());
        return SimulationDto.toDto(simulationEntity);
    }
    
    // 삭제
    public SimulationDto delete(Long id, UsersDto loginUser) throws Exception {
        if (loginUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }
        SimulationEntity simulation = simulationRepo.findById(id).orElseGet(() -> null);
        if (!"ADMIN".equals(loginUser.getRole()) && (loginUser.getId() != simulation.getUser().getId())) {
            throw new RuntimeException("삭제할 수 없는 시뮬레이션입니다.");
        }
        if (simulation != null) {
            try {
                simulationRepo.deleteById(id);
                return SimulationDto.toDto(simulation);
            } catch (Exception e) {
                throw new Exception("데이터 삭제에 문제가 생겼습니다.");
            }
        }
        return null;
    }
    
    // 중복 조회 (매치 ID)
    public boolean checkExists(String matchId) {
        return simulationRepo.existsByMatchId(matchId);
    }
    
    // 관리자용 시뮬레이션 조회 (페이지네이션 포함)
    public Page<SimulationDto> getSimulationsForAdminWithPagination(int page, int size, UsersDto user) {
        if (user == null) return null;
        UsersEntity loginUser = usersRepo.findByAccount(user.getAccount()).orElseGet(() -> null);
        if (loginUser == null) return null;
        
        Pageable pageable = PageRequest.of(page, size);
        Page<SimulationEntity> simulationsPage = simulationRepo.findAllByOrderByCreatedAtDesc(pageable);
        
        Page<SimulationDto> simulationsDtoPage = simulationsPage.map(entity -> {
            SimulationDto dto = SimulationDto.toDto(entity);
            return dto;
        });
        
        return simulationsDtoPage;
    }
    
    // 특정 팀이 참여한 시뮬레이션 조회
    public List<SimulationDto> getByTeamParticipation(Long teamId) {
        List<SimulationEntity> simulationEntities = simulationRepo.findByTeamParticipation(teamId);
        return simulationEntities.stream().map(entity -> SimulationDto.toDto(entity)).collect(Collectors.toList());
    }
    
    // 홈팀 vs 어웨이팀 매치업 조회
    public List<SimulationDto> getByMatchup(Long hometeam, Long awayteam) {
        List<SimulationEntity> simulationEntities = simulationRepo.findByHometeamAndAwayteam(hometeam, awayteam);
        return simulationEntities.stream().map(entity -> SimulationDto.toDto(entity)).collect(Collectors.toList());
    }
    
    // 특정 기간의 시뮬레이션 조회
    public List<SimulationDto> getByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        List<SimulationEntity> simulationEntities = simulationRepo.findByShowAtBetween(startDate, endDate);
        return simulationEntities.stream().map(entity -> SimulationDto.toDto(entity)).collect(Collectors.toList());
    }
    
    // 시뮬레이션 엔트리 생성 (기존 방식 유지 - 엔트리 생성용)
    public Map<String, Object> startSimulationWithLineup(SimulationRequestDto request, UsersDto loginUser) {
        try {
            // 시뮬레이션 기본 정보를 DB에 저장 (엔트리 생성)
            SimulationDto simulationDto = createSimulationFromRequest(request, loginUser);
            if (simulationDto == null) {
                return createErrorResponse("시뮬레이션 생성 실패");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("data", simulationDto);
            response.put("status", "success");
            response.put("message", "시뮬레이션 엔트리가 생성되었습니다.");
            response.put("simulation_id", simulationDto.getId());
            response.put("recommendation", "이제 실시간 게임을 시작하려면 POST /api/simulations/" + simulationDto.getId() + "/start-game 을 호출하세요.");

            return response;

        } catch (Exception e) {
            System.err.println("시뮬레이션 엔트리 생성 실패: " + e.getMessage());
            return createErrorResponse("시뮬레이션 엔트리 생성 실패: " + e.getMessage());
        }
    }
    
    // =============================================
    // 선수 정보 조회 및 매핑
    // =============================================

    /**
     * p_no로 선수 정보 조회 및 스탯 매핑
     */
    private Map<String, Object> getPlayerStatsByPNo(Long pNo) {
        if (pNo == null) {
            return null;
        }

        PlayerEntity player = playerRepo.findBypNo(pNo);
        if (player == null) {
            return null;
        }

        Map<String, Object> playerStats = new HashMap<>();
        playerStats.put("p_no", pNo);
        playerStats.put("player_name", player.getPlayerName());
        playerStats.put("player_type", player.getPlayerType());
        playerStats.put("hand", player.getHand());

        if ("batter".equals(player.getPlayerType())) {
            playerStats.put("batting_stats", mapBattingStats(player));
        }

        if ("pitcher".equals(player.getPlayerType())) {
            playerStats.put("pitching_stats", mapPitchingStats(player));
        }

        return playerStats;
    }
    
    // 타자 통계 매핑
    private Map<String, Object> mapBattingStats(PlayerEntity player) {
        Map<String, Object> battingStats = new HashMap<>();
        
        // 기본 타격 통계
        battingStats.put("b_AVG", player.getBAvg());
        battingStats.put("b_OBP", player.getBObp());
        battingStats.put("b_SLG", player.getBSlg());
        battingStats.put("b_OPS", player.getBOps());
        battingStats.put("b_HR", player.getBHr());
        battingStats.put("b_RBI", player.getBRbi());
        battingStats.put("b_SB", player.getBSb());
        
        // 추가 타격 통계
        battingStats.put("b_2B", player.getB2B());
        battingStats.put("b_3B", player.getB3B());
        battingStats.put("b_HP", player.getBHp());
        battingStats.put("b_GDP", player.getBGdp());
        battingStats.put("b_SF", player.getBSf());
        battingStats.put("b_SO", player.getBSo());
        battingStats.put("b_ePA", player.getBEpa());
        battingStats.put("b_BB", player.getBBb());
        battingStats.put("b_H", player.getBH());
        battingStats.put("b_IB", player.getBIb());
        battingStats.put("b_R", player.getBR());
        
        return battingStats;
    }
    
    // 투수 통계 매핑
    private Map<String, Object> mapPitchingStats(PlayerEntity player) {
        Map<String, Object> pitchingStats = new HashMap<>();
        
        // 기본 투구 통계
        pitchingStats.put("p_ERA", player.getPEra());
        pitchingStats.put("p_FIP", player.getPFip());
        pitchingStats.put("p_WHIP", player.getPWhip());
        pitchingStats.put("p_W", player.getPW());
        pitchingStats.put("p_L", player.getPL());
        pitchingStats.put("p_IP", player.getPIp());
        pitchingStats.put("p_SO", player.getPSo());
        
        // 추가 투구 통계
        pitchingStats.put("p_2B", player.getP2B());
        pitchingStats.put("p_3B", player.getP3B());
        pitchingStats.put("p_HR", player.getPHr());
        pitchingStats.put("p_HP", player.getPHp());
        pitchingStats.put("p_ROE", player.getPRoe());
        pitchingStats.put("p_BB", player.getPBb());
        pitchingStats.put("p_H", player.getPH());
        pitchingStats.put("p_IB", player.getPIb());
        pitchingStats.put("p_R", player.getPR());
        
        return pitchingStats;
    }
    
    
    // =============================================
    // 유틸리티 메서드
    // =============================================

    /**
     * 에러 응답 생성
     */
    private Map<String, Object> createErrorResponse(String errorMessage) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", errorMessage);
        errorResponse.put("home_score", 0);
        errorResponse.put("away_score", 0);
        errorResponse.put("winner", "error");
        return errorResponse;
    }

    /**
     * 시뮬레이션 엔트리 생성
     */
    private SimulationDto createSimulationFromRequest(SimulationRequestDto request, UsersDto loginUser) {
        SimulationDto dto = new SimulationDto();
        dto.setHometeam(Long.valueOf(request.getHomeTeam()));
        dto.setAwayteam(Long.valueOf(request.getAwayTeam()));
        dto.setHomeLineup(convertLineupToString(request.getHomeLineup()));
        dto.setAwayLineup(convertLineupToString(request.getAwayLineup()));
        dto.setUser(getUserByDto(loginUser)); // JWT에서 가져온 사용자
        dto.setShowAt(request.getShowAt());

        return create(dto, loginUser);
    }

    /**
     * 라인업을 JSON 문자열로 변환
     */
    private String convertLineupToString(SimulationRequestDto.LineupDto lineup) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(lineup);
        } catch (Exception e) {
            System.err.println("라인업 변환 실패: " + e.getMessage());
            return "{}";
        }
    }

    /**
     * 사용자 조회 (JWT에서 가져온 UsersDto 기반)
     */
    private UsersDto getUserByDto(UsersDto userDto) {
        try {
            if (userDto == null) {
                System.err.println("사용자 정보가 없습니다.");
                return null;
            }

            // DB에서 실제 사용자 조회
            UsersEntity userEntity = usersRepo.findByAccount(userDto.getAccount())
                .orElse(null);

            if (userEntity == null) {
                System.err.println("사용자를 DB에서 찾을 수 없습니다: " + userDto.getAccount());
                return null;
            }

            // UsersDto로 변환하여 반환
            UsersDto resultDto = new UsersDto();
            resultDto.setId(userEntity.getId());
            resultDto.setAccount(userEntity.getAccount());
            resultDto.setPassword(userEntity.getPassword());
            resultDto.setName(userEntity.getName());
            return resultDto;

        } catch (Exception e) {
            System.err.println("getUserByDto 오류: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Object를 Long으로 안전하게 변환 (null, Integer, Long 모두 처리)
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            System.err.println("⚠️  Long 변환 실패: " + value);
            return null;
        }
    }

    // =============================================
    // 🗄️ 데이터베이스 저장/조회 (타석 기록)
    // =============================================

    /**
     * 파싱된 타석별 데이터를 DB에 저장 (레거시 - 사용되지 않음)
     */
    public void saveAtBatsToDatabase(Long simulationId, List<Map<String, Object>> atBats) {
        try {
            // SimulationEntity 조회 (한 번만)
            SimulationEntity simulation = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("시뮬레이션을 찾을 수 없습니다."));

            for (Map<String, Object> atBatData : atBats) {
                AtBatEntity atBatEntity = AtBatEntity.builder()
                    .simulation(simulation)
                    .inningHalf((String) atBatData.get("inning_half"))
                    .pitcherPNo(Long.valueOf(atBatData.get("pitcher_p_no").toString()))
                    .batterPNo(Long.valueOf(atBatData.get("batter_p_no").toString()))
                    .prevScoreHome((Integer) atBatData.get("prev_score_home"))
                    .prevScoreAway((Integer) atBatData.get("prev_score_away"))
                    .prevOuts((Integer) atBatData.get("prev_outs"))
                    .prevBase1(atBatData.get("prev_base_1") != null ? Long.valueOf(atBatData.get("prev_base_1").toString()) : null)
                    .prevBase2(atBatData.get("prev_base_2") != null ? Long.valueOf(atBatData.get("prev_base_2").toString()) : null)
                    .prevBase3(atBatData.get("prev_base_3") != null ? Long.valueOf(atBatData.get("prev_base_3").toString()) : null)
                    .result((String) atBatData.get("result"))
                    .rbi((Integer) atBatData.get("rbi"))
                    .build();
                
                atBatRepo.save(atBatEntity);
            }
            
            System.out.println("타석별 데이터 저장 완료: " + atBats.size() + "개 타석");
            
        } catch (Exception e) {
            System.err.println("타석별 데이터 저장 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * 특정 시뮬레이션의 타석별 결과 조회
     */
    public List<Map<String, Object>> getSimulationAtBats(Long simulationId) {
        try {
            // ID 순으로 정렬 (타석이 발생한 순서대로)
            List<AtBatEntity> atBats = atBatRepo.findBySimulation_Id(simulationId);
            atBats.sort((a, b) -> a.getId().compareTo(b.getId()));

            return atBats.stream().map(atBat -> {
                Map<String, Object> atBatMap = new HashMap<>();
                atBatMap.put("id", atBat.getId());
                atBatMap.put("simulationId", atBat.getSimulation().getId());
                atBatMap.put("inningHalf", atBat.getInningHalf());
                atBatMap.put("pitcherPNo", atBat.getPitcherPNo());
                atBatMap.put("batterPNo", atBat.getBatterPNo());
                atBatMap.put("batting_order", atBat.getBattingOrder());

                // 선수 정보는 Player 테이블에서 실시간 조회
                Map<String, Object> batterInfo = getPlayerStatsByPNo(atBat.getBatterPNo());
                Map<String, Object> pitcherInfo = getPlayerStatsByPNo(atBat.getPitcherPNo());

                String batterName = batterInfo != null ? (String) batterInfo.getOrDefault("player_name", "알 수 없음") : "알 수 없음";
                String pitcherName = pitcherInfo != null ? (String) pitcherInfo.getOrDefault("player_name", "알 수 없음") : "알 수 없음";

                Double batterAvg = 0.0;
                if (batterInfo != null && batterInfo.containsKey("batting_stats")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> battingStats = (Map<String, Object>) batterInfo.get("batting_stats");
                    batterAvg = (Double) battingStats.getOrDefault("b_AVG", 0.0);
                }

                atBatMap.put("batter_name", batterName);
                atBatMap.put("pitcher_name", pitcherName);
                atBatMap.put("batter_avg", batterAvg);

                // 타석 전 상황
                atBatMap.put("prevScoreHome", atBat.getPrevScoreHome());
                atBatMap.put("prevScoreAway", atBat.getPrevScoreAway());
                atBatMap.put("prevOuts", atBat.getPrevOuts());
                atBatMap.put("prevBase1", atBat.getPrevBase1());
                atBatMap.put("prevBase2", atBat.getPrevBase2());
                atBatMap.put("prevBase3", atBat.getPrevBase3());

                // 타석 결과
                atBatMap.put("result", atBat.getResult());
                atBatMap.put("result_korean", atBat.getResultKorean());
                atBatMap.put("rbi", atBat.getRbi());

                // 타석 후 상황 (⭐ 새로 추가)
                atBatMap.put("newScoreHome", atBat.getNewScoreHome());
                atBatMap.put("newScoreAway", atBat.getNewScoreAway());
                atBatMap.put("newOuts", atBat.getNewOuts());
                atBatMap.put("newBase1", atBat.getNewBase1());
                atBatMap.put("newBase2", atBat.getNewBase2());
                atBatMap.put("newBase3", atBat.getNewBase3());

                atBatMap.put("createdAt", atBat.getCreatedAt());

                // AI 예측 확률 (JSON 문자열)
                atBatMap.put("probabilities", atBat.getProbabilities());

                return atBatMap;
            }).collect(java.util.stream.Collectors.toList());

        } catch (Exception e) {
            System.err.println("타석별 데이터 조회 오류: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // =============================================
    // 🎮 실시간 시뮬레이션 메서드
    // =============================================

    /**
     * 실시간 게임 시작
     */
    public Map<String, Object> startRealtimeGame(Long simulationId) {
        try {
            // 시뮬레이션 정보 조회
            SimulationEntity simulation = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("시뮬레이션을 찾을 수 없습니다."));

            // 이미 게임 상태가 있는지 확인
            if (gameStateRepo.existsBySimulationId(simulationId)) {
                return createErrorResponse("이미 시작된 게임입니다.");
            }

            // 라인업에서 첫 타자/투수 정보 추출
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> homeLineup = objectMapper.readValue(simulation.getHomeLineup(), typeRef);
            Map<String, Object> awayLineup = objectMapper.readValue(simulation.getAwayLineup(), typeRef);

            // 1회초 시작: 어웨이팀 공격, 홈팀 수비
            Long firstBatterPNo = Long.valueOf(awayLineup.get("batting1").toString()); // 어웨이팀 1번타자
            Long firstPitcherPNo = Long.valueOf(homeLineup.get("pitcher").toString()); // 홈팀 투수

            // 초기 게임 상태 생성
            SimulationGameStateEntity gameState = SimulationGameStateEntity.builder()
                .simulation(simulation)
                .inning(1)
                .half("초")
                .outs(0)
                .base1(null)
                .base2(null)
                .base3(null)
                .homeScore(0)
                .awayScore(0)
                .homeBatterIdx(0)
                .awayBatterIdx(0)
                .currentPitcherPNo(firstPitcherPNo)   // 🆕 첫 투수 설정
                .nextBatterPNo(firstBatterPNo)        // 🆕 첫 타자 설정
                .gameStatus("PLAYING")
                .build();

            gameState = gameStateRepo.save(gameState);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "게임이 시작되었습니다.");
            response.put("gameState", convertGameStateToMap(gameState));

            return response;

        } catch (Exception e) {
            System.err.println("게임 시작 오류: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("게임 시작 실패: " + e.getMessage());
        }
    }

    // 시뮬레이션별 동시성 제어용 락 맵
    private final Map<Long, Lock> simulationLocks = new ConcurrentHashMap<>();

    /**
     * 다음 타석 처리 (실시간 타석별 시뮬레이션)
     * 시뮬레이션별 락으로 동시성 제어 (여러 게임 동시 진행 허용)
     */
    public Map<String, Object> processNextAtBat(Long simulationId) {
        Lock lock = simulationLocks.computeIfAbsent(simulationId, id -> new ReentrantLock());
        lock.lock();
        try {
            // 현재 게임 상태 조회
            SimulationGameStateEntity gameState = gameStateRepo.findBySimulationId(simulationId)
                .orElseThrow(() -> new RuntimeException("게임 상태를 찾을 수 없습니다."));

            // 게임이 종료되었는지 확인
            if ("FINISHED".equals(gameState.getGameStatus())) {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "game_ended");
                response.put("message", "게임이 이미 종료되었습니다.");
                response.put("gameState", convertGameStateToMap(gameState));
                SimulationEntity simulation = simulationRepo.findById(simulationId).orElseThrow(() -> new RuntimeException("시뮬레이션을 찾을 수 없습니다."));
                // 종료되었는데 종료 처리 안되었을 경우, simulation 수정
                if (Boolean.FALSE.equals(simulation.getIsFinished())) {
                    simulation.setIsFinished(true);
                    simulationRepo.save(simulation);
                }
                return response;
            }

            // 게임이 진행 중이 아닌 경우 처리하지 않음
            if (!"PLAYING".equals(gameState.getGameStatus())) {
                log.debug("게임이 진행 중이 아닙니다. simulationId={}, status={}", simulationId, gameState.getGameStatus());
                return null;
            }

            // 시뮬레이션 정보 조회
            SimulationEntity simulation = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("시뮬레이션을 찾을 수 없습니다."));

            // 타석 시작 전 상태 저장
            boolean isAwayAttacking = "초".equals(gameState.getHalf());
            int currentBatterIdx = isAwayAttacking ? gameState.getAwayBatterIdx() : gameState.getHomeBatterIdx();
            int currentBatterOrder = (currentBatterIdx % 9) + 1;
            int currentInning = gameState.getInning();
            String currentHalf = gameState.getHalf();
            String currentInningHalf = String.valueOf(currentInning) + (currentHalf != null ? currentHalf : "초");

            // 타자/투수 정보 구성
            Map<String, Object> atBatRequest = buildAtBatRequest(simulation, gameState);

            // Python 서버로 단일 타석 예측 요청
            Map<String, Object> atBatResult = callPythonSingleAtBat(atBatRequest);

            if (atBatResult.containsKey("error")) {
                return atBatResult;
            }

            // 타석 결과 저장 및 게임 상태 업데이트
            Long atBatId = saveAtBatResultFromPython(simulationId, gameState, atBatResult, currentBatterOrder);
            updateGameStateFromPython(gameState, atBatResult, isAwayAttacking, simulation); // simulation 전달

            // 선수 정보 추가
            Long batterPNo = Long.valueOf(atBatResult.get("batter_p_no").toString());
            Long pitcherPNo = Long.valueOf(atBatResult.get("pitcher_p_no").toString());

            Map<String, Object> batterInfo = getPlayerStatsByPNo(batterPNo);
            Map<String, Object> pitcherInfo = getPlayerStatsByPNo(pitcherPNo);

            // 응답에 선수 정보 및 DB ID 추가
            atBatResult.put("id", atBatId);
            atBatResult.put("batter_name", batterInfo.getOrDefault("player_name", "알 수 없음"));
            atBatResult.put("pitcher_name", pitcherInfo.getOrDefault("player_name", "알 수 없음"));
            atBatResult.put("batting_order", currentBatterOrder);
            atBatResult.put("inningHalf", currentInningHalf);
            atBatResult.put("inning", currentInning);
            atBatResult.put("half", currentHalf);
            atBatResult.put("isAwayAttacking", isAwayAttacking);

            if (batterInfo.containsKey("batting_stats")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> battingStats = (Map<String, Object>) batterInfo.get("batting_stats");
                atBatResult.put("batter_avg", battingStats.getOrDefault("b_AVG", 0.0));
            } else {
                atBatResult.put("batter_avg", 0.0);
            }

            // 🔔 게임 종료 알림
            Boolean gameEnded = (Boolean) atBatResult.getOrDefault("game_ended", false);
            if (gameEnded) {
                Long userId = simulation.getUser().getId();
                String winner = (String) atBatResult.get("winner");
                
                @SuppressWarnings("unchecked")
                Map<String, Object> newGameState = (Map<String, Object>) atBatResult.get("new_game_state");
                int homeScore = (Integer) newGameState.get("homeScore");
                int awayScore = (Integer) newGameState.get("awayScore");
                
                notificationService.notifyGameEnded(userId, simulationId, winner, homeScore, awayScore, simulation.getHometeam(), simulation.getAwayteam());
            }

            // 응답 구성
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("atBatResult", atBatResult);
            response.put("gameState", convertGameStateToMap(gameState));

            return response;

        } catch (Exception e) {
            System.err.println("타석 처리 오류: " + e.getMessage());
            e.printStackTrace();
            return createErrorResponse("타석 처리 실패: " + e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 현재 게임 상태 조회
     */
    public Map<String, Object> getCurrentGameState(Long simulationId) {
        try {
            SimulationGameStateEntity gameState = gameStateRepo.findBySimulationId(simulationId)
                .orElseThrow(() -> new RuntimeException("게임 상태를 찾을 수 없습니다."));

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("gameState", convertGameStateToMap(gameState));

            return response;

        } catch (Exception e) {
            System.err.println("게임 상태 조회 오류: " + e.getMessage());
            return createErrorResponse("게임 상태 조회 실패: " + e.getMessage());
        }
    }

    /**
     * 타석 요청 데이터 구성
     */
    private Map<String, Object> buildAtBatRequest(SimulationEntity simulation, SimulationGameStateEntity gameState) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("simulation_id",simulation.getId());
            request.put("away_team", simulation.getAwayteam());
            request.put("home_team", simulation.getHometeam());

            // 게임 상황 정보
            request.put("inning", gameState.getInning());
            request.put("half", gameState.getHalf());
            request.put("outs", gameState.getOuts());
            request.put("base1", gameState.getBase1());
            request.put("base2", gameState.getBase2());
            request.put("base3", gameState.getBase3());
            request.put("homeScore", gameState.getHomeScore());
            request.put("awayScore", gameState.getAwayScore());

            // 라인업 정보 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> homeLineup = objectMapper.readValue(simulation.getHomeLineup(), typeRef);
            Map<String, Object> awayLineup = objectMapper.readValue(simulation.getAwayLineup(), typeRef);

            // 공격팀과 수비팀 결정
            boolean isAwayAttacking = "초".equals(gameState.getHalf());
            Map<String, Object> attackingLineup = isAwayAttacking ? awayLineup : homeLineup;
            Map<String, Object> defendingLineup = isAwayAttacking ? homeLineup : awayLineup;

            // 현재 타자 정보
            int currentBatterIdx = isAwayAttacking ? gameState.getAwayBatterIdx() : gameState.getHomeBatterIdx();
            int batterIndex = currentBatterIdx % 9 + 1;

            // 라인업에서 선수 번호 추출
            Object batterObj = attackingLineup.get("batting" + batterIndex);
            Object pitcherObj = defendingLineup.get("pitcher");

            if (batterObj == null || pitcherObj == null) {
                throw new RuntimeException("라인업에서 선수 정보를 찾을 수 없습니다.");
            }

            Long batterPNo = Long.valueOf(batterObj.toString());
            Long pitcherPNo = Long.valueOf(pitcherObj.toString());

            // 선수 스탯 정보 추가
            request.put("batter", getPlayerStatsByPNo(batterPNo));
            request.put("pitcher", getPlayerStatsByPNo(pitcherPNo));

            return request;

        } catch (Exception e) {
            System.err.println("타석 요청 데이터 구성 오류: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    /**
     * Python 서버로 단일 타석 예측 요청
     */
    private Map<String, Object> callPythonSingleAtBat(Map<String, Object> request) {
        try {
            String pythonServerUrl = fastapiServerUrl + "/simulate-at-bat";
            
            log.info("Python 서버로 타석 요청 전송: {}", request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(request, headers);

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(
                pythonServerUrl,
                requestEntity,
                Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) response.getBody();
            
            log.info("Python 서버 응답 수신: {}", body);
            return body;

        } catch (Exception e) {
            log.error("Python 완전한 시뮬레이션 호출 실패: {}", e.getMessage());
            e.printStackTrace();
            return createErrorResponse("Python 서버 호출 실패: " + e.getMessage());
        }
    }

    /**
     * Python에서 계산된 게임 상태를 DB에 반영
     * @param wasAwayAttacking 타석 시작 시점에 어웨이팀이 공격했는지 여부
     * @param simulation 시뮬레이션 정보 (라인업 조회용)
     */
    private void updateGameStateFromPython(SimulationGameStateEntity gameState, Map<String, Object> pythonResult, boolean wasAwayAttacking, SimulationEntity simulation) {
        try {
            Object newGameStateObj = pythonResult.get("new_game_state");
            if (!(newGameStateObj instanceof Map)) {
                System.err.println("❌ new_game_state가 Map 타입이 아닙니다.");
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> newGameState = (Map<String, Object>) newGameStateObj;

            if (newGameState != null) {
                gameState.setInning((Integer) newGameState.get("inning"));
                gameState.setHalf((String) newGameState.get("half"));
                gameState.setOuts((Integer) newGameState.get("outs"));
                gameState.setHomeScore((Integer) newGameState.get("homeScore"));
                gameState.setAwayScore((Integer) newGameState.get("awayScore"));

                // 베이스 정보 안전하게 변환 (Python에서 Integer로 올 수 있음)
                gameState.setBase1(convertToLong(newGameState.get("base1")));
                gameState.setBase2(convertToLong(newGameState.get("base2")));
                gameState.setBase3(convertToLong(newGameState.get("base3")));

                // 타자 순번 증가
                if (wasAwayAttacking) {
                    int nextIdx = (gameState.getAwayBatterIdx() + 1) % 9;
                    gameState.setAwayBatterIdx(nextIdx);
                } else {
                    int nextIdx = (gameState.getHomeBatterIdx() + 1) % 9;
                    gameState.setHomeBatterIdx(nextIdx);
                }

                // 게임 종료 처리
                Boolean gameEnded = (Boolean) pythonResult.getOrDefault("game_ended", false);
                if (gameEnded) {
                    gameState.setGameStatus("FINISHED");
                    gameState.setWinner((String) pythonResult.get("winner"));
                    // 게임 종료 시 다음 타자/투수 정보 초기화
                    gameState.setNextBatterPNo(null);
                    gameState.setCurrentPitcherPNo(null);
                    
                    // 종료되었는데 종료 처리 안되었을 경우, simulation 수정
                    if (Boolean.FALSE.equals(simulation.getIsFinished())) {
                        simulation.setIsFinished(true);
                        simulationRepo.save(simulation);
                    }
                } else {
                    // 🆕 다음 타자/투수 정보 계산 (게임이 진행 중일 때만)
                    calculateAndSetNextBatterPitcher(gameState, simulation);
                }

                gameState.setUpdatedAt(LocalDateTime.now());
                gameStateRepo.save(gameState);
            }

        } catch (Exception e) {
            System.err.println("❌ Python 결과로 게임 상태 업데이트 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🆕 다음 타자/투수 정보를 계산하여 gameState에 설정
     */
    private void calculateAndSetNextBatterPitcher(SimulationGameStateEntity gameState, SimulationEntity simulation) {
        try {
            // 라인업 정보 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
            Map<String, Object> homeLineup = objectMapper.readValue(simulation.getHomeLineup(), typeRef);
            Map<String, Object> awayLineup = objectMapper.readValue(simulation.getAwayLineup(), typeRef);

            // 현재 공격팀/수비팀 결정
            boolean isAwayAttacking = "초".equals(gameState.getHalf());
            Map<String, Object> attackingLineup = isAwayAttacking ? awayLineup : homeLineup;
            Map<String, Object> defendingLineup = isAwayAttacking ? homeLineup : awayLineup;

            // 다음 타자 계산
            int nextBatterIdx = isAwayAttacking ? gameState.getAwayBatterIdx() : gameState.getHomeBatterIdx();
            int nextBatterOrder = (nextBatterIdx % 9) + 1;
            Object nextBatterObj = attackingLineup.get("batting" + nextBatterOrder);
            Long nextBatterPNo = nextBatterObj != null ? Long.valueOf(nextBatterObj.toString()) : null;

            // 현재 투수 (교체가 없다면 동일)
            Object pitcherObj = defendingLineup.get("pitcher");
            Long currentPitcherPNo = pitcherObj != null ? Long.valueOf(pitcherObj.toString()) : null;

            // gameState에 설정
            gameState.setNextBatterPNo(nextBatterPNo);
            gameState.setCurrentPitcherPNo(currentPitcherPNo);

        } catch (Exception e) {
            System.err.println("❌ 다음 타자/투수 정보 계산 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }



    /**
     * Python에서 계산된 타석 결과를 DB에 저장
     */
    private Long saveAtBatResultFromPython(Long simulationId, SimulationGameStateEntity gameState, Map<String, Object> pythonResult, int battingOrder) {
        try {
            // SimulationEntity 조회
            SimulationEntity simulation = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("시뮬레이션을 찾을 수 없습니다."));

            // 선수 정보 가져오기
            Long batterPNo = Long.valueOf(pythonResult.get("batter_p_no").toString());
            Long pitcherPNo = Long.valueOf(pythonResult.get("pitcher_p_no").toString());

            // Map<String, Object> batterInfo = getPlayerStatsByPNo(batterPNo);
            //Map<String, Object> pitcherInfo = getPlayerStatsByPNo(pitcherPNo);

            // 이닝 정보 구성
            String inningHalf = String.valueOf(gameState.getInning()) +
                               (gameState.getHalf() != null ? gameState.getHalf() : "초");

            // Python에서 반환한 타석 후 상태 추출
            @SuppressWarnings("unchecked")
            Map<String, Object> newGameState = (Map<String, Object>) pythonResult.get("new_game_state");

            // probabilities를 JSON 문자열로 변환
            String probabilitiesJson = null;
            if (pythonResult.containsKey("probabilities")) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    probabilitiesJson = objectMapper.writeValueAsString(pythonResult.get("probabilities"));
                } catch (Exception e) {
                    System.err.println("⚠️ probabilities JSON 변환 실패: " + e.getMessage());
                }
            }

            AtBatEntity atBat = AtBatEntity.builder()
                .simulation(simulation)
                .inningHalf(inningHalf)
                .pitcherPNo(pitcherPNo)
                .batterPNo(batterPNo)
                .battingOrder(battingOrder)
                .prevScoreHome(gameState.getHomeScore())
                .prevScoreAway(gameState.getAwayScore())
                .prevOuts(gameState.getOuts())
                .prevBase1(gameState.getBase1())
                .prevBase2(gameState.getBase2())
                .prevBase3(gameState.getBase3())
                .result((String) pythonResult.get("result"))
                .resultKorean((String) pythonResult.get("result_korean"))
                .rbi((Integer) pythonResult.getOrDefault("rbi", 0))
                .newScoreHome((Integer) newGameState.get("homeScore"))
                .newScoreAway((Integer) newGameState.get("awayScore"))
                .newOuts((Integer) newGameState.get("outs"))
                .newBase1(convertToLong(newGameState.get("base1")))
                .newBase2(convertToLong(newGameState.get("base2")))
                .newBase3(convertToLong(newGameState.get("base3")))
                .probabilities(probabilitiesJson)
                .build();

            AtBatEntity savedAtBat = atBatRepo.save(atBat);
            return savedAtBat.getId();

        } catch (Exception e) {
            System.err.println("❌ Python 타석 기록 저장 오류: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 게임 상태를 Map으로 변환
     */
    private Map<String, Object> convertGameStateToMap(SimulationGameStateEntity gameState) {
        Map<String, Object> map = new HashMap<>();
        map.put("inning", gameState.getInning());
        map.put("half", gameState.getHalf());
        map.put("outs", gameState.getOuts());
        map.put("base1", gameState.getBase1());
        map.put("base2", gameState.getBase2());
        map.put("base3", gameState.getBase3());
        map.put("homeScore", gameState.getHomeScore());
        map.put("awayScore", gameState.getAwayScore());
        map.put("homeBatterIdx", gameState.getHomeBatterIdx());
        map.put("awayBatterIdx", gameState.getAwayBatterIdx());
        map.put("gameStatus", gameState.getGameStatus());
        map.put("winner", gameState.getWinner());

        // 🆕 다음 타자/현재 투수 정보 추가
        map.put("nextBatterPNo", gameState.getNextBatterPNo());
        map.put("currentPitcherPNo", gameState.getCurrentPitcherPNo());

        // 🆕 선수 이름 정보 추가
        if (gameState.getNextBatterPNo() != null) {
            Map<String, Object> batterInfo = getPlayerStatsByPNo(gameState.getNextBatterPNo());
            if (batterInfo != null) {
                map.put("nextBatterName", batterInfo.get("player_name"));
                // 타자 타율도 추가
                if (batterInfo.containsKey("batting_stats")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> battingStats = (Map<String, Object>) batterInfo.get("batting_stats");
                    map.put("nextBatterAvg", battingStats.getOrDefault("b_AVG", 0.0));
                }
            }
        }

        if (gameState.getCurrentPitcherPNo() != null) {
            Map<String, Object> pitcherInfo = getPlayerStatsByPNo(gameState.getCurrentPitcherPNo());
            if (pitcherInfo != null) {
                map.put("currentPitcherName", pitcherInfo.get("player_name"));
                // 투수 ERA도 추가
                if (pitcherInfo.containsKey("pitching_stats")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pitchingStats = (Map<String, Object>) pitcherInfo.get("pitching_stats");
                    map.put("currentPitcherERA", pitchingStats.getOrDefault("p_ERA", 0.0));
                }
            }
        }

        return map;
    }

}
