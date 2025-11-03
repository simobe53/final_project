import { useState, useEffect, useRef } from "react"

function compareWithNow(localDateTimeStr) {
    const target = new Date(localDateTimeStr);
    const now = new Date();

    // 미래인가
    if (target > now) return 1;
    if (target < now) return -1;

    const isSameDay = now.getDate() === target.getDate();
    const timediff = now.getHours() - target.getHours();
    const isClose = timediff >= 0 && timediff < 1;
    // 오늘 가까운 시간인가
    if (isSameDay && isClose) return 2;

    // 아닌가
    return 0;
}

/** target이 현재보다 이후인지 확인 */
export function isAfterNow(localDateTimeStr) {
    return compareWithNow(localDateTimeStr) === 1;
}

/** target이 현재보다 이전인지 확인 */
export function isBeforeNow(localDateTimeStr) {
    return compareWithNow(localDateTimeStr) === -1;
}

export function isCloseNow(localDateTimeStr) {
    return compareWithNow(localDateTimeStr) === 2;
}

/**
 * 특정 시간(LocalDateTime 문자열)과 현재 시각을 비교하여
 * 만료 여부를 주기적으로 체크하는 훅
 */
export function useCheckTime() {
    const [expired, setExpired] = useState(null);
    const timerRef = useRef(); // ← useRef로 관리 (state로 관리하면 불필요한 리렌더 발생)

    const stopTimer = () => {
        if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
        }
        setExpired(null);
    };

    const checkTime = date => {
        setExpired(isBeforeNow(date));
    }

    const startTimer = ({ date, delay = 3000 }) => {
        // 이미 돌고 있는 타이머 정리
        if (timerRef?.current) clearInterval(timerRef.current);

        // 초기 판단
        checkTime(date);

        // 주기적으로 체크
        timerRef.current = setInterval(() => {
            checkTime(date);
        }, delay);
    };

    // 언마운트 시 타이머 정리
    useEffect(() => {
        return () => {
            if (timerRef.current) clearInterval(timerRef.current);
            setExpired(null)
        };
    }, []);

    return {
        startTimer,
        stopTimer,
        expired,
    };
}