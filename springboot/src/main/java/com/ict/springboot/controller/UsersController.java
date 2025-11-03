package com.ict.springboot.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.MailService;
import com.ict.springboot.service.UsersService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UsersController {
    private final MailService mailService;
    private String authCode; // 인증 코드를 문자열로 저장
    private final UsersService usersService;

    // 전체조회 (데이터 여러개)
    @GetMapping("")
    public List<UsersDto> getAllUser(@RequestParam Map<String, String> params) {
        if (params != null) {   // ? 붙은 파라미터로 검색 기능을 전체 조회에서 구현한다 (안해도 무방)
            return usersService.searchByParams(params);
        }
        return usersService.getAll();
    }

    // 상세조회 (데이터 하나만)
    @GetMapping("/{account}")
    public UsersDto getUserByID(@PathVariable String account) {
        return usersService.getByAccount(account);
    }

    // 등록
    @PostMapping("")
    public UsersDto createUser(@RequestBody UsersDto dto) {
        return usersService.create(dto);
    }

    // 수정
    @PutMapping("/{account}")
    public ResponseEntity<?> updateUser(@PathVariable String account, @RequestBody UsersDto dto, HttpServletRequest request) {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(usersService.update(account, dto, loginUser));
    }

    // 삭제
    @DeleteMapping("/{account}")
    public UsersDto deleteUser(@PathVariable String account) throws Exception {
        return usersService.delete(account);
    }

    //  비밀번호 변경
    @PatchMapping("/{id}/password")
    public ResponseEntity<?> changePassword(@PathVariable Long id, @RequestBody Map<String, String> passwords, HttpServletRequest request) {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(usersService.changePassword(id, passwords, loginUser));
    }
    

    // 이메일 인증 코드 발송
    @PostMapping("mailSend")
    public ResponseEntity<Map<String, Object>> mailSend(@RequestParam String email) {
        Map<String, Object> map = new HashMap<>();
        try {
            this.authCode = mailService.sendMail(email);
            
            map.put("success", true);
            map.put("code", authCode);
            return ResponseEntity.ok(map); // 상태 코드 200과 함께 응답 반환
        } catch (Exception e) {
            map.put("success", false);
            map.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map); // 상태 코드 500과 함께 응답 반환
        }
    }

    // 이메일 인증
    @GetMapping("mailCheck")
    public ResponseEntity<?> MailCheck(@RequestParam String checkNumber) {
        boolean isMatch = checkNumber.equals(this.authCode);
        return ResponseEntity.ok(isMatch);
    }

    // 포인트 조회
    @GetMapping("/{id}/point")
    public ResponseEntity<?> getPoint(@PathVariable Long id) {
        Long point = usersService.getPoint(id);
        return ResponseEntity.ok(Map.of("point", point));
    }
}