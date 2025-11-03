package com.ict.springboot.service;

import java.util.*;

import org.springframework.stereotype.Service;

import com.ict.springboot.dto.ArticleDto;
import com.ict.springboot.entity.SimulationEntity;
import com.ict.springboot.repository.ArticleRepository;
import com.ict.springboot.repository.SimulationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ArticleService {
    private final ArticleRepository articleRepository;
    private final SimulationRepository simulationRepository;

    public List<ArticleDto> getArticles(Long simulationId) {
        SimulationEntity simulation = simulationRepository.findById(simulationId)
            .orElseThrow(() -> new RuntimeException("Simulation not found"));

        return articleRepository.findBySimulation(simulation)
            .stream()
            .map(article -> ArticleDto.builder()
                    .id(article.getId())
                    .simulationId(article.getSimulation().getId())
                    .teamName(article.getTeamName())
                    .content(article.getContent())
                    .build())
            .toList();
    }
    
}
