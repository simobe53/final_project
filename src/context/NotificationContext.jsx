/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useEffect, useState, useRef } from 'react';
import { useAuth } from './AuthContext';
import axios from '/config/axios';

const NotificationContext = createContext(null);

export function NotificationProvider({ children }) {
    const [notifications, setNotifications] = useState([]);
    const [unreadCount, setUnreadCount] = useState(0);
    const { auth } = useAuth();
    const eventSourceRef = useRef(null);

    // SSE 연결
    useEffect(() => {
        if (!auth.id) {
            // 로그아웃 시 연결 종료
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
                eventSourceRef.current = null;
            }
            setNotifications([]);
            setUnreadCount(0);
            return;
        }

        console.log('🔔 SSE 연결 시도...');

        const eventSource = new EventSource('/api/notifications/stream', {
            withCredentials: true
        });

        eventSource.addEventListener('connected', (event) => {
            console.log('✅ SSE 연결 성공:', event.data);
        });

        eventSource.addEventListener('notification', (event) => {
            const notification = JSON.parse(event.data);
            console.log('📬 알림 수신:', notification);

            // 알림 목록 추가
            setNotifications(prev => [notification, ...prev]);
            setUnreadCount(prev => prev + 1);

            // 사운드 재생 (긴급 알림)
            if (notification.isUrgent) {
                playNotificationSound();
            }
        });

        eventSource.onerror = (error) => {
            console.error('❌ SSE 오류:', error);
            eventSource.close();
        };

        eventSourceRef.current = eventSource;

        // 읽지 않은 알림 개수 초기 조회
        fetchUnreadCount();

        return () => {
            console.log('🔌 SSE 연결 종료');
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
                eventSourceRef.current = null;
            }
        };
    }, [auth.id]);

    // 읽지 않은 알림 개수 조회
    const fetchUnreadCount = async () => {
        try {
            const { data } = await axios.get('/api/notifications/unread-count');
            setUnreadCount(data.count);
        } catch (error) {
            console.error('읽지 않은 알림 개수 조회 실패:', error);
        }
    };

    // 모든 알림 조회
    const fetchAllNotifications = async () => {
        try {
            const { data } = await axios.get('/api/notifications');
            setNotifications(data);
        } catch (error) {
            console.error('알림 조회 실패:', error);
        }
    };

    // 알림 읽음 처리
    const markAsRead = async (notificationId) => {
        try {
            await axios.put(`/api/notifications/${notificationId}/read`);
            
            // 로컬 상태 업데이트
            setNotifications(prev =>
                prev.map(n => n.id === notificationId ? { ...n, isRead: true } : n)
            );
            setUnreadCount(prev => Math.max(0, prev - 1));
        } catch (error) {
            console.error('알림 읽음 처리 실패:', error);
        }
    };

    // 모든 알림 읽음 처리
    const markAllAsRead = async () => {
        try {
            await axios.put('/api/notifications/read-all');
            
            setNotifications(prev =>
                prev.map(n => ({ ...n, isRead: true }))
            );
            setUnreadCount(0);
        } catch (error) {
            console.error('모든 알림 읽음 처리 실패:', error);
        }
    };

    // 알림 삭제
    const deleteNotification = async (notificationId) => {
        try {
            await axios.delete(`/api/notifications/${notificationId}`);
            
            const notification = notifications.find(n => n.id === notificationId);
            if (notification && !notification.isRead) {
                setUnreadCount(prev => Math.max(0, prev - 1));
            }
            
            setNotifications(prev => prev.filter(n => n.id !== notificationId));
        } catch (error) {
            console.error('알림 삭제 실패:', error);
        }
    };

    // 알림 사운드 재생
    const playNotificationSound = () => {
        try {
            const audio = new Audio('/assets/sounds/notification.mp3');
            audio.volume = 0.3;
            audio.play().catch(err => console.log('사운드 재생 실패:', err));
        } catch (error) {
            console.log('사운드 재생 실패:', error);
        }
    };

    return (
        <NotificationContext.Provider value={{
            notifications,
            unreadCount,
            markAsRead,
            markAllAsRead,
            deleteNotification,
            fetchAllNotifications
        }}>
            {children}
        </NotificationContext.Provider>
    );
}

export function useNotifications() {
    const context = useContext(NotificationContext);
    if (!context) {
        throw new Error('useNotifications must be used within NotificationProvider');
    }
    return context;
}

