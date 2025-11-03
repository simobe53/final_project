import { useEffect, useMemo, useRef, useState } from 'react';
import classes from './PlayGround.module.scss';


export default function PlayGroundBase({ player, getPlayer, currentStatus, finished }) {
    const playerRef = useRef();
    const [prevClass, setPrevClass] = useState("");

    // ⚙️ type(new or prev)에 따라 애니메이션 클래스 적용
    const baseClass = useMemo(() => {
        if (!player?.pno || finished) return classes.hidden;

        const position = player?.position.includes("좌타") ? "left" : "right";
        
        if (["홈런"].includes(currentStatus.result_korean)) return classes[`gobase${position}to4`]
        if (["1루타", "4구", "사구","내야 안타","선행주자아웃 출루", "실책 출루"].includes(currentStatus.result_korean)) return classes[`gobase${position}to1`]
        if (currentStatus.result_korean === "2루타") return classes[`gobase${position}to2`]
        if (currentStatus.result_korean === "3루타") return classes[`gobase${position}to3`]
        
        /** 아웃됨 */
        // 이닝체인지시 삼진만 아니면 무조건 다음 베이스로 달리다가 아웃
        if (currentStatus.prevOuts == 2 && currentStatus.result_korean !== "삼진")
            return classes[`gobase${position}to1out`];

        // 선채로 아웃
        if (["삼진", "직선타 아웃", "플라이 아웃", "희생플라이 아웃", "땅볼 아웃", "병살타 아웃"].includes(currentStatus.result_korean)) return classes[`${position.slice(0, 1)}b`];
        // 달리다가 아웃
        if (currentStatus.prevOuts == 2 || ["땅볼 아웃", "병살타 아웃"].includes(currentStatus.result_korean)) 
            return classes[`gobase${position}to1out`];
        return classes.hidden;

    }, [currentStatus]);


    useEffect(() => {
        const el = playerRef.current;
        if (!el) return;
        // 매번 새 주자가 등장할 때 애니메이션 재시작 효과
        if (prevClass) el.classList.remove(prevClass);
        void el.offsetWidth; // 리플로우 강제 → 애니메이션 재시작 트릭
        el.classList.add(baseClass);
        setPrevClass(baseClass);
    }, [baseClass]);

    if (!baseClass) return null;
    return <>
        <li ref={playerRef}>
            <span className={classes.blind}>타자</span>
            {getPlayer(player, true, true)}
        </li>
    </>
}