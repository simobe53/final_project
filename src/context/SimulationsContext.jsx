/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useState, useRef } from "react";
import { getSimulations } from "/services/simulations";
import { formatDate } from "/components";
import { useAuth } from "./AuthContext";

const initState = {
    simulations: [],
    myRequests: [],
    userRequests: []
};

const SimulationsContext = createContext(null);

export function SimulationsProvider({ children }) {
    const [date, setDate] = useState(formatDate(new Date()));
    const [model, setModel] = useState(initState);
    const { auth } = useAuth();
    const eventSourceRef = useRef(null);

    const fetchModel = (data) => {
        setModel(prev => {
            const newModel = { ...prev, ...data };
            return newModel;
        });
    }

    // SSE 연결 및 이벤트 처리
    useEffect(() => {
        if (!auth.id) {
            // 로그아웃 시 연결 종료
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
                eventSourceRef.current = null;
            }
            return;
        }

        console.log('📡 시뮬레이션 SSE 연결 시도...');

        const eventSource = new EventSource('/api/simulations/stream', {
            withCredentials: true
        });

        eventSource.addEventListener('connected', (event) => {
            console.log('✅ 시뮬레이션 SSE 연결 성공:', event.data);
        });

        // 새 시뮬레이션 요청 이벤트 (관리자용)
        eventSource.addEventListener('newRequest', (event) => {
            const newRequest = JSON.parse(event.data);
            console.log('📬 새 시뮬레이션 요청 수신:', newRequest);

            // 관리자 요청 목록에 추가
            setModel(prev => ({
                ...prev,
                userRequests: [newRequest, ...prev.userRequests]
            }));
        });

        // 요청 상태 변경 이벤트 (승인/거절)
        eventSource.addEventListener('requestStatusChanged', (event) => {
            const { requestId, status, adminComment } = JSON.parse(event.data);
            console.log('🔄 시뮬레이션 요청 상태 변경:', { requestId, status, adminComment });

            // 내 요청 목록에서 상태 업데이트
            setModel(prev => ({
                ...prev,
                myRequests: prev.myRequests.map(request => 
                    request.id === requestId 
                        ? { ...request, status, adminComment }
                        : request
                ),
                userRequests: prev.userRequests.map(request => 
                    request.id === requestId 
                        ? { ...request, status, adminComment }
                        : request
                )
            }));
        });

        // 새 시뮬레이션 승인 이벤트 (모든 사용자용)
        eventSource.addEventListener('simulationApproved', (event) => {
            const newSimulation = JSON.parse(event.data);
            console.log('🎉 새 시뮬레이션 승인 수신:', newSimulation);

            try {
                // 라인업 파싱
                newSimulation.homeLineup = JSON.parse(newSimulation.homeLineup);
                newSimulation.awayLineup = JSON.parse(newSimulation.awayLineup);
            } catch (e) {
                console.log('라인업 파싱 오류:', e);
            }

            // 현재 선택된 날짜일 때만 시뮬레이션 목록에 추가
            if (newSimulation.showAt && newSimulation.showAt.startsWith(date)) {
                setModel(prev => ({
                    ...prev,
                    simulations: [...prev.simulations, newSimulation]
                }));
            }
        });

        eventSource.onerror = (error) => {
            console.error('❌ 시뮬레이션 SSE 오류:', error);
            eventSource.close();
        };

        eventSourceRef.current = eventSource;

        return () => {
            console.log('🔌 시뮬레이션 SSE 연결 종료');
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
                eventSourceRef.current = null;
            }
        };
    }, [auth.id, date]);

    useEffect(() => {
        getSimulations(date).then((data) => {
            const simulations = data.reduce((acc, curr) => {
                const { homeLineup, awayLineup, ...obj } = curr;
                const newcurr = { ...obj, homeLineup: JSON.parse(homeLineup), awayLineup: JSON.parse(awayLineup) }
                acc.push(newcurr);
                return acc;
            }, []);
            setModel(prev => ({ ...prev, simulations }))
        })
    }, [date]);

    return <>
        <SimulationsContext.Provider value={{ ...model, date, setDate, fetchModel }}>
            {children}
        </SimulationsContext.Provider>
    </>
}

export function useSimulations() {
    return useContext(SimulationsContext);
}
