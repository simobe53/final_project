import React, { useState } from "react";
import classes from "./PlayerModal.module.scss";

export default function PlayerModal({ player, onClose }) {
  const [isFlipped, setIsFlipped] = useState(false);
  
  if (!player) return null;

  const isPitcher = player.playerType === 'pitcher';
  const battingStats = player.battingStats;
  const pitchingStats = player.pitchingStats;

  const handleFlip = () => {
    setIsFlipped(!isFlipped);
  };

  // joinYear 포맷팅 함수 (예: "24LG" -> "24년 LG")
  const formatJoinYear = (joinYear) => {
    if (!joinYear) return '정보 없음';
    
    // 숫자와 문자를 분리 (정규식 패턴)
    //(\d+): 하나 이상의 숫자 캡처
    //(.*): 나머지 모든 문자 캡처
    const match = joinYear.match(/^(\d+)(.*)$/);
    if (match) {
      const [, year, team] = match;
      return `${year}년 ${team}`;
    }
    
    return joinYear; // 매치되지 않으면 원본 반환
  };

  return (
    <div className={classes.modalOverlay} onClick={onClose}>
      <div className={classes.modalContainer} onClick={(e) => e.stopPropagation()}>
        {/* X 버튼 */}
        <button className={classes.closeButton} onClick={onClose}>
          ×
        </button>
        
        {/* 플립 카드 */}
        <div className={`${classes.flipCard} ${isFlipped ? classes.flipped : ''}`} onClick={handleFlip}>
          {/* 앞면 - 스탯 정보 */}
          <div className={classes.cardFront}>
            <img src={player.image} alt={player.name} className={classes.playerImage} />
            <h5 className={classes.playerName}>{player.name}</h5>
            
            {/* 투수 스탯 */}
            {isPitcher && pitchingStats && (
              <div className={classes.statsSection}>
                <h6>평균자책점: {pitchingStats.era}</h6>
                <ul>
                  {pitchingStats.whip !== null && (
                    <li>WHIP: {pitchingStats.whip}</li>
                  )}
                  {pitchingStats.w !== null && pitchingStats.l !== null && (
                    <li>승패: {pitchingStats.w}승 {pitchingStats.l}패</li>
                  )}
                  {pitchingStats.so !== null && (
                    <li>탈삼진: {pitchingStats.so}</li>
                  )}
                  {pitchingStats.ip !== null && (
                    <li>이닝: {pitchingStats.ip}</li>
                  )}
                  
                </ul>
              </div>
            )}

            {/* 타자 스탯 */}
            {!isPitcher && battingStats && (
              <div className={classes.statsSection}>
                <h6>타율: {battingStats.avg}</h6>
                <ul>
                  {battingStats.ops !== null && (
                    <li>OPS: {battingStats.ops}</li>
                  )}
                  {battingStats.obp !== null && (
                    <li>출루율: {battingStats.obp}</li>
                  )}
                  {battingStats.hr !== null && (
                    <li>홈런: {battingStats.hr}</li>
                  )}
                  {battingStats.rbi !== null && (
                    <li>타점: {battingStats.rbi}</li>
                  )}
                  
                </ul>
              </div>
            )}

            {/* 스탯 정보가 없는 경우 */}
            {(!isPitcher && !battingStats) && (isPitcher && !pitchingStats) && (
              <div className={classes.noStats}>
                <p>스탯 정보가 없습니다.</p>
              </div>
            )}
          </div>

          {/* 뒷면 - 기본 정보 */}
          <div className={classes.cardBack}>
            <img src={player.image} alt={player.name} className={classes.playerImage} />
            <h5 className={classes.playerName}>{player.name}</h5>
            
            <div className={classes.basicInfo}>
              <h6>기본 정보</h6>
              <ul>
                <li>팀: {player.team || '정보 없음'}</li>
                <li>포지션: {player.position || '정보 없음'}</li>
                <li>입단년도: {formatJoinYear(player.joinYear)}</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
