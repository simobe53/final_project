package com.ict.springboot.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ict.springboot.entity.TeamEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.TeamRepository;
import com.ict.springboot.repository.UsersRepository;

@Configuration
public class UserInitConfig {
    public static List<List<String>> teams = List.of(
        List.of("롯데 자이언츠", "부산광역시 동래구 사직로 45", "LT", "사직야구장"),
        List.of("LG 트윈스", "서울특별시 송파구 올림픽로 25", "LG", "잠실야구장"),
        List.of("한화 이글스", "대전광역시 중구 대종로 373", "HH", "대전한화생명볼파크"),
        List.of("삼성 라이온즈", "대구광역시 수성구 연호동 야구장로", "SS", "대구삼성라이온즈파크"),
        List.of("키움 히어로즈", "서울특별시 구로구 경인로 430", "WO", "고척스카이돔"),
        List.of("두산 베어스", "서울특별시 송파구 올림픽로 25", "OB", "잠실야구장"),
        List.of("KIA 타이거즈", "광주광역시 북구 서림로 10", "HT", "광주기아챔피언스필드"),
        List.of("KT Wiz", "경기도 수원시 장안구 경수대로 893", "KT", "수원KT위즈파크"),
        List.of("NC 다이노스", "경상남도 창원시 마산회원구 삼호로 63", "NC", "창원NC파크"),
        List.of("SSG 랜더스", "인천광역시 미추홀구 매소홀로 618", "SK", "인천SSG랜더스필드")
    );
    public static List<String> username = List.of("불주먹", "주민", "이장", "집순이", "김병만");

    @Bean
    public CommandLineRunner userInitializer(UsersRepository usersRepository, TeamRepository teamRepo) {
        
        return args -> {
            int count = 0;
            for(List<String> t : teams) {
                String teamName = t.get(0);
                String teamLoc = t.get(1);
                String idKey = t.get(2);
                String stadium = t.get(3);
                TeamEntity team = teamRepo.findByName(teamName)
                .orElseGet(() -> teamRepo.save(
                    TeamEntity.builder()
                    .name(teamName)
                    .location(teamLoc)
                    .idKey(idKey)
                    .stadium(stadium)
                    .build()
                ));

                for(int i=0; i<username.size(); i++) {
                    String profile = loadProfileBase64("profile"+(i+1)+".txt");
                    String account = "test" + (count == 0 ? "" : count) + (i+1);
                    String[] teamShort = teamName.split(" ");
                    if (!usersRepository.existsByAccount(account)) {
                        UsersEntity testUser = UsersEntity.builder()
                                .account(account)
                                .email(account+"@test.com")
                                .password("1111")
                                .name(teamShort[0]+' '+username.get(i))
                                .role("USER")
                                .gender((i == 1 || i == 3) ? "여자" : "남자")
                                .profileImage(profile)
                                .team(team)
                                .build();
                        usersRepository.save(testUser);
                    }
                }
                count ++;
            }
            
            // test유저 5개.  아이디: test1, test2, test3, test4, test5  비번: 1111
            String teamName = teams.get(0).get(0);
            TeamEntity team = teamRepo.findByName(teamName)
            .orElseGet(() -> teamRepo.save(
                TeamEntity.builder()
                .name(teamName)
                .idKey(teams.get(0).get(2))
                .location(teams.get(0).get(1))
                .build()
            ));

            String profile = loadProfileBase64("profile.txt");
            //admin 계정 생성
            String account = "admin";
            if (!usersRepository.existsByAccount(account)) {
                UsersEntity testUser = UsersEntity.builder()
                        .account(account)
                        .email(account+"@test.com")
                        .password("1111")
                        .name("관리자")
                        .role("ADMIN")
                        .gender("여자")
                        .profileImage(profile)
                        .team(team)
                        .build();
                usersRepository.save(testUser);
            }
        };
    }

    private String loadProfileBase64(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (is == null) throw new FileNotFoundException(filename + " not found");
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
} 