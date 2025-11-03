/* eslint-disable react-refresh/only-export-components */

import { createContext, useContext, useEffect, useState } from "react";
import { getTeams, getPlayers } from '/services/getinits';


/**  변하지 않는 asset 딱 한번 불러서 저장해놓는 context. 업데이트 메소드 불필요 */
const InitContext = createContext(null);
const initState = {
    teams: [],
    players: {},
    news: [],
    highlights: []
};

const initChat = {
    activate: null,
    questionType: 'free',  // 챗봇 질문 타입
    autoMessage: null,  // 챗봇 열릴 때 자동으로 전송할 메시지
    displayMessage: null,  // 화면에 표시할 메시지 (실제 전송 메시지와 다를 수 있음)
    placeRedirect: null  // 맛집지도로 리다이렉트할 구장 정보
};
const initNotification = {
    activate: null
};

export function InitProvider({ children }) {
    const [chat, setChat] = useState(initChat);  // 전역 state;
    const [notification, setNotification] = useState(initNotification);  // 알림 state
    const [model, setModel] = useState(initState);  // 전역 state;
    useEffect(() => {
        getTeams().then(teams => setModel(prev => ({ ...prev, teams })));
        getPlayers().then(players => {
            setModel(prev => ({ ...prev, players: players.reduce((acc, curr) => ({ ...acc, [curr.pno]: curr }), {}) }));
        });
    }, []);
    // 챗봇 활성화 제어 및 자동 메시지 설정(뉴스 요약에 이용)
    const activateChat = (val, autoMessage = null, displayMessage = null) => {
        if (typeof val === 'boolean') {
            setChat(prev => ({ ...prev, activate: val, autoMessage, displayMessage: displayMessage || autoMessage }));
        } else {
            setChat(prev => ({ ...prev, activate: !prev.activate, autoMessage, displayMessage: displayMessage || autoMessage }));
        }
    };

    const setQuestionType = val => setChat(prev => ({ ...prev, questionType: val }));

    const activateNotification = (val) => setNotification(prev => ({ ...prev, activate: val || !prev.activate }));

    // 맛집 지도로 리다이렉트할 구장 정보 설정
    const setPlaceRedirect = (location) => {
        setChat(prev => ({ ...prev, placeRedirect: location }));
    };

    // 로그아웃 시 UI 초기화 (챗봇, 알림창 닫기)
    const resetUI = () => {
        setChat(initChat);
        setNotification(initNotification);
    };

    return <>
        <InitContext.Provider value={{
            teams: model.teams,
            players: model.players,
            highlights: model.highlights,
            news: model.news,
            chat,
            activateChat,
            setQuestionType,
            setPlaceRedirect,
            notification,
            activateNotification,
            resetUI
        }}>
            {children}
        </InitContext.Provider>
    </>
}

export function useInit() {
    return useContext(InitContext);
}
