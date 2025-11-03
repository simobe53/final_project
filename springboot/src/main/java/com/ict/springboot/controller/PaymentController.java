package com.ict.springboot.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ict.springboot.dto.PaymentRequestDto;
import com.ict.springboot.dto.ReceiptDto;
import com.ict.springboot.service.BlockchainService;
import com.ict.springboot.service.FraudDetectionService;
import com.ict.springboot.service.PaymentService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
        private final FraudDetectionService fraudDetectionService;
    private final BlockchainService blockchainService;

    @PostMapping("/fraud")
    public ResponseEntity<?> checkFraud(@RequestBody PaymentRequestDto req) {
        try {
            double score = fraudDetectionService.evaluate(req);
            boolean isSuspicious = score > 0.4;

            // 블록체인 기록
            blockchainService.recordDetectionHash(req, score, isSuspicious);

            if (isSuspicious) {
                return ResponseEntity.ok(Map.of(
                    "allow", false,
                    "message", "⚠️ 이상거래 감지로 결제가 불가합니다."
                ));
            }
            return ResponseEntity.ok(Map.of("allow", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("allow", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody PaymentRequestDto req, HttpSession session) {
        try {
            boolean success = paymentService.verifyAndSave(req);
            if (success) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "결제 검증 성공"
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "⚠️ 이상거래가 탐지되어 결제가 실패했습니다."
                ));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // 포인트 충전 내역 조회
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getPaymentHistory(@PathVariable Long userId) {
        try {
            List<ReceiptDto> history = paymentService.getPaymentHistory(userId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}