package com.ict.springboot.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import org.springframework.stereotype.Service;

import com.ict.springboot.dto.PlayerDto;
import com.ict.springboot.entity.PlayerEntity;
import com.ict.springboot.repository.PlayerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlayerService {

    private final PlayerRepository playerRepository;

    /* 전체 조회 */
    public List<PlayerDto> getAll() {
        List<PlayerEntity> playerEntities = playerRepository.findAll();
        return playerEntities.stream().map(entity -> PlayerDto.toDto(entity)).collect(Collectors.toList());
    }

    /* 상세 조회 - 선수 번호로 */
    public PlayerDto getByPNo(Long pNo) {
        PlayerEntity playerEntity = playerRepository.findBypNo(pNo);
        return PlayerDto.toDto(playerEntity);
    }

    /* 팀별 선수 조회 (투수/타자 분리) - 선발투수 12명, 타자 15명 제한 */
    public Map<String, List<PlayerDto>> getPlayersByTeamId(String teamId) {
        // 팀 ID 매핑 (프론트엔드 팀 ID → 백엔드 IDKEY)
        String mappedTeamId = mapTeamIdToBackendIdKey(teamId);
        
        List<PlayerEntity> playerEntities = playerRepository.findByTeamIdKey(mappedTeamId);
        List<PlayerDto> allPlayers = playerEntities.stream()
                .map(entity -> PlayerDto.toDto(entity))
                .collect(Collectors.toList());
        
        // 투수/타자 분리 및 개수 제한
        List<PlayerDto> pitchers = allPlayers.stream()
                .filter(player -> "pitcher".equals(player.getPlayerType()))
                .limit(12)  // 선발투수 최대 12명
                .collect(Collectors.toList());
                
        List<PlayerDto> batters = allPlayers.stream()
                .filter(player -> "batter".equals(player.getPlayerType()))
                .sorted((p1, p2) -> {
                    // 타율 비교 (내림차순)
                    Double avg1 = p1.getBattingStats() != null && p1.getBattingStats().getAvg() != null 
                        ? p1.getBattingStats().getAvg() : 0.0;
                    Double avg2 = p2.getBattingStats() != null && p2.getBattingStats().getAvg() != null 
                        ? p2.getBattingStats().getAvg() : 0.0;
                    return Double.compare(avg2, avg1); // 내림차순 정렬
                })
                .limit(15)  // 타자 최대 15명
                .collect(Collectors.toList());
        
        Map<String, List<PlayerDto>> result = new HashMap<>();
        result.put("pitchers", pitchers);
        result.put("batters", batters);
        return result;
    }

    private String mapTeamIdToBackendIdKey(String frontendTeamId) {
        // 프론트엔드 팀 ID를 백엔드 IDKEY로 매핑
        switch (frontendTeamId) {
            case "LG": return "LG";
            case "한화": return "HH";
            case "두산": return "OB";
            case "삼성": return "SS";
            case "KIA": return "HT";
            case "KT": return "KT";
            case "NC": return "NC";
            case "SSG": return "SK";
            case "롯데": return "LT";
            case "키움": return "WO";
            default: return frontendTeamId; // 매핑되지 않은 경우 그대로 사용
        }
    }


}
