package com.ict.springboot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.TeamRankDto;
import com.ict.springboot.service.TeamRankService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/team-ranks")
@RequiredArgsConstructor
public class TeamRankController {

    private final TeamRankService teamRankService;
    
    @GetMapping("/{year}")
    public List<TeamRankDto> getTeamRanksByYear(@PathVariable String year) {
        return teamRankService.getTeamRanksByYear(year);
    }
}

