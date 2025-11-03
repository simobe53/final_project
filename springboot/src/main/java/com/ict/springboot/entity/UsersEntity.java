package com.ict.springboot.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USERS")
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsersEntity {
    
    @Id
    @SequenceGenerator(name = "SEQ_USERS_GENERATOR",sequenceName = "SEQ_USERS",allocationSize = 1,initialValue = 1)
    @GeneratedValue(generator = "SEQ_USERS_GENERATOR",strategy = GenerationType.SEQUENCE)
    @Column(length = 20,nullable = false)
    private long id;
    
    @Column(length = 100,nullable = false, unique=true)
    private String account;

    @Column(length = 50,nullable = false)
    private String password;

    @Column(length = 20,nullable = false)
    private String name;

    @Column(length = 30,nullable = false)
    private String email;

    @Column(length = 30,nullable = true)
    private String method; // 가입방법 (null, "NAVER", "GOOGLE")

    @Column(nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private Long point = 0L; // 유저 충전 금액

    @Lob
    @Column(name="profile_image", columnDefinition="CLOB")  // BASE64로 저장하기 위한 LONGTEXT 설정
    private String profileImage;

    @Column(length = 20, nullable = true)
    private String gender;

    @Column(length = 10, nullable=false)
    @ColumnDefault("'USER'")   // 디폴트값 문자열로 적을때 ''로 감싸야됨
    private String role;
    
    @Column(name="created_at")
    @ColumnDefault("SYSDATE")
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @ManyToOne
    @JoinColumn(name="team_id")
    private TeamEntity team;

    public void chargePoint(Long point) {
        if (point <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.point += point;
    }

    // 포인트 사용
    public void usePoint(Long point) {
        if (point <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        if (this.point < point) {
            throw new IllegalStateException("포인트가 부족합니다.");
        }
        this.point -= point;
    }

}
