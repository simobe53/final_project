import React from 'react';
import styles from './TeamMatches.module.scss';

function TeamMatches() {
  // 투수 데이터
  const pitcher1 = {
    name: "플레이어1",
    team: "팀명1",
    image: "★투수 이미지★",
    games: "n경기 n이닝",
    wins: "n승 n패",
    era: "3.35",
    whip: "1.14",
    battingAvg: "0.272",
    strikeouts: "78",
    walks: "9",
    war: "3.20",
    vsRecord: "1승 1패",
    vsEra: "1.50",
    vsOPS: "0.800",
    vsWhip: "1.42",
  };

  const pitcher2 = {
    name: "플레이어2",
    team: "팀명2",
    image: "★투수 이미지★",
    games: "n경기 n이닝",
    wins: "n승 n패",
    era: "4.48",
    whip: "1.29",
    battingAvg: "0.239",
    strikeouts: "82",
    walks: "35",
    war: "1.71",
    vsRecord: "1승 0패",
    vsEra: "2.35",
    vsOPS: "0.517",
    vsWhip: "0.78",
  };

  // 타석별 선수 데이터
  const battingOrder1 = [
    { name: "김1번", position: "1루수", battingOrder: 1, image: "⚾" },
    { name: "이2번", position: "2루수", battingOrder: 2, image: "🥎" },
    { name: "박3번", position: "3루수", battingOrder: 3, image: "🏏" },
    { name: "최4번", position: "외야수", battingOrder: 4, image: "⚾" },
    { name: "정5번", position: "포수", battingOrder: 5, image: "🥎" },
    { name: "강6번", position: "유격수", battingOrder: 6, image: "🏏" },
    { name: "윤7번", position: "외야수", battingOrder: 7, image: "⚾" },
    { name: "임8번", position: "외야수", battingOrder: 8, image: "🥎" },
    { name: "한9번", position: "지명타자", battingOrder: 9, image: "🏏" }
  ];

  const battingOrder2 = [
    { name: "송1번", position: "외야수", battingOrder: 1, image: "⚾" },
    { name: "조2번", position: "2루수", battingOrder: 2, image: "🥎" },
    { name: "서3번", position: "1루수", battingOrder: 3, image: "🏏" },
    { name: "김4번", position: "3루수", battingOrder: 4, image: "⚾" },
    { name: "이5번", position: "포수", battingOrder: 5, image: "🥎" },
    { name: "박6번", position: "유격수", battingOrder: 6, image: "🏏" },
    { name: "최7번", position: "외야수", battingOrder: 7, image: "⚾" },
    { name: "정8번", position: "외야수", battingOrder: 8, image: "🥎" },
    { name: "강9번", position: "지명타자", battingOrder: 9, image: "🏏" }
  ];
  
    return (
    <div className={styles.container}>
      {/* 투수 비교 섹션 제목 */}
      <h2 className={styles.title}>
        팀 라인업 & 투수 비교
      </h2>

      {/* 전체 라인업 컨테이너 */}
      <div className={styles.lineupContainer}>
        
        {/* 팀1 타석별 선수들 - 좌측 */}
        <div className={styles.lineupBox}>
          <h3 className={styles.lineupTitle}>
            {pitcher1.team} 라인업
          </h3>
          <div className={styles.lineupList}>
            {battingOrder1.map((player, index) => (
              <div key={index} className={styles.playerCard}>
                <div className={styles.playerImage}>
                  {player.image}
                </div>
                <div className={styles.playerInfo}>
                  <div className={styles.playerName}>{player.name}</div>
                  <div className={styles.playerPosition}>{player.position}</div>
                </div>
                <div className={styles.battingOrder}>
                  {player.battingOrder}번
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* 중앙 투수 비교 섹션 */}
        <div className={styles.pitcherSection}>
          {/* 투수 비교 헤더 */}
          <div className={styles.pitcherHeader}>
            {/* 팀1 투수 */}
            <div className={styles.pitcherBox}>
              <div className={styles.pitcherImage}>
                {pitcher1.image}
              </div>
              <h3 className={styles.pitcherName}>
                {pitcher1.name}
              </h3>
              <div className={styles.pitcherTeam}>
                {pitcher1.team}
              </div>
            </div>

            {/* VS */}
            <div className={styles.vsText}>
              VS
            </div>

            {/* 팀2 투수 */}
            <div className={styles.pitcherBox}>
              <div className={styles.pitcherImage}>
                {pitcher2.image}
              </div>
              <h3 className={styles.pitcherName}>
                {pitcher2.name}
              </h3>
              <div className={styles.pitcherTeam}>
                {pitcher2.team}
              </div>
            </div>
          </div>

          {/* 투수 통계 비교 */}
          <div className={styles.pitcherStats}>
            {/* 팀1 투수 통계 */}
            <div className={styles.pitcherStatsLeft}>
              <div className={styles.statItem}>{pitcher1.games}</div>
              <div className={styles.statItemHighlighted}>{pitcher1.wins}</div>
              <div className={styles.statItemHighlighted}>{pitcher1.era}</div>
              <div className={styles.statItemHighlighted}>{pitcher1.whip}</div>
              <div className={styles.statItem}>{pitcher1.battingAvg}</div>
              <div className={styles.statItem}>{pitcher1.strikeouts}</div>
              <div className={styles.statItem}>{pitcher1.walks}</div>
              <div className={styles.statItemHighlighted}>{pitcher1.war}</div>
              <div className={styles.statItem}>{pitcher1.vsRecord}</div>
              <div className={styles.statItem}>{pitcher1.vsEra}</div>
              <div className={styles.statItem}>{pitcher1.vsOPS}</div>
              <div className={styles.statItem}>{pitcher1.vsWhip}</div>
            </div>

            {/* 중앙 통계 라벨 */}
            <div className={styles.statsLabel}>
              <div className={styles.statItem}>경기이닝</div>
              <div className={styles.statItem}>승패</div>
              <div className={styles.statItem}>평균자책</div>
              <div className={styles.statItem}>WHIP</div>
              <div className={styles.statItem}>피안타율</div>
              <div className={styles.statItem}>탈삼진</div>
              <div className={styles.statItem}>볼넷</div>
              <div className={styles.statItem}>WAR</div>
              <div className={styles.statItem}>상대전적</div>
              <div className={styles.statItem}>상대 평균자책</div>
              <div className={styles.statItem}>상대 OOPS</div>
              <div className={styles.statItem}>상대 WHIP</div>
            </div>

            {/* 팀2 투수 통계 */}
            <div className={styles.pitcherStatsRight}>
              <div className={styles.statItem}>{pitcher2.games}</div>
              <div className={styles.statItem}>{pitcher2.wins}</div>
              <div className={styles.statItem}>{pitcher2.era}</div>
              <div className={styles.statItem}>{pitcher2.whip}</div>
              <div className={styles.statItemHighlighted}>{pitcher2.battingAvg}</div>
              <div className={styles.statItemHighlighted}>{pitcher2.strikeouts}</div>
              <div className={styles.statItemHighlighted}>{pitcher2.walks}</div>
              <div className={styles.statItem}>{pitcher2.war}</div>
              <div className={styles.statItem}>{pitcher2.vsRecord}</div>
              <div className={styles.statItem}>{pitcher2.vsEra}</div>
              <div className={styles.statItem}>{pitcher2.vsOPS}</div>
              <div className={styles.statItem}>{pitcher2.vsWhip}</div>
            </div>
          </div>
        </div>

        {/* 팀2 타석별 선수들 - 우측 */}
        <div className={styles.lineupBox}>
          <h3 className={styles.lineupTitle}>
            {pitcher2.team} 라인업
          </h3>
          <div className={styles.lineupList}>
            {battingOrder2.map((player, index) => (
              <div key={index} className={styles.playerCard}>
                <div className={styles.playerImage}>
                  {player.image}
                </div>
                <div className={styles.playerInfo}>
                  <div className={styles.playerName}>{player.name}</div>
                  <div className={styles.playerPosition}>{player.position}</div>
                </div>
                <div className={styles.battingOrder}>
                  {player.battingOrder}번
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
  }
  
  export default TeamMatches