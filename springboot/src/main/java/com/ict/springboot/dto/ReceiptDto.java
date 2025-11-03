package com.ict.springboot.dto;

import java.time.LocalDateTime;

import com.ict.springboot.entity.ReceiptEntity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptDto {
    private Long id;
    private String receiptId;
    private String orderId;
    private String itemName;
    private Long price;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;

    // Entity -> DTO
    public static ReceiptDto toDto(ReceiptEntity entity) {
        if (entity == null) return null;
        return new ReceiptDto(
            entity.getId(),
            entity.getReceiptId(),
            entity.getOrderId(),
            entity.getItemName(),
            entity.getPrice(),
            entity.getUserId(),
            entity.getUserName(),
            entity.getCreated_at()
        );
    }
}

