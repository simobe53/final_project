import { useEffect, useRef, useState } from "react"
import { useAuth } from "/context/AuthContext";
import ChatMessage from "/components/ChatMessage";
import EmojiPicker from "/components/EmojiPicker";
import classes from "/pages/Chat.module.scss";
import { useParams } from "react-router-dom";


const hostname = location.hostname;
const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
const port = hostname === 'localhost' ? ':8080' : '';

async function loadSocket(simulationId) {
    const wsUrl = `${protocol}//${hostname}${port}/api/chat?simulationId=${simulationId}`;
    console.log('WebSocket 연결 시도 (채팅):', wsUrl);
    const data = new WebSocket(wsUrl);
    return data;
}

// TODO:: 유저가 나가는 시점이 아니라, 시뮬레이션이 끝나면 실시간 채팅 socket을 닫아줘야한다.
export default function Chat({ team, title, isHome = true }) {
    const messagesRef = useRef([]);
    const socketRef = useRef(null);
    const audioRef = useRef(null);  // 전역 오디오 객체
    const { auth } = useAuth();
    const { id: simulationId } = useParams();
    // 유저가 입장할때 누른 응원팀 진영
    const [homename, awayname] = title.split(' vs ');
    const [chatMessage, setChatMessage] = useState("");
    const [showEmojiPicker, setShowEmojiPicker] = useState(false);
    const [audioEnabled, setAudioEnabled] = useState(true);  // 토글 상태
    const [messages, setMessages] = useState([
        // 테스트용 메시지들
        // { type: 'aiBot', message: '안녕하세요! AI 어시스턴트입니다. 무엇을 도와드릴까요?' },
        // { type: 'cleanBot', message: '부적절한 표현이 감지되었습니다. 건전한 대화를 부탁드립니다.' },
        { type: 'notice', message: `${isHome ? homename : awayname} 응원 측으로 입장하셨습니다.` },
    ]);
    const chatRef = useRef();
    const inputRef = useRef();
    const makeMessage = ({ message, name: username, userTeam = { name: "", idKey: "" }, align = "right" }, idx) =>
        <div key={idx} className="d-flex flex-column">
            {username && <div className={`pe-4 d-inline-flex align-items-center self-align-${align === 'right' ? 'end' : 'start'}`}>
                <img src={`/assets/icons/${userTeam.idKey}.png`} width="24px" alt={userTeam.name} />
                <span className={classes.username}>{username}</span>
            </div>}
            <div className="pt-1 pb-1">{message}</div>
        </div>

    const sendMessage = (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (socketRef.current && socketRef.current.readyState === 1 && chatMessage) {
            if (auth.id) {
                socketRef.current.send(JSON.stringify({ userId: auth.id, content: chatMessage, align: isHome ? "left" : "right" }));
                setChatMessage("");
                setShowEmojiPicker(false);
            }
            else {
                alert("로그인이 필요한 기능입니다!");
            }
        }
    };

    const handleEmojiSelect = (emoji) => {
        setChatMessage(prev => prev + emoji);

        if (inputRef.current) {
            inputRef.current.focus();
        }
           
        };
    



    useEffect(() => {
        loadSocket(simulationId).then((ws) => {
            socketRef.current = ws;
            // 메시지 수신
            ws.onmessage = ({ data: dataJson }) => {
                const data = JSON.parse(dataJson);
                if (data.type === "bias-comment" && !(data.isHome !== undefined && !!data.isHome === isHome)) return;
                const updated = [...messagesRef.current, data];
                messagesRef.current = updated;
                setMessages(updated);

                // 편파해설 TTS 오디오 처리
                if (data.type === "bias-comment" && data.audioUrl && audioRef.current && audioEnabled) {
                    // 배열인 경우 순차 재생
                    if (Array.isArray(data.audioUrl)) {
                        let currentIndex = 0;
                        
                        const playNext = () => {
                            if (currentIndex < data.audioUrl.length && audioEnabled && audioRef.current) {
                                audioRef.current.src = data.audioUrl[currentIndex];
                                audioRef.current.play().catch(e => console.log("오디오 재생 실패:", e));
                                
                                // 현재 오디오가 끝나면 다음 오디오 재생
                                audioRef.current.onended = () => {
                                    currentIndex++;
                                    if (currentIndex < data.audioUrl.length) {
                                        playNext();
                                    }
                                };
                            }
                        };
                        
                        playNext();
                    } else {
                        // 단일 오디오 (하위 호환)
                        audioRef.current.pause();
                        audioRef.current.src = data.audioUrl;
                        audioRef.current.play().catch(e => console.log("오디오 재생 실패:", e));
                    }
                }
            };
        }).catch(e => console.log(e));

        return () => {
            // 사용자가 채팅을 종료하면, websocket 종료
            socketRef.current?.close();
            socketRef.current = null;
        }
    }, [audioEnabled, isHome]);

    //새로운 메시지가 나타나면 채팅창 스크롤을 밑으로 내린다.
    useEffect(() => {
        messagesRef.current = messages;
        if(chatRef.current) chatRef.current.scrollTop = chatRef.current.scrollHeight;
    }, [messages]);

    return <>
        <aside className={`d-flex flex-column align-items-stretch bg-white ${classes.sidebar} ${classes.right} ${classes.active} ${classes.maxHeight800}`}>
            <div className={`d-flex p-2 flex-column bg-point align-items-center justify-content-between ${classes.sidebarHeader}`}>
                <small className="text-white m-0">실시간 응원챗</small>
                <p className="text-white h6 m-0 p-1">{title}</p>
                {/* TTS 오디오 토글 버튼 */}
                <button 
                    onClick={() => setAudioEnabled(!audioEnabled)}
                    className={`btn btn-sm ${classes.audioToggleBtn} ${audioEnabled ? classes.enabled : ''}`}
                    title={audioEnabled ? "편파해설 소리 끄기" : "편파해설 소리 켜기"}
                >
                   {audioEnabled ? (
                        <i className="fas fa-volume-up"></i>
                    ) : (
                        <i className="fas fa-volume-mute"></i>
                    )}
                </button>
            </div>
            <div className={`flex-grow d-flex flex-column bg-white overflow-hidden ${classes.chat}`}>
                <div ref={chatRef} className="flex-grow d-flex flex-column pt-2 pb-2 ps-3 pe-3 overflow-y-auto position-relative">
                    {messages.map(({ type, message, audioUrl, ...others }, i) => (
                        <ChatMessage 
                            key={i} 
                            type={type} 
                            message={["send", "receive"].includes(type) ? makeMessage({ message, ...others }) : message} 
                            team={team}
                        />
                    ))}
                </div>
                {/* 이모지 피커 - 채팅 입력창 바로 위에 위치 */}
                <EmojiPicker 
                    isVisible={showEmojiPicker}
                    onEmojiSelect={handleEmojiSelect}
                    onClose={() => setShowEmojiPicker(false)}
                />
                <form className="p-2 border-top border-gray d-flex small gap-1 position-relative align-items-stretch" onSubmit={sendMessage}>
                    <div className="position-relative flex-grow">
                        <input ref={inputRef} type="text" name="content" className={`full-height form-control border-radius-12 ${classes.chatInput}`} value={chatMessage} onChange={e => setChatMessage(e.target.value)} />
                        <button 
                            type="button" 
                            onClick={() => setShowEmojiPicker(!showEmojiPicker)}
                            className={`text-nowrap position-absolute p-0 border-radius-20 btn btn-none d-flex align-items-center flex-column ${classes.emojiBtn}`}
                            title="이모지 추가"
                        >
                            ☺
                        </button>
                    </div>
                    <button type="submit" className={`btn btn-primary btn-sm text-nowrap p-2 border-radius-20 ${classes.submitBtn}`}>
                        <i className={`fas fa-paper-plane ${classes.submitIcon}`} />
                    </button>
                </form>
            </div>
            {/* 숨겨진 전역 오디오 플레이어 */}
            <audio ref={audioRef} preload="auto" className={classes.hiddenAudio} />
        </aside>
    </>
}