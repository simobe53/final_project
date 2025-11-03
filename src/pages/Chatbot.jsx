import { useEffect, useRef, useState } from "react"
import axios from "/config/axios";
import { useInit } from "/context/InitContext";
import { useAuth } from "/context/AuthContext";
import ChatMessage from "/components/ChatMessage";
import classes from "./Chat.module.scss";

export default function Chatbot() {
    const { auth } = useAuth();
    const { chat: { activate, autoMessage, displayMessage, questionType: selectedType }, activateChat, setQuestionType: setSelectedType } = useInit();
    const [chatMessage, setChatMessage] = useState("");
    const [messages, setMessages] = useState([
        { type: 'aiBot', message: '안녕하세요! AI 어시스턴트입니다. 무엇을 도와드릴까요?' }
    ]);
    const chatRef = useRef();
    const [aiPlaceHolder, setaiPlaceHolder] = useState("자유롭게 질문해주세요");

    useEffect(() => {
        const placeholders = {
            free: "야구와 관련된 질문을 자유롭게 해주세요.",
            rules: "야구 규칙에 대해 물어보세요. 예시)콜드게임 조건이 뭐야?",
            playerInfo: "선수에 대해 궁금한 점을 물어보세요. 예시)김광현 삼진 잘 잡아?",
            analysis: "팀에 대한 질문을 해주세요. 예시) 한화 잘하고 있어?",
            restaurant_rec: "구장 주변 맛집을 물어보세요. 문학구장 치킨집"
        };

        setaiPlaceHolder(placeholders[selectedType] || ["free"]);
    }, [selectedType]); // 버튼을 누를 때만 실행

 
    // 질문 기반으로 초기 로딩 메시지
    const getInitialLoadingMessage = (question) => {
        const q = question.toLowerCase();

        return '질문 분석 중...';
    };

    const sendMessage = async (e, actualMessage = null, shownMessage = null) => {
        if (e) e.preventDefault();

        const messageToSend = actualMessage || chatMessage;
        const messageToShow = shownMessage || messageToSend;

        if (!messageToSend.trim()) return;

        // 로그인 체크
        if (!auth.id) {
            setMessages(prev => [...prev,
                { type: 'send', message: messageToShow },
                { type: 'aiBot', message: '로그인이 필요합니다. 로그인 후 이용해주세요.' }
            ]);
            setChatMessage("");
            return;
        }

        setMessages(prev => [...prev, { type: 'send', message: messageToShow }])
        setChatMessage("");

        // 초기 로딩 메시지 (질문 기반 추측)
        const initialLoadingMessage = getInitialLoadingMessage(messageToSend);
        setMessages(prev => [...prev, { type: 'aiBot', message: initialLoadingMessage, isLoading: true }]);

        try {
            // 로딩 메시지 제외하고 최근 2개만 전달
            const recentMessages = messages
                .filter(msg => !msg.isLoading)
                .slice(-2);

            const response = await axios.post('/api/ai/chat/question', {
                message: messageToSend,
                selectedType: selectedType,
                chat_history: recentMessages
            });
            const finalMessage = response.data?.message || "응답을 불러오지 못했습니다.";

            // 로딩 메시지 제거하고 최종 응답 표시
            setMessages(prev => prev.filter(msg => !msg.isLoading).concat({ type: 'aiBot', message: finalMessage }));

        } catch (error) {
            console.error('[Chatbot] 챗봇 요청 실패:', error);

            let errorMessage = '오류가 발생했습니다. 다시 시도해주세요.';
            if (error.response?.status === 401) {
                errorMessage = '로그인이 필요합니다. 로그인 후 이용해주세요.';
            } else if (error.message) {
                errorMessage = error.message;
            }
            setMessages(prev => prev.filter(msg => !msg.isLoading).concat({ type: 'aiBot', message: errorMessage }));
        }
        if (e) e.stopPropagation();
    };

    // 자동 메시지가 있을 때 자동으로 전송
    useEffect(() => {
        if (activate && autoMessage) {
            // 챗봇이 열리고 autoMessage가 있으면 자동으로 전송
            setTimeout(() => {
                sendMessage(null, autoMessage, displayMessage);
                // 전송 후 autoMessage 초기화
                activateChat(true, null, null);
            }, 300); // 챗봇 애니메이션 후 전송
        }
    }, [activate, autoMessage]);


    // 새로운 메시지가 나타나면 채팅창 스크롤을 밑으로 내린다.
    useEffect(() => {
        if(chatRef.current) chatRef.current.scrollTop = chatRef.current.scrollHeight;
    }, [messages]);

    return <>
    <aside className={`d-flex flex-column align-items-stretch bg-white ${classes.sidebar}  ${activate === true ? classes.active : ""} ${activate === false ? classes.inactive : ""}`}>
        <div className="d-flex pt-2 p-3 align-items-center gap-20" style={{ height: 80 }}>
            <img src="/assets/icons/chatbot.png" className="fa-bounce" width="50px" />
            <div className="me-auto">
                <p className="h5 m-0">AI 야구 매니저</p>
                <small className="text-gray" style={{ fontSize: 13 }}>야구와 관련된 질문에 답변해드려요!</small>
            </div>
            <button className="btn btn-none p-2" onClick={() => activateChat(false)}>
                <i className="fas fa-xmark h5 m-0" />
            </button>
        </div>
        <section className="d-flex flex-grow-1" style={{ height: 'calc(100% - 80px)' }}>
            <div className={`d-flex flex-grow-1 flex-column overflow-hidden ${classes.chat} ${classes.leftAlign}`}>
                <div ref={chatRef} className="flex-grow d-flex flex-column pt-2 pb-2 ps-4 pe-4 overflow-y-auto" style={{ background: 'url("/assets/icons/robot.png") center / 40% no-repeat' }}>
                    {messages.map(({ type, message, isLoading }, i) => <ChatMessage key={i} type={type} message={message} isLoading={isLoading} />)}
                </div>
                <form className="p-2 d-flex flex-column small gap-8" onSubmit={sendMessage}>
                   {/* 챗봇이 참고하는 답변에 참고하는 버튼 */}
                    <div className="d-flex ps-1 gap-2 justify-content-start">
                        <button type="button" className={`btn btn-sm border-radius-12 ps-2 pe-2 ${selectedType === 'rules' ? 'btn-secondary' : 'btn-outline-secondary'}`}
                            onClick={() => setSelectedType(selectedType === 'rules' ? 'free' : 'rules')}> 야구 규칙 </button>
                        <button type="button" className={`btn btn-sm border-radius-12 ps-2 pe-2 ${selectedType === 'playerInfo' ? 'btn-secondary' : 'btn-outline-secondary'}`}
                            onClick={() => setSelectedType(selectedType === 'playerInfo' ? 'free' : 'playerInfo')}> 선수 정보 </button>
                        <button type="button" className={`btn btn-sm border-radius-12 ps-2 pe-2 ${selectedType === 'analysis' ? 'btn-secondary' : 'btn-outline-secondary'}`}
                            onClick={() => setSelectedType(selectedType === 'analysis' ? 'free' : 'analysis')}> 팀 분석 </button>
                        <button type="button" className={`btn btn-sm border-radius-12 ps-2 pe-2 ${selectedType === 'restaurant_rec' ? 'btn-secondary' : 'btn-outline-secondary'}`}
                            onClick={() => setSelectedType(selectedType === 'restaurant_rec' ? 'free' : 'restaurant_rec')}> 구장 주변 맛집 추천 </button>
                    </div>
                    <input type="text" name="content" placeholder={aiPlaceHolder} className="bg-gray form-control border-radius-20 p-3" style={{ fontSize: 14 }} value={chatMessage} onChange={e => setChatMessage(e.target.value)} />
                </form>
            </div>
        </section>
    </aside>

    </>
}
