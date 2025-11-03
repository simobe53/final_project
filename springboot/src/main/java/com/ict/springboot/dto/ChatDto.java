package com.ict.springboot.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatDto {
    private String type; // 메시지의 타입
    private Long id;     // 메시지를 보내는 유저의 아이디
    private String name; // 메시지를 보내는 유저 이름
    private String message; // 메시지의 내용
    private String profileImage; // 메시지를 보내는 유저 프로필 사진
    private TeamDto userTeam; // 메시지를 보내는 유저가 응원하는 팀
    private String align; // 채팅방에서 팀별 정렬을 위한 변수
    private int isHome; // 채팅방에서 팀별공지를 위한 변수
    private List<String> audioUrl; // 편파해설 TTS 오디오 URL (단일 또는 배열)

    //dto를 json으로 파싱할 때 쓰는 용도
    private static final ObjectMapper objectMapper = new ObjectMapper();

    //dto에 값을 넣어놓으면 toJson을 통해 json값으로 반환할 수 있다.
    public String toJson() {
        try {
            return objectMapper.writeValueAsString(this);
        }
        catch(JsonProcessingException e) {
            e.printStackTrace();
            return "{}";
        }
    }

}
