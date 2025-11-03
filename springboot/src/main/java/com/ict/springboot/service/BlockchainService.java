package com.ict.springboot.service;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import com.ict.springboot.dto.PaymentRequestDto;

@Service
@RequiredArgsConstructor
public class BlockchainService {

    @Value("${fastapi.server-url}")
    private String fastapiServerUrl;
    private final RestTemplate restTemplate = new RestTemplate();

    public void recordDetectionHash(PaymentRequestDto req, double riskScore, boolean isSuspicious) {
        try {
            String url = fastapiServerUrl + "/recordDetection";
            Map<String, Object> body = Map.of(
                "data", Map.of(
                    "paymentId", req.getReceiptId(),
                    "userId", req.getUserId(),
                    "orderId", req.getOrderId(),
                    "name", req.getName(),
                    "price", req.getPrice(),
                    "userName", req.getUserName(),
                    "score", riskScore,
                    "result", isSuspicious ? "fraudulent" : "normal",
                    "timestamp", System.currentTimeMillis()
                )
            );
            restTemplate.postForEntity(url, body, String.class);
            System.out.println("블록체인 기록 요청 성공");
        } catch (Exception e) {
            System.out.println("블록체인 기록 실패: " + e.getMessage());
        }
    }
}
