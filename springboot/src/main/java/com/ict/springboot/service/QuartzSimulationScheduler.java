package com.ict.springboot.service;

import com.ict.springboot.entity.SimulationEntity;
import com.ict.springboot.entity.SimulationGameStateEntity;
import com.ict.springboot.job.*;
import com.ict.springboot.repository.SimulationRepository;
import com.ict.springboot.repository.SimulationGameStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.annotation.PostConstruct;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Quartz 기반 시뮬레이션 스케줄러
 * @Scheduled를 대체하여 더 정확하고 유연한 스케줄링 제공
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuartzSimulationScheduler {
    
    private final SchedulerFactoryBean schedulerFactoryBean;
    private final SimulationRepository simulationRepo;
    private final SimulationGameStateRepository gameStateRepo;
    
    /**
     * 서버 시작 시 오늘 날짜의 시뮬레이션들을 스케줄링
     */
    @PostConstruct
    @Transactional
    public void initializeScheduledSimulations() {
        try {
            // 진행 중인 게임들 복구
            recoverActiveGames();
            
            // 오늘 날짜의 아직 실행되지 않은 시뮬레이션들만 스케줄링
            scheduleTodayRemainingSimulations();
            
        } catch (Exception e) {
            log.error("시뮬레이션 스케줄링 초기화 중 오류", e);
        }
    }
    
    /**
     * 오늘 날짜의 아직 실행되지 않은 시뮬레이션들만 스케줄링
     */
    private void scheduleTodayRemainingSimulations() {
        try {
            // 오늘 날짜 범위 계산
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            LocalDateTime now = LocalDateTime.now();
            
            // 오늘 날짜의 시뮬레이션 조회
            List<SimulationEntity> todaySimulations = 
                simulationRepo.findByShowAtBetween(startOfDay, endOfDay);
            
            // 아직 실행되지 않은 시뮬레이션들만 필터링
            List<SimulationEntity> remainingSimulations = todaySimulations.stream()
                .filter(simulation -> simulation.getShowAt().isAfter(now))
                .collect(Collectors.toList());
            
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            
            for (SimulationEntity simulation : remainingSimulations) {
                // 이미 스케줄링된 Job이 있는지 확인
                String jobName = "simulation_" + simulation.getId();
                String groupName = "simulations";
                JobKey jobKey = JobKey.jobKey(jobName, groupName);
                
                if (!scheduler.checkExists(jobKey)) {
                    scheduleSimulationForInitialization(simulation);
                }
            }
            
        } catch (Exception e) {
            log.error("오늘 남은 시뮬레이션 스케줄링 중 오류", e);
        }
    }
    
    /**
     * 서버 시작 시 초기화용 시뮬레이션 스케줄링 (중복 체크 없음)
     */
    private void scheduleSimulationForInitialization(SimulationEntity simulation) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            Long simulationId = simulation.getId();
            LocalDateTime showAt = simulation.getShowAt();
            
            // Job 이름 생성
            String jobName = "simulation_" + simulationId;
            String groupName = "simulations";
            
            // 1. 시뮬레이션 실행 Job 스케줄링
            scheduleSimulationExecution(scheduler, simulationId, showAt, jobName, groupName);
            
            // 2. 알림 Job들 스케줄링
            scheduleNotificationJobs(scheduler, simulationId, showAt, jobName, groupName);
            
            
        } catch (Exception e) {
            log.error("초기화 시뮬레이션 스케줄링 중 오류: simulationId={}", simulation.getId(), e);
        }
    }
    
    /**
     * 개별 시뮬레이션 스케줄링
     */
    public void scheduleSimulation(SimulationEntity simulation) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            Long simulationId = simulation.getId();
            LocalDateTime showAt = simulation.getShowAt();
            
            // Job 이름 생성
            String jobName = "simulation_" + simulationId;
            String groupName = "simulations";
            
            // 중복 Job 등록 방지
            JobKey jobKey = JobKey.jobKey(jobName, groupName);
            if (scheduler.checkExists(jobKey)) {
                return;
            }
            
            // 1. 시뮬레이션 실행 Job 스케줄링
            scheduleSimulationExecution(scheduler, simulationId, showAt, jobName, groupName);
            
            // 2. 알림 Job들 스케줄링
            scheduleNotificationJobs(scheduler, simulationId, showAt, jobName, groupName);
            
        } catch (Exception e) {
            log.error("시뮬레이션 스케줄링 중 오류: simulationId={}", simulation.getId(), e);
        }
    }
    
    /**
     * 시뮬레이션 실행 Job 스케줄링
     */
    private void scheduleSimulationExecution(Scheduler scheduler, Long simulationId, 
                                          LocalDateTime showAt, String jobName, String groupName) throws Exception {
        
        // Job 생성
        JobDetail job = JobBuilder.newJob(SimulationExecutionJob.class)
                .withIdentity(jobName, groupName)
                .usingJobData("simulationId", simulationId)
                .build();
        
        // Trigger 생성 (정확한 시간에 실행)
        Date triggerTime = Date.from(showAt.atZone(ZoneId.systemDefault()).toInstant());
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "_trigger", groupName)
                .startAt(triggerTime)
                .build();
        
        // Job 등록
        scheduler.scheduleJob(job, trigger);
    }
    
    /**
     * 알림 Job들 스케줄링
     */
    private void scheduleNotificationJobs(Scheduler scheduler, Long simulationId, 
                                        LocalDateTime showAt, String jobName, String groupName) throws Exception {
        
        // 10분 전 알림
        LocalDateTime reminder10Time = showAt.minusMinutes(10);
        if (reminder10Time.isAfter(LocalDateTime.now())) {
            String reminder10JobName = jobName + "_reminder10";
            JobKey reminder10JobKey = JobKey.jobKey(reminder10JobName, groupName);
            if (!scheduler.checkExists(reminder10JobKey)) {
                scheduleNotificationJob(scheduler, simulationId, reminder10Time, 
                        reminder10JobName, groupName, NotificationReminder10Job.class);
            }
        }
        
        // 5분 전 알림
        LocalDateTime reminder5Time = showAt.minusMinutes(5);
        if (reminder5Time.isAfter(LocalDateTime.now())) {
            String reminder5JobName = jobName + "_reminder5";
            JobKey reminder5JobKey = JobKey.jobKey(reminder5JobName, groupName);
            if (!scheduler.checkExists(reminder5JobKey)) {
                scheduleNotificationJob(scheduler, simulationId, reminder5Time, 
                        reminder5JobName, groupName, NotificationReminder5Job.class);
            }
        }
    }
    
    /**
     * 개별 알림 Job 스케줄링
     */
    private void scheduleNotificationJob(Scheduler scheduler, Long simulationId, 
                                      LocalDateTime triggerTime, String jobName, 
                                      String groupName, Class<? extends Job> jobClass) throws Exception {
        
        JobDetail job = JobBuilder.newJob(jobClass)
                .withIdentity(jobName, groupName)
                .usingJobData("simulationId", simulationId)
                .build();
        
        Date date = Date.from(triggerTime.atZone(ZoneId.systemDefault()).toInstant());
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "_trigger", groupName)
                .startAt(date)
                .build();
        
        scheduler.scheduleJob(job, trigger);
    }
    
    /**
     * 게임 진행 Job 스케줄링 (10초마다 반복)
     */
    public void scheduleGameProgress(Long simulationId) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            
            String jobName = "game_progress_" + simulationId;
            String groupName = "game_progress";
            JobKey jobKey = JobKey.jobKey(jobName, groupName);
            TriggerKey triggerKey = TriggerKey.triggerKey(jobName + "_trigger", groupName);

            // 중복 방지: 이미 등록되어 있으면 스킵
            if (scheduler.checkExists(jobKey) || scheduler.checkExists(triggerKey)) {
                return;
            }

            // Job 생성
            JobDetail job = JobBuilder.newJob(GameProgressJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("simulationId", simulationId)
                    .build();
            
            // Trigger 생성 (10초마다 반복, 미스파이어 전략 포함)
            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .startNow()
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInSeconds(8)
                            .repeatForever()
                            .withMisfireHandlingInstructionNextWithExistingCount())
                    .build();
            
            // Job 등록
            scheduler.scheduleJob(job, trigger);
            
        } catch (Exception e) {
            log.error("게임 진행 Job 스케줄링 중 오류: simulationId={}", simulationId, e);
        }
    }
    
    /**
     * 게임 진행 Job 중지
     */
    public void stopGameProgress(Long simulationId) {
        try {
            Scheduler scheduler = schedulerFactoryBean.getScheduler();
            
            String jobName = "game_progress_" + simulationId;
            String groupName = "game_progress";
            
            JobKey jobKey = JobKey.jobKey(jobName, groupName);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }
            
        } catch (Exception e) {
            log.error("게임 진행 Job 중지 중 오류: simulationId={}", simulationId, e);
        }
    }
    
    /**
     * 서버 재시작 시 진행 중인 게임들 복구 (오늘 날짜만)
     */
    private void recoverActiveGames() {
        try {
            // 오늘 날짜 범위 계산
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            // 오늘 날짜의 진행 중인 게임들만 조회
            List<SimulationGameStateEntity> activeGames = 
                gameStateRepo.findByGameStatusAndSimulationShowAtBetween("PLAYING", startOfDay, endOfDay);
            
            for (SimulationGameStateEntity gameState : activeGames) {
                Long simulationId = gameState.getSimulation().getId();
                // 이미 존재하면 스킵, 없으면 등록
                Scheduler scheduler = schedulerFactoryBean.getScheduler();
                String jobName = "game_progress_" + simulationId;
                String groupName = "game_progress";
                JobKey jobKey = JobKey.jobKey(jobName, groupName);
                if (!scheduler.checkExists(jobKey)) {
                    scheduleGameProgress(simulationId);
                }
            }
            
        } catch (Exception e) {
            log.error("진행 중인 게임 복구 중 오류", e);
        }
    }
    
    
}
