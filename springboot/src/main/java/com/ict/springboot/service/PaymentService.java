package com.ict.springboot.service;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ict.springboot.dto.PaymentRequestDto;
import com.ict.springboot.dto.ReceiptDto;
import com.ict.springboot.entity.ReceiptEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.ReceiptRepository;
import com.ict.springboot.repository.UsersRepository;

import jakarta.persistence.EntityNotFoundException;
import kr.co.bootpay.pg.Bootpay;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentService {

    private final ReceiptRepository receiptRepository;
    private final UsersRepository usersRepository;
    private final FraudDetectionService fraudDetectionService;
    private final BlockchainService blockchainService;

    public boolean verifyAndSave(PaymentRequestDto req) {

        Bootpay bootpay = new Bootpay("68d544dcb96306619f55ba78", "WF/pgSB+UqdnPovZnnaTil1TsnHakxJgg65jpX5yF8c=");

        System.out.println("userId : " + req.getUserId());
        System.out.println("userName : " + req.getUserName());
        System.out.println("req : " + req);

        try {
            // 1️⃣ 서버 토큰 발급
            HashMap<String, Object> res = bootpay.getAccessToken();
            if (res.get("error_code") != null) {
                System.out.println("토큰 발급 실패 : " + res);
                return false;
            }

            // 2️⃣ 이상거래 탐지
            double riskScore = fraudDetectionService.evaluate(req);
            boolean isSuspicious = riskScore > 0.4;
            System.out.println("탐지결과: score=" + riskScore + ", suspicious=" + isSuspicious);

            // 블록체인 기록
            blockchainService.recordDetectionHash(req, riskScore, isSuspicious);

            if (isSuspicious) {
                // 이상거래이면 DB 저장 및 포인트 충전 하지 않고 종료
                System.out.println("⚠️ 이상거래 감지 — 결제 실패 처리");
                return false;
            }

            // 3️⃣ 정상 거래 처리
            UsersEntity user = usersRepository.findById(req.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));
            user.chargePoint(req.getPrice());

            ReceiptEntity receipt = new ReceiptEntity(
                req.getReceiptId(),
                req.getOrderId(),
                req.getName(),
                req.getPrice(),
                req.getUserId(),
                req.getUserName()
            );
            receiptRepository.save(receipt);

            System.out.println("✅ 정상거래 저장 완료");
            return true;

        } catch (Exception e) {
            System.out.println("Error : " + e);
            return false;
        }
    }
    

    // 포인트 충전 내역 조회
    public List<ReceiptDto> getPaymentHistory(Long userId) {
        List<ReceiptEntity> receipts = receiptRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return receipts.stream()
                .map(ReceiptDto::toDto)
                .collect(Collectors.toList());
    }
}
