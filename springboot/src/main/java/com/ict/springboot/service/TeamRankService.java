package com.ict.springboot.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ict.springboot.dto.TeamRankDto;
import com.ict.springboot.repository.TeamRankRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TeamRankService {

    private final TeamRankRepository teamRankRepository;

    public List<TeamRankDto> getTeamRanksByYear(String year) {
        return teamRankRepository.findByYearOrderByRankAsc(year)
            .stream()
            .map(TeamRankDto::toDto)
            .collect(Collectors.toList());
    }
}

