import { useState } from "react";
import { getDate } from "/components";
import { useInit } from "/context/InitContext";
import { useSimulations } from "/context/SimulationsContext";
import Modal from "/components/Modal";
import TeamLogo from "/components/TeamLogo";
import { approveUserSimulationRequest, rejectUserSimulationRequest } from "/services/simulations";

export default function UserRequestGame({ 
    id, user, hometeam, awayteam, homeLineup, awayLineup, 
    status, requestAt, adminComment, onStatusChange 
}) {
    const { teams, players } = useInit();
    const { date, simulations, fetchModel } = useSimulations();
    const [showModal, setShowModal] = useState(false);
    const [action, setAction] = useState('');
    const [comment, setComment] = useState('');
    const [scheduledAt, setScheduledAt] = useState('');

    // 팀 ID로 팀 정보 찾기 (Game.jsx와 동일한 방식)
    const getTeamById = (teamId) => teams.filter(({ id }) => id === teamId)[0];

    const homeTeam = getTeamById(hometeam);
    const awayTeam = getTeamById(awayteam);

    // 라인업 정보 파싱
    const parseLineup = (lineupString) => {
        try {
            return JSON.parse(lineupString);
        } catch (error) {
            console.error('라인업 파싱 오류:', error);
            return {};
        }
    };

    const homeLineupData = parseLineup(homeLineup);
    const awayLineupData = parseLineup(awayLineup);

    // 선수 ID를 이름으로 변환하는 함수
    const getPlayerName = (playerId) => {
        if (!playerId || !players) return '미정';
        const player = players[playerId];
        return player ? player.playerName : '미정';
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

    const handleAction = (actionType) => {
        setAction(actionType);
        setComment('');
        setScheduledAt('');
        setShowModal(true);
    };

    const handleSubmit = async () => {
        if (!comment.trim()) {
            alert('코멘트를 입력해주세요.');
            return;
        }

        if (action === 'approve' && !scheduledAt) {
            alert('시뮬레이션 예정 시간을 선택해주세요.');
            return;
        }

        try {
            if (action === 'approve') {
                const result = await approveUserSimulationRequest(id, comment, scheduledAt);
                alert('요청이 승인되었습니다!');

                try {
                    result.homeLineup = JSON.parse(result.homeLineup);
                    result.awayLineup = JSON.parse(result.awayLineup);
                } catch(e) {
                    console.log(e)
                }
                // 승인된 시뮬레이션을 Context에 추가
                if (result.showAt.startsWith(date)) fetchModel({ simulations: [...simulations, result] });

            } else if (action === 'reject') {
                await rejectUserSimulationRequest(id, comment);
                alert('요청이 거절되었습니다.');
            }

            setShowModal(false);
            onStatusChange(); // 목록 새로고침
        } catch (error) {
            console.error('처리 실패:', error);
            alert('처리에 실패했습니다.');
        }
    };

    return (
        <>
            <div className="border-bottom border-gray p-3">
                <div className="d-flex align-items-center justify-content-between">
                    {/* 팀 정보 */}
                    <div className="d-flex align-items-center gap-3">
                        <div className="d-flex align-items-center gap-2">
                            <TeamLogo name={homeTeam.idKey} zoom={0.8} />
                            <span className="fw-bold">{homeTeam.name}</span>
                        </div>
                        <span className="text-muted">VS</span>
                        <div className="d-flex align-items-center gap-2">
                            <TeamLogo name={awayTeam.idKey} zoom={0.8} />
                            <span className="fw-bold">{awayTeam.name}</span>
                        </div>
                    </div>

                    {/* 상태 배지 */}
                    <div>
                        {getStatusBadge(status)}
                    </div>
                </div>

                {/* 요청 정보 */}
                <div className="mt-2 d-flex justify-content-between align-items-center">
                    <div className="text-muted small">
                        <span>요청자: {user.name}</span>
                        <span className="mx-2">|</span>
                        <span>요청일: {getDate(requestAt)}</span>
                    </div>

                    {/* 액션 버튼 */}
                    {status === 'PENDING' && (
                        <div className="d-flex gap-2">
                            <button 
                                className="btn btn-success btn-sm"
                                onClick={() => handleAction('approve')}
                            >
                                승인
                            </button>
                            <button 
                                className="btn btn-danger btn-sm"
                                onClick={() => handleAction('reject')}
                            >
                                거절
                            </button>
                        </div>
                    )}
                </div>

                {/* 라인업 미리보기 */}
                <div className="mt-2">
                    {/* 홈팀 라인업 */}
                    <div className="d-flex justify-content-start align-items-center gap-2 mb-2">
                        <small className="text-muted">
                            <strong>홈팀 선발:</strong> {homeLineupData.pitcher ? getPlayerName(homeLineupData.pitcher) : '미정'}
                        </small>
                        <div className="border border-radius-12 pt-1 pb-1 p-2" style={{ background: 'rgba(0,0,0,0.05)', fontSize: 12 }}>
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
                        <div className="border border-radius-12 pt-1 pb-1 p-2" style={{ background: 'rgba(0,0,0,0.05)', fontSize: 12 }}>
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
                {adminComment && (
                    <div className="mt-2 p-2 bg-light rounded">
                        <small className="text-muted">관리자 코멘트: {adminComment}</small>
                    </div>
                )}
            </div>

            {/* 승인/거절 모달 */}
            {showModal && (
                <Modal title={action === 'approve' ? '요청 승인' : '요청 거절'} onClose={() => setShowModal(false)}>
                    <div style={{ width: 300 }}>
                        <div className="mb-3">
                            <label className="form-label">코멘트</label>
                            <textarea
                                className="form-control"
                                rows="3"
                                placeholder="코멘트를 입력하세요"
                                value={comment}
                                onChange={(e) => setComment(e.target.value)}
                            />
                        </div>

                        {action === 'approve' && (
                            <div className="mb-3">
                                <label className="form-label">시뮬레이션 예정 시간</label>
                                <input
                                    type="datetime-local"
                                    className="form-control"
                                    value={scheduledAt}
                                    onChange={(e) => setScheduledAt(e.target.value)}
                                />
                            </div>
                        )}

                        <div className="d-flex justify-content-end gap-2">
                            <button 
                                className="btn btn-secondary"
                                onClick={() => setShowModal(false)}
                            >
                                취소
                            </button>
                            <button 
                                className={`btn ${action === 'approve' ? 'btn-success' : 'btn-danger'}`}
                                onClick={handleSubmit}
                            >
                                {action === 'approve' ? '승인' : '거절'}
                            </button>
                        </div>
                    </div>
                </Modal>
            )}
        </>
    );
}
