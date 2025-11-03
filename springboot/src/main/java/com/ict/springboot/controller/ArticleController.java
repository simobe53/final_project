package com.ict.springboot.controller;

import java.util.*;

import org.springframework.web.bind.annotation.*;

import com.ict.springboot.dto.ArticleDto;
import com.ict.springboot.service.ArticleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;
    @GetMapping
    public List<ArticleDto> getArticles(@RequestParam("simulation_id") Long simulationId) {
        return articleService.getArticles(simulationId);
    }
}