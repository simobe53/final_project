package com.ict.springboot.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ict.springboot.dto.SimulationDto;
import com.ict.springboot.dto.UserSimulRequestDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.UserSimulRequestEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.UserSimulRequestRepository;
import com.ict.springboot.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserSimulRequestService {

    private final UserSimulRequestRepository userSimulRequestRepo;
    private final UsersRepository usersRepo;
    private final SimulationService simulationService;
    private final NotificationService notificationService;
    private final SimulationSseService simulationSseService;

    // 시뮬레이션 요청 생성 (일반 사용자)
    @Transactional
    public UserSimulRequestDto create(UserSimulRequestDto dto, UsersDto loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        UsersEntity userEntity = usersRepo.findByAccount(loginUser.getAccount())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 일반 사용자만 요청 생성 가능
        if (!"USER".equals(userEntity.getRole())) {
            throw new RuntimeException("일반 사용자만 시뮬레이션 요청을 생성할 수 있습니다.");
        }

        UserSimulRequestEntity entity = UserSimulRequestEntity.builder()
                .user(userEntity)
                .hometeam(dto.getHometeam())
                .awayteam(dto.getAwayteam())
                .homeLineup(dto.getHomeLineup())
                .awayLineup(dto.getAwayLineup())
                .stadium(dto.getStadium())
                .status("PENDING")
                .requestAt(LocalDateTime.now())
                .build();

        entity = userSimulRequestRepo.save(entity);
        
        UserSimulRequestDto result = UserSimulRequestDto.toDto(entity);
        
        // 🔔 관리자에게 신규 요청 알림
        notificationService.notifyNewSimulationRequest(entity.getId(), loginUser.getName(), entity.getRequestAt());
        
        // 📡 SSE로 모든 사용자에게 새 요청 이벤트 발송
        simulationSseService.sendNewRequestEvent(result);
        
        return result;
    }

    // 모든 요청 조회 (관리자: 모든 요청, 일반 사용자: 본인 요청만)
    public List<UserSimulRequestDto> getAll(UsersDto loginUser, Map<String, String> params) {
        if (loginUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        UsersEntity userEntity = usersRepo.findByAccount(loginUser.getAccount())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        List<UserSimulRequestEntity> entities;

        if ("ADMIN".equals(userEntity.getRole())) {
            // 관리자는 모든 요청 조회
            if (params.containsKey("status")) {
                entities = userSimulRequestRepo.findByStatusOrderByRequestAtDesc(params.get("status"));
            } else {
                entities = userSimulRequestRepo.findAllByOrderByRequestAtDesc();
            }
        } else {
            // 일반 사용자는 본인 요청만 조회
            entities = userSimulRequestRepo.findByUserIdOrderByRequestAtDesc(userEntity.getId());
        }

        return entities.stream().map(UserSimulRequestDto::toDto).collect(Collectors.toList());
    }

    // 특정 요청 상세 조회
    public UserSimulRequestDto getById(Long id, UsersDto loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        UsersEntity userEntity = usersRepo.findByAccount(loginUser.getAccount())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        Optional<UserSimulRequestEntity> entityOpt = userSimulRequestRepo.findById(id);
        if (entityOpt.isEmpty()) {
            return null;
        }

        UserSimulRequestEntity entity = entityOpt.get();

        // 권한 체크: 관리자이거나 본인 요청인 경우만 조회 가능
        if (!"ADMIN".equals(userEntity.getRole()) &&
                !Objects.equals(entity.getUser().getId(), userEntity.getId())) {
            throw new RuntimeException("접근 권한이 없습니다.");
        }

        return UserSimulRequestDto.toDto(entity);
    }

    // 요청 승인 (관리자) - 핵심 로직 재사용!
    @Transactional
    public SimulationDto approveRequest(Long id, String adminComment, LocalDateTime scheduledAt, UsersDto loginUser) {
        validateAdminPermission(loginUser);

        UserSimulRequestEntity entity = userSimulRequestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다."));

        if (!"PENDING".equals(entity.getStatus())) {
            throw new RuntimeException("대기 중인 요청만 승인할 수 있습니다.");
        }

        

        try {
            // 🔑 핵심: UserSimulRequestEntity -> SimulationDto 변환
            SimulationDto simulation = SimulationDto.builder()
            .hometeam(entity.getHometeam())
            .awayteam(entity.getAwayteam())
            .homeLineup(entity.getHomeLineup())
            .awayLineup(entity.getAwayLineup())
            .createdAt(entity.getRequestAt())
            .showAt(scheduledAt)
            .user(UsersDto.toDto(entity.getUser()))
            .build();

            // simulation 테이블에 저장 🔑 핵심: 기존 로직 그대로 재사용!
            SimulationDto simulationResult = simulationService.create(simulation, loginUser);
            
            // 🔔 사용자에게 승인 알림 (팀 정보 포함)
            notificationService.notifyRequestApproved(
                entity.getUser().getId(), 
                simulationResult.getId(), 
                scheduledAt,
                entity.getHometeam(),
                entity.getAwayteam()
            );

            // 📡 SSE로 요청 상태 변경 이벤트 발송 (승인)
            simulationSseService.sendRequestStatusChangeEvent(
                entity.getId(), 
                "APPROVED", 
                adminComment, 
                entity.getUser().getId()
            );
            
            // 📡 SSE로 새 시뮬레이션 생성 이벤트 발송
            simulationSseService.sendSimulationApprovedEvent(simulationResult);

            // simulation 테이블에 저장 후 기존 요청 테이블에선 삭제
            delete(id);

            return simulationResult;

        } catch (Exception e) {
            throw new RuntimeException("시뮬레이션 실행 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 요청 거절 (관리자)
    @Transactional
    public UserSimulRequestDto rejectRequest(Long id, String adminComment, UsersDto loginUser) {
        validateAdminPermission(loginUser);

        UserSimulRequestEntity entity = userSimulRequestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다."));

        if (!"PENDING".equals(entity.getStatus())) {
            throw new RuntimeException("대기 중인 요청만 거절할 수 있습니다.");
        }

        entity.setStatus("REFUSE");
        entity.setAdminComment(adminComment);
        entity.setUpdateAt(LocalDateTime.now());
        entity = userSimulRequestRepo.save(entity);
        
        // 🔔 사용자에게 거절 알림 (팀 정보 포함)
        notificationService.notifyRequestRejected(
            entity.getUser().getId(), 
            adminComment,
            entity.getHometeam(),
            entity.getAwayteam()
        );
        
        // 📡 SSE로 요청 상태 변경 이벤트 발송 (거절)
        simulationSseService.sendRequestStatusChangeEvent(
            entity.getId(), 
            "REFUSE", 
            adminComment, 
            entity.getUser().getId()
        );

        return UserSimulRequestDto.toDto(entity);
    }

    // 대기 중인 요청 개수 조회
    public Long countPendingRequests(UsersDto loginUser) {
        validateAdminPermission(loginUser);
        return userSimulRequestRepo.countByStatusPending();
    }

    // 관리자 권한 검증
    private void validateAdminPermission(UsersDto loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("로그인이 필요합니다.");
        }

        UsersEntity userEntity = usersRepo.findByAccount(loginUser.getAccount())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (!"ADMIN".equals(userEntity.getRole())) {
            throw new RuntimeException("관리자만 이 작업을 수행할 수 있습니다.");
        }
    }

    // 삭제
    public UserSimulRequestDto delete(Long id) throws Exception {
        UserSimulRequestEntity simulRequest = userSimulRequestRepo.findById(id).orElseGet(() -> null);
        if (simulRequest != null) {
            try {
                userSimulRequestRepo.deleteById(id);
                return UserSimulRequestDto.toDto(simulRequest);
            } catch (Exception e) {
                throw new Exception("데이터 삭제에 문제가 생겼습니다.");
            }
        }
        return null;
    }
}
