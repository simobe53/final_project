package com.ict.springboot.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict.springboot.dto.ScheduleDto;
import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.service.ScheduleService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheService;

    private LocalDate toDate(String datestr) {
        // date 는 yyyy-MM-dd로 들어옴
        String[] dts = datestr.split("-");
        return LocalDate.of(Integer.valueOf(dts[0]), Integer.valueOf(dts[1]), Integer.valueOf(dts[2]));
    }

    @GetMapping("")
    public ResponseEntity<?> getListByDate(@RequestParam String date) {
        if (date.isEmpty()) { // date가 들어오지 않을 시, 미래의 일정만 반환하도록 (전부 반환하는 경우는 없다)
            List<ScheduleDto> schedules = scheService.getFutureList();
            return ResponseEntity.ok(schedules);
        }
        LocalDate dt = toDate(date);
        List<ScheduleDto> schedules = scheService.getListByDate(dt);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/today")
    public ResponseEntity<?> getListToday() {
        String url = "https://api-gw.sports.naver.com/cms/templates/kbaseball_new_home_feed";
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Referer", "https://sports.naver.com/");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        // RestTemplate으로 API 호출
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            entity,
            String.class
        );

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root;
        try {
            root = mapper.readTree(response.getBody());
            JsonNode items = root.path("result").path("templates");
            List<ScheduleDto> schedules = new ArrayList<>();
            for (JsonNode item : items) {
                if (!item.get("templateId").asText().equals("section_today_match_template")) continue;
                JsonNode lists = item.path("json").path("schedules");
                long id = 0;
                for (JsonNode list : lists) {
                    if (!list.get("categoryId").asText().equals("kbo")) continue;
                    String[] dates = list.get("gameStartTime").asText().split(" ");
                    ScheduleDto schedule = ScheduleDto.builder()
                    .id(id)
                    .awayTeam(list.get("away").get("teamName").asText())
                    .awayTeamScore(list.get("away").get("teamScore").asInt())
                    .gameDate(toDate(dates[0]))
                    .gameTime(dates[1])
                    .nowInning(list.get("statusInfo").asText())
                    .homeTeam(list.get("home").get("teamName").asText())
                    .homeTeamScore(list.get("home").get("teamScore").asInt())
                    .stadium(list.get("stadium").asText())
                    .build();
                    schedules.add(schedule);
                    id ++;
                }
            }
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/{id}")
    public ScheduleDto getById(@PathVariable Long id) {
        return scheService.getById(id);
    }
    
    /**
     * 사용자의 응원 팀 일정 조회
     * @param limit 조회할 일정 개수 (기본 5개)
     * @return 응원 팀의 최신 일정 리스트
     */
    @GetMapping("/my-team")
    public ResponseEntity<?> getMyTeamSchedules(
        @RequestParam(defaultValue = "5") int limit,
        @RequestParam(defaultValue = "false") boolean highlightOnly,
        HttpServletRequest request)
    {
        UsersDto loginUser = (UsersDto) request.getAttribute("user");
        List<ScheduleDto> schedules = scheService.getLatestSchedules(limit, highlightOnly, loginUser);
        return ResponseEntity.ok(schedules);
    }

    /**
     * 하이라이트 영상 AI 요약 생성
     * @param id 일정 ID
     * @return 성공 시 요약 텍스트, 실패 시 에러 메시지
     */
    @PostMapping("/{id}/generate-summary")
    public ResponseEntity<?> generateHighlightSummary(@PathVariable Long id, HttpServletRequest request) {
        try {
            UsersDto loginUser = (UsersDto) request.getAttribute("user");
            if(loginUser == null){
                Map<String, Object> unauthorized = new HashMap<>();
                unauthorized.put("success", false);
                unauthorized.put("error", "로그인이 필요합니다.");
                return ResponseEntity.status(401).body(unauthorized);
            }
            String summary = scheService.generateHighlightSummary(id);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summary);
            response.put("message", "요약이 성공적으로 생성되었습니다.");
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
            
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
