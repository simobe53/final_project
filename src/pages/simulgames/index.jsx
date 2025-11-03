import { useState, useEffect } from "react";
import { Link, Outlet } from "react-router-dom";
import { useSimulations } from "/context/SimulationsContext";
import { useAuth } from "/context/AuthContext";
import Calendar from "/components/Calendar";
import Empty from "/components/Empty";
import { URL } from '/config/constants';
import Game from "./Game";
import UserRequestGame from "./UserRequestGame";
import UserMyRequests from "./UserMyRequests";
import { getUserSimulationRequests } from "/services/simulations";

export default function SimulateGames() {
    const { auth } = useAuth();
    const { date, simulations = [], myRequests = [], userRequests = [], setDate, fetchModel } = useSimulations();
    const [team, setTeam] = useState(null);
    const [showRequests, setShowRequests] = useState(false);
    const isAdmin = auth?.role === 'ADMIN';
    const isUser = auth?.role === 'USER';

    // 관리자인 경우 사용자 요청 목록 조회
    useEffect(() => {
        if (isAdmin) {
            fetchUserRequests();
        }
    }, [isAdmin]);

    const fetchUserRequests = async () => {
        try {
            const requests = await getUserSimulationRequests({ status: 'PENDING' });
            fetchModel({ userRequests: requests });
        } catch (error) {
            console.error('사용자 요청 조회 실패:', error);
        }
    };

    useEffect(() => {
        if (isUser && !myRequests.length) setShowRequests(false);
    }, [myRequests, isUser])

    useEffect(() => {
        if (isAdmin && !userRequests.length) setShowRequests(false);
    }, [userRequests, isAdmin])

    
    /** 스트리밍 시간 순 정렬 */
    const games = simulations.sort((a, b) => {
        if (b.showAt < a.showAt) return 1;
        else return -1;
    });

    return <>
        <section className="d-flex flex-column full-height">
            <Calendar onChange={setDate} />
            <div hidden={showRequests} className="border-top border-gray overflow-y-auto" style={{ flexGrow: 1 }}>
                {games.length == 0 && <Empty message="예정된 시뮬레이션이 없습니다" />}
                {games.map(game => <Game key={game.id} id={game.id} {...game} setTeam={setTeam} />)}
            </div>
            {/* 관리자용 사용자 요청 목록 */}
            {isAdmin && userRequests.length > 0 && (
                <div className="border-top border-gray">
                    <div className="p-3 border-bottom border-gray bg-light pointer">
                        <h6 className="mb-0 text-primary" onClick={() => setShowRequests(!showRequests)}>
                            🔔 사용자 시뮬레이션 요청이 있습니다! ({userRequests.length}개 대기중)
                        </h6>
                    </div>
                    {showRequests && <>
                        <div className="overflow-y-auto">
                            {userRequests.map(request => (
                                <UserRequestGame
                                    key={request.id}
                                    {...request}
                                    onStatusChange={fetchUserRequests}
                                />
                            ))}
                        </div>
                    </>}
                </div>
            )}
            
            {/* 사용자용 내 요청 목록 */}
            {isUser && (
                <div className="border-top border-gray">
                    <div className="p-3 border-bottom border-gray bg-light">
                        <h6 className="mb-0 text-info" onClick={() => setShowRequests(!showRequests)}>
                            📋 내 시뮬레이션 요청 {myRequests.length >= 0 ? `(${myRequests.length}개)` : ''}
                        </h6>
                    </div>
                    <UserMyRequests hidden={!showRequests} />
                </div>
            )}
        </section>
        {!!auth.id && <Link to={`${URL.SIMULATION}/create`} className="create_button" />}
       
        <Outlet context={{ setShowRequests, selectedTeam: team }} />
    </>
}
