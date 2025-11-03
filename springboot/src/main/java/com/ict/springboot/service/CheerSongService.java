package com.ict.springboot.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ict.springboot.dto.CheerSongDto;
import com.ict.springboot.entity.CheerSongEntity;
import com.ict.springboot.entity.TeamEntity;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.repository.CheerSongRepository;
import com.ict.springboot.repository.TeamRepository;
import com.ict.springboot.repository.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheerSongService {

    private final CheerSongRepository cheerSongRepository;
    private final UsersRepository usersRepository;
    private final TeamRepository teamRepository;

    // 응원곡 저장
    @Transactional
    public CheerSongDto saveCheerSong(CheerSongDto dto, Long userId) {
        UsersEntity user = usersRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

        TeamEntity team = teamRepository.findByIdKey(dto.getTeamId())
            .orElseThrow(() -> new RuntimeException("팀을 찾을 수 없습니다"));

        CheerSongEntity entity = CheerSongDto.toEntity(dto, user, team);
        CheerSongEntity saved = cheerSongRepository.save(entity);
        return CheerSongDto.toDto(saved);
    }

    // 공유된 응원곡 목록 조회
    public List<CheerSongDto> getSharedCheerSongs() {
        return cheerSongRepository.findByIsSharedTrueOrderByCreatedAtDesc()
            .stream()
            .map(CheerSongDto::toDto)
            .collect(Collectors.toList());
    }

    // 특정 사용자의 응원곡 목록 조회
    public List<CheerSongDto> getUserCheerSongs(Long userId) {
        return cheerSongRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(CheerSongDto::toDto)
            .collect(Collectors.toList());
    }

    // 응원곡 상세 조회
    public CheerSongDto getCheerSong(Long songId) {
        CheerSongEntity entity = cheerSongRepository.findById(songId)
            .orElseThrow(() -> new RuntimeException("응원곡을 찾을 수 없습니다"));
        return CheerSongDto.toDto(entity);
    }

    // 응원곡 삭제
    @Transactional
    public void deleteCheerSong(Long songId, Long userId) {
        CheerSongEntity entity = cheerSongRepository.findById(songId)
            .orElseThrow(() -> new RuntimeException("응원곡을 찾을 수 없습니다"));

        if (entity.getUser() == null || !userId.equals(entity.getUser().getId())) {
            throw new RuntimeException("삭제 권한이 없습니다");
        }

        cheerSongRepository.delete(entity);
    }

    // 응원곡 공유 토글
    @Transactional
    public CheerSongDto toggleShare(Long songId, Long userId) {
        CheerSongEntity entity = cheerSongRepository.findById(songId)
            .orElseThrow(() -> new RuntimeException("응원곡을 찾을 수 없습니다"));

        if (entity.getUser() == null || !userId.equals(entity.getUser().getId())) {
            throw new RuntimeException("공유 권한이 없습니다");
        }

        entity.setIsShared(!entity.getIsShared());
        CheerSongEntity saved = cheerSongRepository.save(entity);
        return CheerSongDto.toDto(saved);
    }
}
