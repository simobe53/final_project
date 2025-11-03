package com.ict.springboot.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequestDto {
    private String receiptId;
    private String orderId;
    private Long price;
    private String name;
    private Long userId;
    private String userName;
    private Integer transactionCount;
    private Integer accountAgeDays;
    private Integer deviceChangeCount;
    private Double ipRiskScore;
}