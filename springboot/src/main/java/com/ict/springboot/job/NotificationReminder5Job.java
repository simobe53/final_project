package com.ict.springboot.job;

import com.ict.springboot.entity.SimulationEntity;
import com.ict.springboot.repository.SimulationRepository;
import com.ict.springboot.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 5분 전 알림 Job
 */
@Component
@Slf4j
public class NotificationReminder5Job implements Job {
    
    @Autowired
    private SimulationRepository simulationRepo;
    
    @Autowired
    private NotificationService notificationService;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Long simulationId = context.getJobDetail().getJobDataMap().getLong("simulationId");
            log.info("5분 전 알림 Job 실행: simulationId={}", simulationId);
            
            // 시뮬레이션 정보 조회
            SimulationEntity simulation = simulationRepo.findById(simulationId)
                .orElseThrow(() -> new RuntimeException("시뮬레이션을 찾을 수 없습니다: " + simulationId));
            
            // 5분 전 알림 발송
            notificationService.notifySimulationReminder5(
                simulation.getUser().getId(), 
                simulationId, 
                simulation.getHometeam(), 
                simulation.getAwayteam()
            );
            
            log.info("5분 전 알림 발송 완료: simulationId={}", simulationId);
            
        } catch (Exception e) {
            log.error("5분 전 알림 Job 오류", e);
            throw new JobExecutionException(e);
        }
    }
}
