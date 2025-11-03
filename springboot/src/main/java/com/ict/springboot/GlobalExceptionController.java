package com.ict.springboot;

import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.persistence.EntityNotFoundException;

@RestControllerAdvice
public class GlobalExceptionController {

    //<< 모든 컨트롤러의 엔터티 조회 불능시 예외 처리 >> 
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> entityNotFound(EntityNotFoundException e) {
        Map<String, String> map = new HashMap<>();
        map.put("ERROR", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "존재하지 않는 엔티티입니다.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map);
    }

    //<< 부모 삭제 시 자식 레코드가 있는 경우 >>
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> dataIntegrityViolation(DataIntegrityViolationException e) {
        Map<String, String> map = new HashMap<>();
        map.put("ERROR", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "자식 레코드가 발견되었습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
    }

    //<< 파일 업로드 용량 초과 예외 >>
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> maxUploadSizeError(MaxUploadSizeExceededException e) {
        Map<String, String> map = new HashMap<>();
        map.put("ERROR", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "파일 업로드 최대 용량을 초과 했어요.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
    }

    //<< 잘못된 상태 예외 >>
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> illegalState(IllegalStateException e) {
        Map<String, String> map = new HashMap<>();
        map.put("ERROR", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "잘못된 요청 상태입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
    }

    //<< 잘못된 인자 예외 >>
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> illegalArgument(IllegalArgumentException e) {
        Map<String, String> map = new HashMap<>();
        map.put("ERROR", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "잘못된 요청입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
    }

    
    //<<실행중 오류 예외 처리 >>
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> runtimeException(RuntimeException e) {
        Map<String, String> map = new HashMap<>();
        map.put("ERROR", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "런타임 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);        
    }

    //<< 기타 예상 못한 예외 >>
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOtherExceptions(Exception e) {
        Map<String, String> map = new HashMap<>();
        map.put("ERROR", e.getMessage() != null && !e.getMessage().isBlank()
                        ? e.getMessage()
                        : "예기치 못한 오류가 발생했습니다.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
    }
}