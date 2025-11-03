import { useState } from "react";
import { useNavigate, useOutletContext } from "react-router-dom";
import { OverlayPage } from "/components";
import { URL } from "/config/constants";
import TeamSelect from "/components/TeamSelect";
import LineupSelect from "./components/LineupSelect";
import { useInit } from "/context/InitContext";
import { useSimulations } from "/context/SimulationsContext";
import { useAuth } from "/context/AuthContext";
import { createSimulation, createSimulationRequest } from "/services/simulations";

function TeamPlaceholder({ type }) {
    return <div className="d-flex flex-column align-items-center justify-content-center gap-8">
        <span className="h4 text-gray">{type}</span>
        <img src="/assets/icons/choose.png" alt="팀을 선택해주세요" width="40%" style={{ opacity: 0.3 }} />
    </div>
}

export default function CreateSimulation() {
    const { auth } = useAuth();
    const { teams } = useInit();
    const { date, simulations, myRequests, fetchModel } = useSimulations();
    const { setShowRequests } = useOutletContext();
    const isAdmin = auth?.role === 'ADMIN';
    const isUser = auth?.role === 'USER';
    const [form, setForm] = useState({
        homeTeam: null,
        awayTeam: null,
        homeLineup: {},
        awayLineup: {},
        showAt: null
    });
    const [step, setStep] = useState(1); // 1: 팀 선택, 2: 라인업 선택
    const navigate = useNavigate();

    const handleTeamSelect = (teamType, team) => {
        if (form[teamType] === team) setForm(prev => ({ ...prev, [teamType]: "" }))
        else if (teamType === 'awayTeam' && form.homeTeam === team) setForm(prev => ({ ...prev, homeTeam: "" }))
        else if (teamType === 'homeTeam' && form.awayTeam === team) setForm(prev => ({ ...prev, awayTeam: "", homeTeam: team }))
        else {
            setForm(prev => ({
                ...prev,
                [teamType]: team
            }));
        }
    };

    const handleLineupSelect = (teamType, lineup) => {
        setForm(prev => ({
            ...prev,
            [teamType]: lineup
        }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        console.log('Form data:', form); // 디버깅용
        
        if (!form.homeTeam || !form.awayTeam) {
            alert("홈팀과 어웨이팀을 모두 선택해주세요.");
            return;
        }

        // 라인업 검증: 투수 + 9명의 타자가 모두 선택되어야 함
        const isHomeLineupComplete = form.homeLineup.pitcher && Object.keys(form.homeLineup).length >= 10; // pitcher + 9 batters
        const isAwayLineupComplete = form.awayLineup.pitcher && Object.keys(form.awayLineup).length >= 10; // pitcher + 9 batters
        
        console.log('Home lineup:', form.homeLineup, 'Keys:', Object.keys(form.homeLineup).length); // 디버깅용
        console.log('Away lineup:', form.awayLineup, 'Keys:', Object.keys(form.awayLineup).length); // 디버깅용
        
        if (!isHomeLineupComplete || !isAwayLineupComplete) {
            alert("양팀의 투수와 9명의 타자를 모두 선택해주세요.");
            return;
        }

        try {
            // 라인업 데이터 변환 (Spring Boot API 형식으로)
            const convertLineup = (lineup) => {
                const converted = {
                    pitcher: lineup.pitcher?.id || null
                };
                
                // batting1~batting9 변환
                for (let i = 1; i <= 9; i++) {
                    converted[`batting${i}`] = lineup[i]?.id || null;
                }
                
                return converted;
            };

            if (isAdmin) {
                if (!form.showAt) {
                    alert('스트리밍 시간을 설정해주세요!');
                    return;
                }
                // 관리자인 경우: 기존 로직 그대로 (즉시 실행)
                const simulationData = {
                    hometeam: form.homeTeam.id, // 팀 ID (String)
                    awayteam: form.awayTeam.id, // 팀 ID (String)
                    homeLineup: JSON.stringify(convertLineup(form.homeLineup)),
                    awayLineup: JSON.stringify(convertLineup(form.awayLineup)),
                    showAt: form.showAt,
                    user: auth
                };

                console.log('관리자 시뮬레이션 생성:', simulationData);
                
                const data = await createSimulation(simulationData);
                try {
                    data.homeLineup = JSON.parse(data.homeLineup);
                    data.awayLineup = JSON.parse(data.awayLineup);
                } catch(e) {
                    console.log(e);
                }
                
                // 현재 선택된 날짜일때 Context에 추가 (로컬 상태 관리)
                if (data.showAt.startsWith(date)) fetchModel({ simulations: [ ...simulations, data ] });
                
                
                alert("시뮬레이션이 생성되었습니다!");
                navigate(`${URL.SIMULATION}`, { replace: true });
                
            } else if (isUser) {
                // 관리자도 일반 사용자도 : 요청으로 생성한다
                const requestData = {
                    hometeam: form.homeTeam.id, // String -> Long 변환
                    awayteam: form.awayTeam.id, // String -> Long 변환
                    homeLineup: JSON.stringify(convertLineup(form.homeLineup)), // JSON 문자열로 변환
                    awayLineup: JSON.stringify(convertLineup(form.awayLineup)), // JSON 문자열로 변환
                };

                console.log('사용자 시뮬레이션 요청:', requestData);
                
                const response = await createSimulationRequest(requestData);
                // Context에 추가 (로컬 상태 관리)
                fetchModel({ myRequests: [ ...myRequests, response ] });

                
                alert("시뮬레이션을 요청하였습니다!\n관리자가 확인 후 스트리밍 일정에 반영합니다.");
                setShowRequests(true);
                navigate(URL.SIMULATION, { replace: true });
            } else {
                alert("로그인이 필요합니다.");
                return;
            }
            
        } catch (error) {
            console.error('시뮬레이션 처리 실패:', error);
            alert("처리에 실패했습니다. 다시 시도해주세요.");
        }
    };

    const canProceedToLineup = form.homeTeam && form.awayTeam;
    
    // 🔧 라인업 완료 조건 개선
    const isHomeLineupComplete = form.homeLineup && 
        form.homeLineup.pitcher && 
        Object.keys(form.homeLineup).length >= 10; // pitcher + 9 batters
    
    const isAwayLineupComplete = form.awayLineup && 
        form.awayLineup.pitcher && 
        Object.keys(form.awayLineup).length >= 10; // pitcher + 9 batters
        
    const canSubmit = canProceedToLineup && isHomeLineupComplete && isAwayLineupComplete;
    
    // 💡 디버깅용 로그
    console.log('Create.jsx Debug:', {
        canProceedToLineup,
        isHomeLineupComplete,
        isAwayLineupComplete,
        canSubmit,
        homeLineup: form.homeLineup,
        awayLineup: form.awayLineup,
        homeKeys: Object.keys(form.homeLineup).length,
        awayKeys: Object.keys(form.awayLineup).length
    });
  
    const now = new Date().toISOString().slice(0, 16);
    return <>
        <OverlayPage 
            title="시뮬레이션 생성" 
            header={
                isAdmin && <div className="d-flex align-items-center gap-8" style={{ minWidth: 260 }}>
                    <small className="m-0 text-nowrap">일정</small>
                    <input type="datetime-local" style={{ width: 220 }} min={now} className="form-control" value={form.showAt} onChange={(e) => setForm(prev => ({ ...prev, showAt: e.target.value }))} />
                </div>
            }
        >
            <form method="POST" className="d-flex flex-column" style={{ height: '100%' }} onSubmit={handleSubmit}>
                {step === 1 && <>
                    <div className="p-4 d-flex flex-column gap-4">
                        <h4 className="text-center m-4">
                            <span className="p-1" style={{ borderBottom: '4px solid var(--point-color)' }}>홈팀과 어웨이팀을 선택해주세요</span>
                        </h4>
                        <div className="p-4 d-flex gap-20 align-items-center justify-content-center mb-4" style={{ zoom: 1.2 }}>
                            <div className="border-radius-12 bg-white d-flex align-items-center justify-content-center overflow-hidden" style={{ border: `3px ${form.homeTeam ? "solid" : "dashed"} var(--gray-border-color)`, width: 170, height: 178 }}>
                                {form.homeTeam ? <TeamSelect team={form.homeTeam} disabled /> : <TeamPlaceholder type="HOME" />}
                            </div>
                            <i className="h1 text-gray me-2">VS</i>
                            <div className="border-radius-12 bg-white d-flex align-items-center justify-content-center overflow-hidden" style={{ border: `3px ${form.awayTeam ? "solid" : "dashed"} var(--gray-border-color)`, width: 170, height: 178 }}>
                                {form.awayTeam ? <TeamSelect team={form.awayTeam} disabled /> : <TeamPlaceholder type="AWAY" />}
                            </div>
                        </div>
                        
                        <div className="d-flex gap-4">
                            <div className="d-flex flex-wrap justify-content-center gap-4" style={{ zoom: 0.7 }}>
                                {teams.map(team => <div key={team.id} className="position-relative">
                                    {((form.homeTeam?.id === team.id) || (form.awayTeam?.id === team.id )) && 
                                        <span className="position-absolute badge bg-point" style={{ borderWidth: 3 }}>
                                            {form.homeTeam?.id === team.id && "Home"}
                                            {form.awayTeam?.id === team.id && "Away"}
                                        </span>
                                    }
                                    <TeamSelect
                                        team={team}
                                        isSelected={form.homeTeam?.id === team.id || form.awayTeam?.id === team.id}
                                        onClick={() => handleTeamSelect(form.homeTeam?.id ? 'awayTeam' : 'homeTeam', team)}
                                    />
                                </div>)}
                            </div>
                        </div>
                    </div>
                    <button 
                        type="button"
                        className="btn btn-primary p-3 mt-4 border-radius-0 mt-auto"
                        onClick={() => setStep(2)}
                        disabled={!canProceedToLineup}
                    >
                        {!canProceedToLineup ? "홈팀과 어웨이팀을 선택하세요" : "라인업 선택하기"}
                    </button>
                </>}

                {step === 2 && <>
                    <div className="p-4 d-flex flex-column gap-4 position-relative">
                        <h4 className="text-center m-4">
                            <button 
                                type="button" 
                                className="btn btn-sm position-absolute"
                                style={{ top: 0, left: 0 }}
                                onClick={() => setStep(1)}
                            >
                                ← 팀 선택으로 돌아가기
                            </button>
                            <span className="p-1" style={{ borderBottom: '4px solid var(--point-color)' }}>라인업을 선택해주세요</span>
                        </h4>
                        
                        <div className="d-flex gap-4">
                            <div className="flex-1 d-flex flex-column">
                                <LineupSelect
                                    team={form.homeTeam}
                                    lineup={form.homeLineup}
                                    onLineupChange={(lineup) => handleLineupSelect('homeLineup', lineup)}
                                />
                            </div>
                            
                            <div className="flex-1 d-flex flex-column">
                                <LineupSelect
                                    team={form.awayTeam}
                                    lineup={form.awayLineup}
                                    onLineupChange={(lineup) => handleLineupSelect('awayLineup', lineup)}
                                />
                            </div>
                        </div>
                    </div>
                    <button 
                        type="submit" 
                        className="btn btn-primary p-3 mt-4 border-radius-0"
                        disabled={!canSubmit}
                    >
                        {isAdmin ? "시뮬레이션 생성하기" : "시뮬레이션 요청보내기"}
                    </button>
                </>}
            </form>
        </OverlayPage>
    </>
}