package com.ict.springboot.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReceiptEntity {

    //영수증 고유키
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //주문별 아이디
    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;

    //영수증별 아이디
    @Column(name = "receipt_id", nullable = false, unique = true)
    private String receiptId;

    //주문한 내용
    @Column(name = "item_name", nullable = false)
    private String itemName;

    //주문한 가격
    @Column(nullable = false)
    private Long price;

    //주문한 유저 고유키
    @Column(name = "user_id", nullable = false)
    private Long userId;

    //주문한 유저 이름
    @Column(name = "user_name", nullable = false)
    private String userName;

    //결제한 시간
    @CreationTimestamp
    @ColumnDefault("SYSDATE")
    @Column(name = "created_at",nullable = false,updatable = false)
    private LocalDateTime created_at;

    // 생성자로만 세팅 나중에 Long userId 추가
    public ReceiptEntity(String receiptId, String orderId, String itemName, Long price, Long userId, String userName) {
        this.receiptId = receiptId;
        this.orderId = orderId;
        this.itemName = itemName;
        this.price = price;
        this.userId = userId;
        this.userName = userName;
        this.created_at = LocalDateTime.now();
    }

}