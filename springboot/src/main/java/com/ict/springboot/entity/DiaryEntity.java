package com.ict.springboot.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "diary")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = true)
    private ScheduleEntity schedule;

    private LocalDate diaryDate;
    private String ticketUrl;

    @ElementCollection
    @CollectionTable(name = "diary_photos", joinColumns = @JoinColumn(name = "diary_id"))
    @Column(name = "photo_url")
    private java.util.List<String> photoUrls;

    @Lob
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UsersEntity user;

    @Column(name = "total_games")
    private Integer totalGames;

    @Column(name = "total_wins")
    private Integer totalWins;
}
