package com.ict.springboot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
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

import com.ict.springboot.dto.MeetApplyDto;
import com.ict.springboot.dto.MeetCommentsDto;
import com.ict.springboot.dto.MeetsDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.MeetCommentsService;
import com.ict.springboot.service.MeetsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "직관 모임", description = "직관 모임 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meets")
public class MeetsController {

    private final MeetsService meetsService;
    private final MeetCommentsService meetCommentsService;

    @Operation(summary = "모임 게시글 생성")
    @PostMapping("")
    public MeetsDto createMeet(@RequestBody MeetsDto dto){
        return meetsService.create(dto);
    }
    @Operation(summary = "전체 게시글 조회")
    @GetMapping("/all")
    public ResponseEntity<?> getMeetAll(HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
//        if (loginUser == null) {
//            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
//        }
        List<MeetsDto> meetsDtos = meetsService.meetAllByTeam(loginUser);
        return ResponseEntity.ok(meetsDtos);
    }
    @Operation(summary = "게시글 페이지네이션 조회")
    @GetMapping("")
    public ResponseEntity<?> getMeetAllPaged(HttpServletRequest request, @PageableDefault(page = 1,sort = "createdAt",direction = Direction.DESC) Pageable pageable){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
//        if (loginUser == null) {
//            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
//        }
        System.out.println(pageable.getPageNumber());
        pageable = PageRequest.of(pageable.getPageNumber() , 6 , pageable.getSort());
        Page<MeetsDto> meetsAndPage = meetsService.meetAllByUserTeam(loginUser,pageable);
        return ResponseEntity.ok(meetsAndPage.getContent());
    }
    @Operation(summary = "게시글 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<?> getMeet(@PathVariable long id, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
//        if (loginUser == null) {
//            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
//        }
        MeetsDto meetsDto = meetsService.getMeet(id, loginUser);
        return ResponseEntity.ok(meetsDto);
    }
    @Operation(summary = "마감 임박 모집 조회")
    @GetMapping("/top")
    public ResponseEntity<?> getMeetForMain(@RequestParam int limit, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        return ResponseEntity.ok(meetsService.getClosingSoonMeets(limit, loginUser));
    }
    @Operation(summary = "게시글 삭제")
    @DeleteMapping("/{id}")
    public void deleteMeet(@PathVariable long id){
        meetsService.deleteMeet(id);
    }
    @Operation(summary = "게시글 수정")
    @PutMapping("/{id}")
    public MeetsDto updateMeet(@PathVariable String id,@RequestBody MeetsDto dto){
        return meetsService.updateMeet(dto);
    }
    @Operation(summary = "댓글 작성")
    @PostMapping("/{id}/comments")
    public ResponseEntity<?> createMeetComment(@PathVariable long id, @RequestBody MeetCommentsDto dto, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        MeetCommentsDto meetCommentsDto = meetCommentsService.createComment(dto);
        return ResponseEntity.ok(meetCommentsDto);
    }
    @Operation(summary = "댓글 조회")
    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getMeetComment(@PathVariable long id, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
//        if (loginUser == null) {
//            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
//        }
        return ResponseEntity.ok(meetCommentsService.getComment(id));
    }
    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteMeetComment(@PathVariable long id, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        meetCommentsService.deleteComment(id);
        return ResponseEntity.ok().build();
    }
    @Operation(summary = "모임 지원")
    @PostMapping("/{id}/apply")
    public ResponseEntity<?> createMeetApply(@PathVariable long id, @RequestBody MeetApplyDto dto, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        MeetApplyDto meetApplyDto = meetsService.createApply(dto);
        //지원 여부 true
        meetApplyDto.getMeet().setApply(true);
        return ResponseEntity.ok(meetApplyDto);
    }
    @Operation(summary = "지원 목록 조회")
    @GetMapping("/{id}/apply")
    public ResponseEntity<?> getMeetApplies(@PathVariable long id, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
//        if (loginUser == null) {
//            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
//        }
        return ResponseEntity.ok(meetsService.getApplyAll(id));
    }
    @Operation(summary = "지원 승인")
    @PatchMapping("/{id}/apply/accept")
    public ResponseEntity<?> acceptApply(@PathVariable long id, @RequestBody UsersDto dto, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        MeetApplyDto applyDto = meetsService.acceptApplyUser(id, dto);
        return ResponseEntity.ok(applyDto);
    }
    @Operation(summary = "지원 거절")
    @PatchMapping("/{id}/apply/reject")
    public ResponseEntity<?> rejectApply(@PathVariable long id, @RequestBody UsersDto dto, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        MeetApplyDto applyDto  = meetsService.rejectApplyUser(id, dto);
        return ResponseEntity.ok(applyDto);
    }
    @Operation(summary = "모집 마감")
    @PatchMapping("/{id}/apply/close")
    public ResponseEntity<?> closeApply(@PathVariable long id, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        MeetsDto meetsDto = meetsService.closeApply(id);
        return ResponseEntity.ok(meetsDto);
    }
    
    @Operation(summary = "지원 취소")
    @DeleteMapping("{id}/apply/cancle")
    public ResponseEntity<?> cancleApplyMeet(@PathVariable long id, @RequestParam long userId, HttpServletRequest request){
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        if (loginUser == null) {
            return ResponseEntity.status(401).body(Map.of("message", "로그인이 필요합니다."));
        }
        MeetsDto dto = meetsService.cancleApply(id, userId);
        dto.setApply(false);
        return ResponseEntity.ok(dto);
    }
}
