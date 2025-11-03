import { useState, useEffect, useMemo, useRef } from 'react';
import classes from './PlayGround.module.scss';


export default function PlayGroundBase({ player, getPlayer, currentStatus, finished }) {
    const playerRef = useRef();
    const result = currentStatus.result_korean;
    const [prevClass, setPrevClass] = useState("");

    const [prev, next, type = ""] = useMemo(() => {
        if (!player?.pno) return [];
        let i = 1;
        let j = 0;
        let t = "";
        while(i <= 3) {
            if (currentStatus[`prevBase${i}`] === player.pno) {
                j = [currentStatus.newBase1, currentStatus.newBase2, currentStatus.newBase3].findIndex(p => p === player.pno) + 1;
                if ([1,2,3].includes(j)) {
                    t = "";
                } 
                else if (["1루타", "2루타", "3루타", "홈런", "희생플라이 아웃"].includes(result)) {
                    // 안타쳤는데 newBase 없는 경우 무조건 홈인
                    t = "";
                    j = 4;
                }
                else {
                    t = "out";
                }
                break;
            }
            i++;
        }
        return [i, j, t];
    }, [currentStatus, player])

    // ⚙️ type(new or prev)에 따라 애니메이션 클래스 적용
    const baseClass = useMemo(() => {
        if (!player?.pno) return classes.hidden;

        // 베이스에 서있거나 서서 아웃된 경우
        if (next === 0 || prev === next) return classes[`bs${prev}${type}`];

        // 경기가 끝났을땐 gobase로 달리는 주자를 보여주지 않는다.
        if (finished) return classes.hidden;

        // 베이스에서 러닝
        return classes[`gobase${prev}to${next}${type}`];
    }, [player.pno, currentStatus, finished]);

    useEffect(() => {
        const el = playerRef.current;
        if (!el) return;
        // 매번 새 주자가 등장할 때 애니메이션 재시작 효과
        if (prevClass) el.classList.remove(prevClass);
        void el.offsetWidth; // 리플로우 강제 → 애니메이션 재시작 트릭
        el.classList.add(baseClass);
        setPrevClass(baseClass);
    }, [baseClass]);

    if (finished) return null;

    return <>
        <li ref={playerRef}>
            <span className={classes.blind}>{`${prev}루 주자`}</span>
            {getPlayer(player, true, true)}
        </li>
    </>
}