package com.ict.springboot.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ict.springboot.dto.TeamDto;
import com.ict.springboot.entity.TeamEntity;
import com.ict.springboot.repository.TeamRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepo;

    public List<TeamDto> getAll() {
        List<TeamEntity> teams = teamRepo.findAll();
        return teams.stream().map(team->TeamDto.toDto(team)).collect(Collectors.toList());
    }
    
    
}
