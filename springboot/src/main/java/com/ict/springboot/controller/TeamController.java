package com.ict.springboot.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.TeamDto;
import com.ict.springboot.service.TeamService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    
    @GetMapping("")
    public List<TeamDto> getAllTeams() {
        return teamService.getAll();
    }
}
