package com.ict.springboot.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

import com.ict.springboot.dto.PaymentRequestDto;
import com.ict.springboot.entity.ReceiptEntity;
import com.ict.springboot.repository.ReceiptRepository;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final ReceiptRepository receiptRepository; // final로 DI 보장
    private final RestTemplate restTemplate; // 외부 Bean으로 주입 가능

    @Value("${fastapi.server-url}")
    private String fastApiServerUrl;

    public double evaluate(PaymentRequestDto req) {
        double score = 0.0;

        // 1️⃣ 최근 1분간 DB 거래 조회
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minus(1, ChronoUnit.MINUTES);
        List<ReceiptEntity> recentTx = receiptRepository.findRecentByUserId(req.getUserId(), oneMinuteAgo);
        if (recentTx.size() >= 2) score += 0.5;

        // 2️⃣ AI 모델 호출 (FastAPI)
        double aiScore = predict(req); 
        score += aiScore;

        return Math.min(score, 1.0);
    }

    public double predict(PaymentRequestDto req) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", req.getPrice());
            payload.put("transaction_count", req.getTransactionCount());
            payload.put("account_age_days", req.getAccountAgeDays());
            payload.put("device_change_count", req.getDeviceChangeCount());
            payload.put("ip_risk_score", req.getIpRiskScore());

            ResponseEntity<Map> resp = restTemplate.postForEntity(fastApiServerUrl + "/recordDetection", payload, Map.class);
            if (resp.getBody() != null && resp.getBody().get("score") != null) {
                return ((Number) resp.getBody().get("score")).doubleValue();
            }
        } catch (Exception e) {
            System.out.println("AI 모델 호출 실패: " + e);
        }
        return 0.0;
    }
}
