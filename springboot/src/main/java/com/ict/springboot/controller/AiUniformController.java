package com.ict.springboot.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.AiUniformDto;
import com.ict.springboot.service.AiUniformService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ai-uniform")
@RequiredArgsConstructor
public class AiUniformController {
    
    private final AiUniformService aiUniformService;
    
    /**
     * AI 유니폼 저장
     */
    @PostMapping
    public ResponseEntity<AiUniformDto> saveAiUniform(@RequestBody AiUniformDto dto) {
        try {
            AiUniformDto savedDto = aiUniformService.saveAiUniform(dto);
            return ResponseEntity.ok(savedDto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 사용자별 AI 유니폼 목록 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AiUniformDto>> getAiUniformsByUserId(@PathVariable Long userId) {
        try {
            List<AiUniformDto> uniforms = aiUniformService.getAiUniformsByUserId(userId);
            return ResponseEntity.ok(uniforms);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 팀별 AI 유니폼 목록 조회
     */
    @GetMapping("/team/{teamId}")
    public ResponseEntity<List<AiUniformDto>> getAiUniformsByTeamId(@PathVariable Long teamId) {
        try {
            List<AiUniformDto> uniforms = aiUniformService.getAiUniformsByTeamId(teamId);
            return ResponseEntity.ok(uniforms);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 파일명으로 AI 유니폼 조회
     */
    @GetMapping("/filename/{filename}")
    public ResponseEntity<AiUniformDto> getAiUniformByFilename(@PathVariable String filename) {
        try {
            Optional<AiUniformDto> uniform = aiUniformService.getAiUniformByFilename(filename);
            return uniform.map(ResponseEntity::ok)
                         .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * AI 유니폼 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAiUniform(@PathVariable Long id) {
        try {
            aiUniformService.deleteAiUniform(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
}
