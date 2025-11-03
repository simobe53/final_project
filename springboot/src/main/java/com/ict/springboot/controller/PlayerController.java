package com.ict.springboot.controller;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.PlayerDto;
import com.ict.springboot.service.PlayerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/players")
@RequiredArgsConstructor
public class PlayerController {
    
    private final PlayerService playerService;
    
    @GetMapping
    public ResponseEntity<List<PlayerDto>> getAllPlayers() {
        List<PlayerDto> players = playerService.getAll();
        return ResponseEntity.ok(players);
    }
    
    @GetMapping("/{pNo}")
    public ResponseEntity<PlayerDto> getPlayerByPNo(@PathVariable Long pNo) {
        PlayerDto player = playerService.getByPNo(pNo);
        if (player != null) {
            return ResponseEntity.ok(player);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/teams/{teamId}/players")
    public ResponseEntity<Map<String, List<PlayerDto>>> getTeamPlayers(@PathVariable String teamId) {
        Map<String, List<PlayerDto>> playersByTeam = playerService.getPlayersByTeamId(teamId);
        return ResponseEntity.ok(playersByTeam);
}
    
}
