package com.ict.springboot.service;

import com.ict.springboot.entity.PlayerEntity;
import com.ict.springboot.entity.TeamEntity;
import com.ict.springboot.repository.PlayerRepository;
import com.ict.springboot.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class PlayerDataLoaderService {
    
    @Autowired
    private PlayerRepository playerRepository;
    
    @Autowired
    private TeamRepository teamRepository;
    
    /**
     * 애플리케이션 시작 시 자동으로 선수 데이터 로딩
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadPlayersOnStartup() {
        // 이미 데이터가 있으면 실행하지 않음
        if (playerRepository.count() > 0) {
            System.out.println("선수 데이터가 이미 존재합니다. 로딩을 건너뜁니다.");
            return;
        }
        
         System.out.println("애플리케이션 시작 시 선수 데이터 로딩을 시작합니다...");
        try {
            // Docker 환경에서는 /app/sql/players_all_2025.tsv
            // 로컬 환경에서는 ../sql/players_all_2025.tsv
            String filePath = "sql/players_all_2025.tsv"; // Docker 경로
            File file = new File(filePath);
            if (!file.exists()) {
                filePath = "../sql/players_all_2025.tsv"; // 로컬 경로
            }
            loadPlayersFromTsv(filePath);
            System.out.println("선수 데이터 로딩이 완료되었습니다!");
        } catch (Exception e) {
            System.err.println("선수 데이터 로딩 실패: " + e.getMessage());
        }
    }
    
    @Transactional
    public void loadPlayersFromTsv(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            boolean isFirstLine = true;
            int successCount = 0;
            int errorCount = 0;
            
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // 헤더 스킵
                    continue;
                }
                
                try {
                    String[] columns = line.split("\t");
                    if (columns.length >= 15) {
                        PlayerEntity player = createPlayerFromTsvRow(columns);
                        if (player != null) {
                            playerRepository.save(player);
                            successCount++;
                        } else {
                            errorCount++; // 팀이 없어서 스킵된 경우
                        }
                    } else {
                        System.out.println("컬럼 수 부족: " + line);
                        errorCount++;
                    }
                } catch (Exception e) {
                    System.out.println("데이터 처리 오류: " + line + " - " + e.getMessage());
                    errorCount++;
                }
            }
            
            System.out.println("데이터 로딩 완료 - 성공: " + successCount + ", 실패: " + errorCount);
            
        } catch (IOException e) {
            System.err.println("파일 읽기 오류: " + e.getMessage());
            throw new RuntimeException("파일 읽기 실패", e);
        }
    }
    
           private PlayerEntity createPlayerFromTsvRow(String[] columns) {
               // TSV의 IDKEY 컬럼 (14번째, 인덱스 14)
               String teamIdKey = columns[14]; // "KT", "LG" 등
               
               // IDKEY로 TeamEntity 찾기
               TeamEntity team = teamRepository.findByIdKey(teamIdKey).orElse(null);
               if (team == null) {
                   System.err.println("경고: 팀을 찾을 수 없습니다 - " + teamIdKey + " (선수: " + columns[1] + ")");
                   return null; // 팀이 없으면 null 반환하여 해당 선수 스킵
               }
               
               return PlayerEntity.builder()
                   .pNo(Long.parseLong(columns[0]))
                   .playerName(columns[1])
                   .imgUrl(columns[3])
                   .backNo(parseDoubleToInteger(columns[4]))
                   .birth(columns[5])
                   .position(columns[6])
                   .height(parseDouble(columns[7]))
                   .weight(parseDouble(columns[8]))
                   .history(columns[9])
                   .signingFee(columns[10])
                   .salary(columns[11])
                   .draft(columns[12])
                   .joinYear(columns[13])
                   // 추가 통계 정보
                   .year(parseDouble(columns[15]))  // Year
                   .age(parseDouble(columns[16]))   // Age
                   .hand(columns[19])               // hand
                   .playerType(columns[20])         // player_type
                   // 타격 통계 (b_ 접두사)
                   .bAb(parseDoubleToInteger(columns[21]))    // b_AB
                   .bAvg(parseDouble(columns[22]))   // b_AVG
                   .bObp(parseDouble(columns[36]))   // b_OBP
                   .bSlg(parseDouble(columns[48]))   // b_SLG
                   .bOps(parseDouble(columns[37]))   // b_OPS
                   .bHr(parseDoubleToInteger(columns[59]))    // b_HR
                   .bRbi(parseDoubleToInteger(columns[41]))   // b_RBI
                   .bSb(parseDoubleToInteger(columns[44]))    // b_SB
                   .b2B(parseDoubleToInteger(columns[54]))    // b_2B
                   .b3B(parseDoubleToInteger(columns[55]))    // b_3B
                   .bHp(parseDoubleToInteger(columns[58]))    // b_HP
                   .bGdp(parseDoubleToInteger(columns[29]))   // b_GDP
                   .bSf(parseDoubleToInteger(columns[45]))    // b_SF
                   .bSo(parseDoubleToInteger(columns[62]))    // b_SO
                   .bEpa(parseDoubleToInteger(columns[65]))   // b_ePA
                   .bBb(parseDoubleToInteger(columns[56]))    // b_BB
                   .bH(parseDoubleToInteger(columns[57]))     // b_H
                   .bIb(parseDoubleToInteger(columns[60]))    // b_IB
                   .bR(parseDoubleToInteger(columns[61]))     // b_R
                   // 투수 통계 (p_ 접두사)
                   .pW(parseDoubleToInteger(columns[51]))     // p_W
                   .pL(parseDoubleToInteger(columns[35]))     // p_L
                   .pEra(parseDouble(columns[27]))   // p_ERA
                   .pFip(parseDouble(columns[28]))   // p_FIP
                   .pWhip(parseDouble(columns[52]))  // p_WHIP
                   .pIp(parseDouble(columns[34]))    // p_IP
                   .pSo(parseDoubleToInteger(columns[75]))    // p_SO
                   .p2B(parseDoubleToInteger(columns[67]))    // p_2B
                   .p3B(parseDoubleToInteger(columns[68]))    // p_3B
                   .pHr(parseDoubleToInteger(columns[72]))    // p_HR
                   .pHp(parseDoubleToInteger(columns[71]))    // p_HP
                   .pRoe(parseDoubleToInteger(columns[42]))    // p_ROE
                   .pBb(parseDoubleToInteger(columns[69]))    // p_BB
                   .pH(parseDoubleToInteger(columns[70]))     // p_H
                   .pIb(parseDoubleToInteger(columns[73]))    // p_IB
                   .pR(parseDouble(columns[74]))     // p_R
                   .team(team)  // TeamEntity 객체로 저장
                   .build();
           }
    
    private Integer parseDoubleToInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
