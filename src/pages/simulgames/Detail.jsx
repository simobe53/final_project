import { useOutletContext, useParams } from "react-router-dom";
import { useSimulations } from "/context/SimulationsContext";
import { useInit } from "/context/InitContext";
import { getSimulationAtBats, getGameState, getSimulation } from "/services/simulations";
import { useState, useEffect, useRef, useCallback, useMemo } from "react";
import { OverlayPage } from "/components";
import { useCheckTime } from "/components/hooks/useCheckTime";
import PlayGround from "./components/PlayGround";
import Scoreboard from "./components/Scoreboard";
import SimulationTabs from "./components/SimulationTabs";
import Chat from "./components/Chat";


export default function Detail() {
    const { id } = useParams();
    const { teams } = useInit();
    const { expired, startTimer, stopTimer } = useCheckTime();
    const { simulations } = useSimulations();
    const [atBats, setAtBats] = useState([]);
    const [loading, setLoading] = useState(true);
    const [gameState, setGameState] = useState(null);
    const [isGameStarted, setIsGameStarted] = useState(false);
    const intervalRef = useRef(null);

    const simulationId = parseInt(id);
    const [contextSimulation = {}] = useMemo(() => simulations.filter(({ id }) => simulationId === id), [simulations, simulationId]);
    const [simulation, setSimulation] = useState({});
    const [isGameFinished, setIsGameFinished] = useState(simulation.isFinished);
    const { selectedTeam: teamId = simulation?.hometeam } = useOutletContext(); // 유저가 응원한다고 누르고 들어온 팀 (디폴트값 : 홈팀)

    // checkAndStartPolling 함수는 백그라운드 스케줄러 연동으로 더 이상 필요하지 않음

    const startRealtimePolling = useCallback(() => {
        if (intervalRef.current) {
            clearInterval(intervalRef.current);
        }

        // 실시간 폴링 시작 - 백그라운드 스케줄러가 진행한 타석들을 감지

        intervalRef.current = setInterval(async () => {
            try {
                // 🆕 백그라운드 스케줄러가 진행한 새로운 타석들 확인
                const atBatsData = await getSimulationAtBats(simulationId);
                
                // 새로운 타석이 있는지 확인
                setAtBats(prev => {
                    if (atBatsData.length > prev.length) {
                        return atBatsData; // 새로운 타석 데이터로 업데이트
                    }
                    return prev; // 변경 없음
                });

                // 게임 상태 확인
                const gameStateResult = await getGameState(simulationId);
                if (gameStateResult.status === 'success') {
                    setGameState(gameStateResult.gameState);
                    
                    if (gameStateResult.gameState.gameStatus === 'FINISHED') {
                        setTimeout(() => setIsGameFinished(true), 3000);
                        clearInterval(intervalRef.current);
                        intervalRef.current = null;
                    }
                }

            } catch (error) {
                // 오류 발생 시 조용히 처리 (백그라운드 폴링이므로)
            }
        }, 5000); // 5초 간격으로 변경 (백그라운드 스케줄러 10초 간격과 연동)
    }, [simulationId]);
    
    useEffect(() => {
        console.log(expired);
        if (expired) {
            startRealtimePolling();
            stopTimer();
        }
    }, [expired])

    useEffect(() => {
        return () => {
            stopTimer()
            if (intervalRef.current) {
                clearInterval(intervalRef.current);
            }
        };
    }, []);

    useEffect(() => {
        const fetchExistingData = async () => {
            try {
                setLoading(true);

                // 시뮬레이션 데이터 직접 조회 (Context에 없을 경우)
                if (!simulation || Object.keys(simulation).length === 0) {
                    try {
                        const simulationData = await getSimulation(simulationId);
                        setSimulation(simulationData);
                    } catch (error) {
                        // 시뮬레이션 데이터 조회 실패 시 조용히 처리
                    }
                }

                // 게임 상태 확인
                const gameStateResult = await getGameState(simulationId);
                if (gameStateResult.status === 'success') {
                    setGameState(gameStateResult.gameState);
                    setIsGameStarted(gameStateResult.gameState.gameStatus === 'PLAYING' || gameStateResult.gameState.gameStatus === 'FINISHED');
                    setIsGameFinished(gameStateResult.gameState.gameStatus === 'FINISHED');

                    // 백그라운드 스케줄러 연동: 진행 중인 게임 감지
                    if (gameStateResult.gameState.gameStatus === 'PLAYING') {
                        // 기존 타석 데이터 로드
                        const atBatsData = await getSimulationAtBats(simulationId);
                        setAtBats(atBatsData);
                        
                        // 실시간 폴링 시작 (백그라운드 스케줄러가 진행하는 타석들을 감지)
                        startRealtimePolling();
                        
                    } else if (gameStateResult.gameState.gameStatus === 'FINISHED') {
                        // 완료된 게임의 모든 타석 데이터 로드
                        const atBatsData = await getSimulationAtBats(simulationId);
                        setAtBats(atBatsData);
                        setIsGameFinished(true);
                    }
                } else {
                    startTimer({ date: simulation.showAt })
                }

            } catch (error) {
                // 데이터 로드 실패 시 조용히 처리
            } finally {
                setLoading(false);
            }
        };

        if (simulationId) {
            fetchExistingData();
        }
    }, [simulationId, startRealtimePolling, simulation]);

    useEffect(() => {
        if (simulation?.id !== id) {
            getSimulation(id).then(data => {
                const { homeLineup, awayLineup } = data;
                data.homeLineup = JSON.parse(homeLineup); 
                data.awayLineup = JSON.parse(awayLineup); 
                setSimulation(data);
            })
        }
    }, [id])

    const { homeScores, awayScores } = useMemo(() => {
        const homeScores = [0, 0, 0, 0, 0, 0, 0, 0, 0];
        const awayScores = [0, 0, 0, 0, 0, 0, 0, 0, 0];

        atBats.forEach(atBat => {
            const inningHalf = atBat.inningHalf || '';
            const inningNum = parseInt(inningHalf.replace(/초|말/gi, "")) || 0;
            const half = inningHalf.slice(inningHalf.length-1, inningHalf.length); // '초' or '말'
            const runs = atBat.rbi || 0;

            if (inningNum >= 1 && inningNum <= 9) {
                if (half === '초') {
                    awayScores[inningNum - 1] += runs;
                } else if (half === '말') {
                    homeScores[inningNum - 1] += runs;
                }
            }
            else if (inningNum > 9) {
                if (half === '초') {
                    if (!awayScores[inningNum - 1]) awayScores[inningNum - 1] = 0;
                    awayScores[inningNum - 1] += runs;
                } else if (half === '말') {
                    if (!homeScores[inningNum - 1]) homeScores[inningNum - 1] = 0;
                    homeScores[inningNum - 1] += runs;
                }
            }
        });


        return { homeScores, awayScores };
    }, [atBats]);

    const calculateHits = useCallback((isHome) => {
        return atBats.filter(atBat => {
            const inningHalf = atBat.inningHalf || '';
            const half = inningHalf.slice(1);
            const isHomeAt = (half === '말');
            const isHit = ['single', 'double', 'triple', 'home_run'].includes(atBat.result);
            return isHomeAt === isHome && isHit;
        }).length;
    }, [atBats]);

    const [hometeam] = teams.filter(({ id }) => id === simulation?.hometeam);
    const [awayteam] = teams.filter(({ id }) => id === simulation?.awayteam);

    const title = `${hometeam?.name} vs ${awayteam?.name}`;

    const gameInfo = useMemo(() => gameState ? {
        homeTeam: {
            ...hometeam,
            scores: homeScores,
            totalRuns: gameState.homeScore || 0,
            hits: calculateHits(true),
            errors: 0
        },
        awayTeam: {
            ...awayteam,
            scores: awayScores,
            totalRuns: gameState.awayScore || 0,
            hits: calculateHits(false),
            errors: 0
        },
        isTopInning: gameState.half === "초",
        count: {
            balls: 0,
            strikes: 0,
            outs: gameState.outs || 0
        },
        outs: gameState.outs || 0,
        currentInning: gameState.inning || 1,
        currentBatter: gameState.nextBatterName
            ? `${gameState.nextBatterName}${gameState.nextBatterAvg ? ` (${gameState.nextBatterAvg.toFixed(3)})` : ''}`
            : '타석 대기 중',
        currentPitcher: gameState.currentPitcherName
            ? `${gameState.currentPitcherName}${gameState.currentPitcherERA ? ` (${gameState.currentPitcherERA.toFixed(2)})` : ''}`
            : '투구 대기 중',
        gameStatus: gameState.gameStatus,
        winner: gameState.winner
    } : {
        homeTeam: {
            name: simulation.homeTeam,
            fullName: simulation.homeTeam,
            scores: [0, 0, 0, 0, 0, 0, 0, 0, 0],
            totalRuns: 0,
            hits: 0,
            errors: 0
        },
        awayTeam: {
            name: simulation.awayTeam,
            fullName: simulation.awayTeam,
            scores: [0, 0, 0, 0, 0, 0, 0, 0, 0],
            totalRuns: 0,
            hits: 0,
            errors: 0
        },
        isTopInning: undefined,
        count: { balls: 0, strikes: 0, outs: 0 },
        currentInning: 1,
        currentBatter: '게임 시작 전',
        currentPitcher: '게임 시작 전',
        gameStatus: 'READY'
    }, [gameState, simulation, homeScores, awayScores, calculateHits, hometeam, awayteam]);

    if (!simulation) return <div className="p-4 text-center">시뮬레이션을 찾을 수 없습니다.</div>;

    if (loading) return <div className="p-4 text-center">타석 데이터를 불러오는 중...</div>;

    const currentStatus = atBats[atBats.length - 1];

    return <>
        <OverlayPage title={title}>

            {/* 게임 종료 메시지 */}
            {isGameFinished && (
                <div className="mb-0 text-white text-center p-3" style={{ background: '#222' }}>
                    <small>게임이 종료되었습니다!</small><br/>
                    <b className="point">
                    {gameState?.winner === 'HOME' && ` ${hometeam.name} 승리! (${gameState.homeScore}-${gameState.awayScore})`}
                    {gameState?.winner === 'AWAY' && ` ${awayteam.name} 승리! (${gameState.awayScore}-${gameState.homeScore})`}
                    {gameState?.winner === 'TIE' && ` 무승부! (${gameState.homeScore}-${gameState.awayScore})`}
                    </b>
                </div>
            )}

            {/* 경기장 : lineup : 수비 라인업 */}
            <PlayGround gameStatus={gameInfo.gameStatus} currentStatus={currentStatus} lineup={currentStatus?.inningHalf.includes("말") ? simulation.awayLineup : simulation.homeLineup} batterLineup={currentStatus?.inningHalf.includes("말") ? simulation.homeLineup : simulation.awayLineup} />

            {/* 스코어보드 */}
            <Scoreboard gameInfo={gameInfo} homeTeam={hometeam} awayTeam={awayteam} />

            {/* 실시간 중계 */}
            <SimulationTabs simulationId={simulationId} atBats={atBats} homeTeam={hometeam} awayTeam={awayteam} />
            {loading && (
                <div className="text-center p-4">
                    <div className="spinner-border text-primary" role="status">
                        <span className="visually-hidden">로딩 중...</span>
                    </div>
                    <p className="mt-2">처리 중...</p>
                </div>
            )}

            <Chat team={teamId == simulation.hometeam ? hometeam : awayteam} title={title} isHome={teamId == simulation.hometeam} />
        </OverlayPage>
    </>
}