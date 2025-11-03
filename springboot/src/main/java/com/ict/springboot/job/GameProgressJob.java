package com.ict.springboot.job;

import com.ict.springboot.service.QuartzSimulationScheduler;
import com.ict.springboot.service.SimulationService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 게임 진행 Job
 * 10초마다 게임 상태를 업데이트하고 다음 이닝으로 진행
 */
@Component
@Slf4j
@DisallowConcurrentExecution
public class GameProgressJob implements Job {
    
    @Autowired
    private SimulationService simulationService;
    
    @Autowired
    private QuartzSimulationScheduler quartzScheduler;
    
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            Long simulationId = context.getJobDetail().getJobDataMap().getLong("simulationId");
            log.debug("게임 진행 Job 실행: simulationId={}", simulationId);
            
            // 게임 진행 처리
            Map<String, Object> atBatResult = simulationService.processNextAtBat(simulationId);
            
            // null 응답인 경우 (게임이 진행 중이 아닌 경우) 로그만 출력하고 종료
            if (atBatResult == null) {
                log.debug("게임 진행 처리 건너뜀: simulationId={}", simulationId);
                return;
            }
            
            if ("game_ended".equals(atBatResult.get("status"))) {
                String winner = (String) atBatResult.get("winner");
                // 게임 끝 알림
                log.info("게임 완료 알림: simulationId={}, winner={}", simulationId, winner);
                
                // 게임 완료 시 Job 중지
                quartzScheduler.stopGameProgress(simulationId);
                log.info("게임 완료로 인한 Job 중지: simulationId={}, winner={}", simulationId, winner);
            }
            
        } catch (Exception e) {
            log.error("게임 진행 Job 오류", e);
            throw new JobExecutionException(e);
        }
    }
}
