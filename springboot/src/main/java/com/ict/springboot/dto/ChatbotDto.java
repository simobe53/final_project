package com.ict.springboot.dto;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotDto {
    private String selectedType; //질문의 타입
    private String message;      //질문의 내용
    private List<Object> chat_history; //대화 히스토리 (RAG용)

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
