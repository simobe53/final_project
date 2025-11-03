package com.ict.springboot.job;

import com.ict.springboot.entity.SimulationEntity;
import com.ict.springboot.repository.SimulationRepository;
import com.ict.springboot.repository.SimulationGameStateRepository;
import com.ict.springboot.service.NotificationService;
import com.ict.springboot.service.QuartzSimulationScheduler;
import com.ict.springboot.service.SimulationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 시뮬레이션 실행 Job
 * 예약된 시뮬레이션을 실행하고 게임 진행 Job을 스케줄링
 */
@Component
@Slf4j
public class SimulationExecutionJob implements Job {
    
    @Autowired
    private SimulationService simulationService;
    
    @Autowired
    private QuartzSimulationScheduler quartzScheduler;
    
    @Autowired
    private SimulationRepository simulationRepo;
    
    @Autowired
    private SimulationGameStateRepository gameStateRepo;
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Long simulationId = context.getJobDetail().getJobDataMap().getLong("simulationId");
            log.info("시뮬레이션 실행 Job 시작: simulationId={}", simulationId);
            
            // 시뮬레이션 정보 조회
            SimulationEntity simulation = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("시뮬레이션을 찾을 수 없습니다: " + simulationId));
            
            // 이미 게임이 시작되었는지 확인 (중복 실행 방지)
            if (gameStateRepo.findBySimulationId(simulationId).isPresent()) {
                log.warn("시뮬레이션이 이미 시작되었습니다. 중복 실행 방지: simulationId={}", simulationId);
                return;
            }
            
            // 시뮬레이션 시간 확인 (아직 실행 시간이 아닌 경우 중복 실행 방지)
            if (simulation.getShowAt().isAfter(LocalDateTime.now())) {
                log.warn("시뮬레이션 실행 시간이 아직 되지 않았습니다. 중복 실행 방지: simulationId={}, showAt={}", 
                        simulationId, simulation.getShowAt());
                return;
            }
            
            // 시뮬레이션 실행
            simulationService.startRealtimeGame(simulationId);
            
            // 게임 시작 알림 발송
            notificationService.notifySimulationStarted(
                simulation.getUser().getId(), 
                simulationId, 
                simulation.getHometeam(), 
                simulation.getAwayteam()
            );
            
            // 게임 진행 Job 스케줄링 (10초마다)
            quartzScheduler.scheduleGameProgress(simulationId);
            
            log.info("시뮬레이션 실행 Job 완료: simulationId={}", simulationId);
            
        } catch (Exception e) {
            log.error("시뮬레이션 실행 Job 오류", e);
            throw new JobExecutionException(e);
        }
    }
}
