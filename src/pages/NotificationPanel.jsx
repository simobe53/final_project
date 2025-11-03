import { useEffect, useRef } from "react";
import { useInit } from "/context/InitContext";
import { useNotifications } from "/context/NotificationContext";
import { useNavigate } from "react-router-dom";
import classes from "./Chat.module.scss";

export default function NotificationPanel() {
    const { notification: { activate }, activateNotification, teams } = useInit();
    const { notifications, unreadCount, markAsRead, markAllAsRead, deleteNotification, fetchAllNotifications } = useNotifications();
    const notificationRef = useRef();
    const navigate = useNavigate();

    // 패널 열릴 때 모든 알림 조회
    useEffect(() => {
        if (activate) {
            fetchAllNotifications();
        }
    }, [activate]);

    // 새 알림이 추가되면 스크롤 맨 위로
    useEffect(() => {
        if (notificationRef.current) {
            notificationRef.current.scrollTop = 0;
        }
    }, [notifications]);

    // 알림 클릭 처리
    const handleNotificationClick = (notification) => {
        markAsRead(notification.id);
        
        if (notification.link) {
            navigate(notification.link);
            activateNotification(false); // 패널 닫기
        }
    };

    // 알림 삭제 처리
    const handleDelete = (e, notificationId) => {
        e.stopPropagation();
        deleteNotification(notificationId);
    };

    // 팀 ID로 팀 이름 조회
    const getTeamName = (teamId) => {
        if (!teamId || !teams) return '';
        const team = teams.find(t => t.id === teamId);
        return team ? team.name : '';
    };

    // 알림 메시지에서 팀 이름 추가 (시뮬레이션 관련 알림)
    const enhanceNotificationMessage = (notification) => {
        const { message, notificationType, homeTeamId, awayTeamId } = notification;
        
        // 팀 정보가 있는 모든 시뮬레이션 관련 알림에 팀 이름 추가
        const simulationNotificationTypes = [
            'REQUEST_APPROVED', 
            'REQUEST_REJECTED', 
            'SIMULATION_REMINDER_10', 
            'SIMULATION_REMINDER_5', 
            'SIMULATION_STARTED', 
            'GAME_ENDED'
        ];
        
        if (simulationNotificationTypes.includes(notificationType) && homeTeamId && awayTeamId) {
            const homeTeam = getTeamName(homeTeamId);
            const awayTeam = getTeamName(awayTeamId);
            
            if (homeTeam && awayTeam) {
                // 메시지 타입에 따라 다른 형식 적용
                if (notificationType === 'GAME_ENDED') {
                    // "게임이 종료되었습니다. 홈팀 승리! (3 - 2)" → "LG vs 삼성 게임이 종료되었습니다. LG 승리! (3 - 2)"
                    let enhancedMessage = message;
                    
                    // "홈팀 승리"를 실제 홈팀 이름으로 변경
                    if (message.includes('홈팀 승리')) {
                        enhancedMessage = message.replace('홈팀 승리', `${homeTeam} 승리`);
                    }
                    // "원정팀 승리"를 실제 원정팀 이름으로 변경
                    else if (message.includes('원정팀 승리')) {
                        enhancedMessage = message.replace('원정팀 승리', `${awayTeam} 승리`);
                    }
                    
                    return `${homeTeam} vs ${awayTeam} ${enhancedMessage}`;
                } else {
                    // 다른 알림들은 앞에 팀 이름 추가
                    return `${homeTeam} vs ${awayTeam} ${message}`;
                }
            }
        }
        
        return message;
    };

    // 시간 포맷팅
    const formatTime = (timestamp) => {
        const now = new Date();
        const notificationTime = new Date(timestamp);
        const diffMs = now - notificationTime;
        const diffMinutes = Math.floor(diffMs / 60000);
        
        if (diffMinutes < 1) return '방금 전';
        if (diffMinutes < 60) return `${diffMinutes}분 전`;
        
        const diffHours = Math.floor(diffMinutes / 60);
        if (diffHours < 24) return `${diffHours}시간 전`;
        
        const diffDays = Math.floor(diffHours / 24);
        if (diffDays < 7) return `${diffDays}일 전`;
        
        return notificationTime.toLocaleDateString('ko-KR');
    };

    return (
        <aside className={`d-flex flex-column align-items-stretch bg-white ${classes.sidebar} ${classes.right} ${activate === true && classes.active} ${activate === false && classes.inactive}`}>
            {/* 헤더 */}
            <div className="d-flex pt-2 p-3 align-items-center gap-20" style={{ height: 80 }}>
                <i className="fas fa-bell text-primary fa-bounce" style={{ fontSize: '40px' }}></i>
                <div className="me-auto">
                    <p className="h5 m-0">알림</p>
                    <small className="text-gray" style={{ fontSize: 13 }}>
                        {unreadCount > 0 ? `읽지 않은 알림 ${unreadCount}개` : '모든 알림을 확인했습니다'}
                    </small>
                </div>
                <button className="btn btn-none p-2" onClick={() => activateNotification(false)}>
                    <i className="fas fa-xmark h5 m-0" />
                </button>
            </div>

            {/* 알림 목록 */}
            <section className="d-flex flex-column flex-grow-1" style={{ height: 'calc(100% - 80px)' }}>
                {/* 모두 읽음 버튼 */}
                {unreadCount > 0 && (
                    <div className="p-2 border-bottom text-center">
                        <button 
                            className="btn btn-sm btn-outline-primary"
                            onClick={markAllAsRead}
                        >
                            모두 읽음 처리
                        </button>
                    </div>
                )}

                {/* 알림 리스트 */}
                <div 
                    ref={notificationRef}
                    className="flex-grow-1 overflow-y-auto"
                    style={{ padding: '0.5rem' }}
                >
                    {notifications.length === 0 ? (
                        <div className="text-center p-4 text-muted">
                            <i className="fas fa-bell-slash mb-3" style={{ fontSize: '48px', opacity: 0.3 }}></i>
                            <p>알림이 없습니다.</p>
                        </div>
                    ) : (
                        notifications.map((notification) => (
                            <div 
                                key={notification.id}
                                className={`p-3 mb-2 border rounded cursor-pointer ${!notification.isRead ? 'bg-light border-primary' : ''}`}
                                onClick={() => handleNotificationClick(notification)}
                                style={{ 
                                    cursor: 'pointer',
                                    transition: 'all 0.2s',
                                    borderWidth: !notification.isRead ? '2px' : '1px'
                                }}
                                onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f8f9fa'}
                                onMouseLeave={(e) => e.currentTarget.style.backgroundColor = !notification.isRead ? '#f0f8ff' : 'white'}
                            >
                                <div className="d-flex justify-content-between align-items-start mb-2">
                                    <div className="d-flex align-items-center gap-2">
                                        {!notification.isRead && (
                                            <span className="badge bg-primary" style={{ fontSize: '10px' }}>NEW</span>
                                        )}
                                    </div>
                                    <button 
                                        className="btn btn-sm btn-none text-muted p-0"
                                        onClick={(e) => handleDelete(e, notification.id)}
                                        style={{ fontSize: '12px' }}
                                    >
                                        <i className="fas fa-times"></i>
                                    </button>
                                </div>
                                
                                <div className="mb-1">
                                    <strong style={{ fontSize: '14px' }}>{notification.title}</strong>
                                </div>
                                
                                <p className="mb-2 text-muted" style={{ fontSize: '13px', lineHeight: '1.4' }}>
                                    {enhanceNotificationMessage(notification)}
                                </p>
                                
                                <div className="text-muted" style={{ fontSize: '11px' }}>
                                    <i className="far fa-clock me-1"></i>
                                    {formatTime(notification.createdAt)}
                                </div>
                            </div>
                        ))
                    )}
                </div>
            </section>
        </aside>
    );
}

