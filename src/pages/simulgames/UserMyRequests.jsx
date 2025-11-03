import { useEffect } from "react";
import { getDate } from "/components";
import { useInit } from "/context/InitContext";
import { useSimulations } from "/context/SimulationsContext";
import TeamLogo from "/components/TeamLogo";
import { getUserSimulationRequests } from "/services/simulations";

export default function UserMyRequests({ hidden }) {
    const { teams, players } = useInit();
    const { myRequests, fetchModel } = useSimulations();

    // 팀 ID로 팀 정보 찾기 (UserRequestGame.jsx와 동일한 방식)
    const getTeamById = (teamId) => teams.filter(({ id }) => id === teamId)[0];

    // 선수 ID를 이름으로 변환하는 함수
    const getPlayerName = (playerId) => {
        if (!playerId || !players) return '미정';
        const player = players[playerId];
        return player ? player.playerName : '미정';
    };

    // 라인업 정보 파싱
    const parseLineup = (lineupString) => {
        try {
            return JSON.parse(lineupString);
        } catch (error) {
            console.error('라인업 파싱 오류:', error);
            return {};
        }
    };

    // 상태 배지 컴포넌트
    const getStatusBadge = (status) => {
        const statusConfig = {
            'PENDING': { text: '대기중', class: 'bg-warning text-dark' },
            'APPROVED': { text: '승인됨', class: 'bg-success text-white' },
            'REFUSE': { text: '거절됨', class: 'bg-danger text-white' }
        };
        
        const config = statusConfig[status] || { text: status, class: 'bg-secondary text-white' };
        
        return (
            <span className={`badge ${config.class} px-2 py-1`}>
                {config.text}
            </span>
        );
    };

    const fetchMyRequests = async () => {
        try {
            const requests = await getUserSimulationRequests();
            fetchModel({ myRequests: requests })
        } catch (error) {
            console.error('내 요청 조회 실패:', error);
        }
    };

    // 내 요청 목록 조회
    useEffect(() => {
        if (hidden) return;
        fetchMyRequests();
    }, [hidden]);

    useEffect(() => { fetchMyRequests(); }, [])

    if (myRequests.length === 0) {
        return (
            <div className="p-3 text-center">
                <small className="text-muted">요청한 시뮬레이션이 없습니다.</small>
            </div>
        );
    }

    return (
        <div hidden={hidden} className="overflow-y-auto">
            {myRequests.map(request => {
                const homeTeam = getTeamById(request.hometeam);
                const awayTeam = getTeamById(request.awayteam);
                const homeLineupData = parseLineup(request.homeLineup);
                const awayLineupData = parseLineup(request.awayLineup);

                return (
                    <div key={request.id} className="border-bottom border-gray p-3">
                        <div className="d-flex align-items-center justify-content-between">
                            {/* 팀 정보 */}
                            <div className="d-flex align-items-center gap-3">
                                <div className="d-flex align-items-center gap-2">
                                    <TeamLogo name={homeTeam.idKey} zoom={0.7} />
                                    <span className="fw-bold">{homeTeam.name}</span>
                                </div>
                                <span className="text-muted">VS</span>
                                <div className="d-flex align-items-center gap-2">
                                    <TeamLogo name={awayTeam.idKey} zoom={0.7} />
                                    <span className="fw-bold">{awayTeam.name}</span>
                                </div>
                            </div>

                            {/* 상태 배지 */}
                            <div>
                                {getStatusBadge(request.status)}
                            </div>
                        </div>

                        {/* 요청 정보 */}
                        <div className="mt-2 text-muted small">
                            <span>요청일: {getDate(request.requestAt)}</span>
                            {request.scheduledAt && (
                                <>
                                    <span className="mx-2">|</span>
                                    <span>예정일: {getDate(request.scheduledAt)}</span>
                                </>
                            )}
                        </div>

                        {/* 라인업 미리보기 */}
                        <div className="mt-2">
                            {/* 홈팀 라인업 */}
                            <div className="d-flex justify-content-start align-items-center gap-2 mb-1">
                                <small className="text-muted">
                                    <strong>홈팀 선발:</strong> {homeLineupData.pitcher ? getPlayerName(homeLineupData.pitcher) : '미정'}
                                </small>
                                <div className="border border-radius-12 pt-1 pb-1 p-2" style={{ background: 'rgba(0,0,0,0.05)', fontSize: 11 }}>
                                    {[1,2,3,4,5,6,7,8,9].map(i => {
                                        const batterId = homeLineupData[`batting${i}`];
                                        return (
                                            <small key={i}>
                                                {i} {batterId ? getPlayerName(batterId) : '미정'}{' '}
                                            </small>
                                        );
                                    })}
                                </div>
                            </div>
                            
                            {/* 원정팀 라인업 */}
                            <div className="d-flex justify-content-start align-items-center gap-2">
                                <small className="text-muted">
                                    <strong>원정팀 선발:</strong> {awayLineupData.pitcher ? getPlayerName(awayLineupData.pitcher) : '미정'}
                                </small>
                                <div className="border border-radius-12 pt-1 pb-1 p-2" style={{ background: 'rgba(0,0,0,0.05)', fontSize: 11 }}>
                                    {[1,2,3,4,5,6,7,8,9].map(i => {
                                        const batterId = awayLineupData[`batting${i}`];
                                        return (
                                            <small key={i}>
                                                {i} {batterId ? getPlayerName(batterId) : '미정'}{' '}
                                            </small>
                                        );
                                    })}
                                </div>
                            </div>
                        </div>

                        {/* 관리자 코멘트 */}
                        {request.adminComment && (
                            <div className="mt-2 p-2 bg-light rounded">
                                <small className="text-muted">관리자 코멘트: {request.adminComment}</small>
                            </div>
                        )}
                    </div>
                );
            })}
        </div>
    );
}
