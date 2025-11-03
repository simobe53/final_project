import React, { useState, useEffect, Fragment } from "react";
import axios from "/config/axios";
import styles from './LineupSelect.module.scss';
import TeamLogo from '/components/TeamLogo';
import PlayerModal from './PlayerModal';

export default function LineupSelect({ team, lineup, onLineupChange }) {
    const [selectedPitcher, setSelectedPitcher] = useState(null);
    const [selectedBatters, setSelectedBatters] = useState({});

    // ⭐️ 추가: 팀 선수 데이터를 상태로 관리
    const [teamPlayers, setTeamPlayers] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [currentTeamId, setCurrentTeamId] = useState(null); // 현재 팀 ID 추적
    
    // ⭐️ PlayerModal 상태 관리
    const [modalPlayer, setModalPlayer] = useState(null);
    const [isModalOpen, setIsModalOpen] = useState(false);

    // ⭐️ API 호출로 실제 선수 데이터 가져오기
    useEffect(() => {
        if (!team) return;

        // 💡 팀이 바뀌었는지 체크하여 라인업 리셋
        const teamChanged = currentTeamId !== team.id;
        
        if (teamChanged) {
            // 팀이 바뀔 때만 라인업 리셋
            setSelectedPitcher(null);
            setSelectedBatters({});
            setCurrentTeamId(team.id);
        }

        setIsLoading(true);
        setError(null);
        setTeamPlayers(null);

        const fetchPlayers = async () => {
            try {
                const response = await axios.get(`/api/players/teams/${team.idKey}/players`);
                
                // API 응답 구조에 맞게 변환 (투수/타자 ID 키 추가)

                // 선발투수 12명, 타자 15명으로 제한
                const transformedData = {
                    pitchers: response.data.pitchers
                        .slice(0, 12)  // 선발투수 최대 12명

                        .map((player) => ({
                            id: (player.pno),
                            name: player.playerName,
                            image: player.imgUrl,
                            position: player.position,
                            team: player.teamName,
                            playerType: player.playerType,
                            battingStats: player.battingStats,
                            pitchingStats: player.pitchingStats,
                            joinYear: player.joinYear
                        })),
                    batters: response.data.batters
                        .slice(0, 15)  // 타자 최대 15명

                        .map((player) => ({
                            id: (player.pno),

                            name: player.playerName,
                            image: player.imgUrl,
                            position: player.position,
                            team: player.teamName,
                            playerType: player.playerType,
                            battingStats: player.battingStats,
                            pitchingStats: player.pitchingStats,
                            joinYear: player.joinYear
                        }))

                };
                
                setTeamPlayers(transformedData);
                
                // 🔧 팀이 바뀌지 않았을 때만 라인업 복원
                if (!teamChanged && lineup && Object.keys(lineup).length > 0) {
            if (lineup.pitcher) {
                setSelectedPitcher(lineup.pitcher);
            }
            if (Object.keys(lineup).length > 1) {
                const batters = { ...lineup };
                delete batters.pitcher;
                setSelectedBatters(batters);
            }
        }
                
            } catch (err) {
                console.error("선수 데이터 로드 실패:", err);
                setError("선수 목록을 불러오지 못했습니다.");
            } finally {
                setIsLoading(false);
            }
        };

        fetchPlayers();
    }, [team]); // lineup을 다시 의존성에 포함

    // 핸들러 함수들 (이제 teamPlayers 상태를 사용)
    const handlePitcherSelect = (pitcherId) => {
        const pitcher = teamPlayers?.pitchers?.find(p => p.id === pitcherId);
        setSelectedPitcher(pitcher);
        updateLineup(pitcher, selectedBatters);
    };

    const handleQuickBatterSelect = (batter) => {
        // 이미 선택된 타자인지 확인
        const existingPosition = Object.keys(selectedBatters).find(pos => 
            selectedBatters[pos]?.id === batter.id
        );
        
        if (existingPosition) {
            // 이미 선택된 선수면 제거
            const newBatters = { ...selectedBatters };
            delete newBatters[existingPosition];
            setSelectedBatters(newBatters);
            updateLineup(selectedPitcher, newBatters);
        } else {
            // 사용 가능한 첫 번째 빈 순서 찾기 (1번 타자부터)
            let nextPosition = null;
            for (let i = 1; i <= 9; i++) {
                if (!selectedBatters[i]) {
                    nextPosition = i;
                    break;
                }
            }
            
            if (nextPosition) {
                const newBatters = { ...selectedBatters, [nextPosition]: batter };
                setSelectedBatters(newBatters);
                updateLineup(selectedPitcher, newBatters);
            }
        }
    };

    const updateLineup = (pitcher, batters) => {
        const newLineup = { 
            pitcher: pitcher, 
            ...batters 
        };

        onLineupChange(newLineup);
    };

    // ⭐️ PlayerModal 관련 함수들
    const openPlayerModal = (player) => {
        setModalPlayer(player);
        setIsModalOpen(true);
    };

    const closePlayerModal = () => {
        setModalPlayer(null);
        setIsModalOpen(false);
    };


    // 로딩 및 에러 처리
    if (!team) {
        return <div className="text-center text-gray">팀을 먼저 선택해주세요.</div>;
    }

    if (isLoading) {
        return <div className="text-center text-primary">선수 명단을 불러오는 중입니다...</div>;
    }
    
    if (error) {
        return <div className="text-center text-danger">{error}</div>;
    }

    if (!teamPlayers) {
        return <div className="text-center text-gray">해당 팀의 선수 데이터가 없습니다.</div>;
    }

    const positions = [1, 2, 3, 4, 5, 6, 7, 8, 9];

    return (
        <Fragment>
        {/* 1. 전체 컨테이너 클래스 변경 */}
        <div className={styles.lineupSelectContainer}> 
            {/* 선택된 라인업 요약 */}
            <div className={`mt-3 border border-gray border-radius-12 ${styles.lineupSummary}`}>
                <div className="d-flex p-2 gap-20 border-bottom align-items-center border-gray">
                    <TeamLogo name={team?.idKey} />
                    <span className="text-center h6 m-0">선택된 라인업</span> 
                </div>
                {selectedPitcher ? (
                    <div className="mb-2 p-3 d-flex align-items-center justify-content-center gap-2">
                        <img src={selectedPitcher.image} alt={selectedPitcher.name} className={styles.playerImage} />
                        <small className="text-gray text-center">투수: </small>
                        <small className="fw-bold">{selectedPitcher.name}</small>
                    </div>
                ) : (
                    <div className="d-flex p-3 flex-wrap justify-content-center gap-2">
                        <small className="text-gray text-center mb-2">아직 선발 투수를 선택하지 않았습니다.</small>
                    </div>
                )}
                <div className="d-flex pb-3 flex-wrap justify-content-center gap-2">
                    {positions.map(position => {
                        const batter = selectedBatters[position];
                        return batter ? (
                            <div key={position} className={styles.batterBadge + ' badge bg-secondary'}> 
                                <img src={batter.image} alt={batter.name} className={styles.playerImage} />
                                <span>{position}번 {batter.name}</span>
                        </div>
                        ) : null;
                    })}
                    {Object.keys(selectedBatters).length === 0 && (
                        <small className="p-3 text-gray text-center mb-2">아직 타자를 선택하지 않았습니다.</small>
                    )}
                </div>
            </div>

            {/* 투수 선택 */}
            <div className={styles.pitchersSection}>
                <h6 className={styles.playerSection}>선발 투수</h6>
                <div className={styles.pitchersContainer}>
                    {teamPlayers.pitchers?.map(pitcher => {
                        const isSelected = selectedPitcher?.id === pitcher.id;
                        return (
                            <button
                                key={pitcher.id}
                                type="button"
                                className={`btn border-radius-12 p-2 d-flex align-items-center gap-2 ${styles.pitcherButton} ${ 
                                    isSelected ? 'btn-primary' : 'btn-outline-secondary' 
                                }`}
                                onClick={() => handlePitcherSelect(pitcher.id)}
                                onMouseDown={() => {
                                    // 길게 누르기 시작
                                    pitcher.longPressTimer = setTimeout(() => {
                                        openPlayerModal(pitcher);
                                    }, 500); // 0.5초 후 모달 열기
                                }}
                                onTouchStart={() => {
                                    // 터치 시작 (모바일)
                                    pitcher.longPressTimer = setTimeout(() => {
                                        openPlayerModal(pitcher);
                                    }, 500);
                                }}
                                title="클릭: 선택 | 길게 누르기: 상세 정보"
                                onMouseUp={() => {
                                    // 마우스 버튼을 놓으면 타이머 취소
                                    if (pitcher.longPressTimer) {
                                        clearTimeout(pitcher.longPressTimer);
                                        pitcher.longPressTimer = null;
                                    }
                                }}
                                onTouchEnd={() => {
                                    // 터치 종료 (모바일)
                                    if (pitcher.longPressTimer) {
                                        clearTimeout(pitcher.longPressTimer);
                                        pitcher.longPressTimer = null;
                                    }
                                }}
                                onMouseLeave={() => {
                                    // 마우스가 버튼을 벗어나면 타이머 취소
                                    if (pitcher.longPressTimer) {
                                        clearTimeout(pitcher.longPressTimer);
                                        pitcher.longPressTimer = null;
                                    }
                                }}
                            >
                                <img 
                                    src={pitcher.image} 
                                    alt={pitcher.name}
                                    className={styles.playerImage}
                                />
                                <small className="text-nowrap">{pitcher.name}</small>
                            </button>
                        );
                    })}
                </div>
            </div>

            {/* 타자 선택 */}
            <div>
                <h6 className={styles.playerSection}>타순</h6>
                <div className={styles.battersContainer}>
                    {teamPlayers.batters?.map(batter => {
                        const isSelected = Object.values(selectedBatters).some(selectedBatter => selectedBatter?.id === batter.id);
                        return (
                            <button
                                key={batter.id}
                                type="button"
                                className={`btn border-radius-12 p-2 d-flex align-items-center gap-2 ${styles.playerButton} ${ 
                                    isSelected ? 'btn-primary' : 'btn-outline-secondary' 
                                }`}
                                onClick={() => handleQuickBatterSelect(batter)}
                                onMouseDown={() => {
                                    // 길게 누르기 시작
                                    batter.longPressTimer = setTimeout(() => {
                                        openPlayerModal(batter);
                                    }, 500); // 0.5초 후 모달 열기
                                }}
                                onTouchStart={() => {
                                    // 터치 시작 (모바일)
                                    batter.longPressTimer = setTimeout(() => {
                                        openPlayerModal(batter);
                                    }, 500);
                                }}
                                title="클릭: 선택 | 길게 누르기: 상세 정보"
                                onMouseUp={() => {
                                    // 마우스 버튼을 놓으면 타이머 취소
                                    if (batter.longPressTimer) {
                                        clearTimeout(batter.longPressTimer);
                                        batter.longPressTimer = null;
                                    }
                                }}
                                onTouchEnd={() => {
                                    // 터치 종료 (모바일)
                                    if (batter.longPressTimer) {
                                        clearTimeout(batter.longPressTimer);
                                        batter.longPressTimer = null;
                                    }
                                }}
                                onMouseLeave={() => {
                                    // 마우스가 버튼을 벗어나면 타이머 취소
                                    if (batter.longPressTimer) {
                                        clearTimeout(batter.longPressTimer);
                                        batter.longPressTimer = null;
                                    }
                                }}
                            >
                                    <img 
                                        src={batter.image} 
                                        alt={batter.name}
                                    className={styles.playerImage}
                                />
                                <small className="text-nowrap">{batter.name}</small>
                            </button>
                        );
                        })}
                    </div>
                </div>

        </div>

        {/* ⭐️ PlayerModal 추가 */}
        {isModalOpen && (
            <PlayerModal 
                player={modalPlayer} 
                onClose={closePlayerModal} 
            />
        )}
        </Fragment>
    );
}
