import { useEffect, useMemo, useState } from 'react';
import { useInit } from "/context/InitContext";
import classes from './PlayGround.module.scss';
import PlayGroundBase from './PlayGroundBase';
import PlayGroundBatter from './PlayGroundBatter';

export default function PlayGround({ currentStatus = {}, lineup = {}, batterLineup = {}, gameStatus = "" }) {
    const { players } = useInit();
    const [result, setResult] = useState(null);
    const finished = gameStatus === "FINISHED";

    const getPlayer = (player, withImage = false) => {
        if (!player) return null;
        return <>
            {withImage && <span className={classes.img}>
                <img src={player.imgUrl} />
            </span>}
            <span className={classes.name}>{player.playerName}</span>
        </>
    }

    // 수비수
    const playerSet = useMemo(() => {
        return Object.keys(lineup).reduce((acc, curr) => {
            const player = players[lineup[curr]];
            if (!player) return acc;
            if (curr.includes("batting")) {
                if (player.position?.includes("포수") && !acc.c)
                    return { ...acc, c: player };
                else if (player.position?.includes("내야수")) 
                    return acc.bs?.length > 4 ? {...acc, of: [...acc.of, player]} : { ...acc, bs: [...acc.bs, player] };
                else if (player.position?.includes("외야수"))
                    return acc.of?.length > 3 ? { ...acc, bs: [...acc.bs, player] } : { ...acc, of: [...acc.of, player] };
                else
                    return acc.of?.length > 3 ? { ...acc, bs: [...acc.bs, player] } : { ...acc, of: [...acc.of, player] };
            }
            else return { ...acc, p: player }
        }, { p: null, c: null, bs: [], of: [] });
    }, [lineup]);

    useEffect(() => {
        if (currentStatus.result_korean) {
            setResult(false);
            setTimeout(() => setResult(currentStatus.result_korean), 2000);
            setTimeout(() => setResult(null), 3000);
        }
    }, [currentStatus]);

    const nextBatters = useMemo(() => {
        const [turn] = Object.keys(batterLineup).filter(d => batterLineup[d] === currentStatus.batterPNo);
        let idx = parseInt(turn?.replace("batting", ""));
        let ret = [];
        while(true) {
            let nextTurn = batterLineup[`batting${idx}`];
            if (!nextTurn) {
                nextTurn = batterLineup['batting1'];
                idx = 1;
            }
            ret.push({ ...players[nextTurn], turn: idx });
            idx++;
            if (ret.length >= 4) break;
        }
        return ret;
    }, [batterLineup, currentStatus.batterPNo, players]);

    const baseRunners = useMemo(() => {
        const { prevBase1, prevBase2, prevBase3, newBase1, newBase2, newBase3 } = currentStatus;
        return [...new Set([prevBase1, prevBase2, prevBase3, newBase1, newBase2, newBase3])];
    }, [currentStatus]);

    console.log(playerSet);
    return <>
        <div className={classes.root}>
            {!finished && result && <div className={classes.popResult}>
                <p className="text-white mb-2">{currentStatus.batter_name}</p>
                {result}!
            </div>}
            <div className={classes.nextTurn} hidden={gameStatus !== "PLAYING"}>
                <p className="h6">다음 타석</p>
                <ul>
                    {nextBatters.map(({ playerName, turn }, idx) => <li key={turn} className={idx !== 0 ? "small" : ""}>
                        <span className="me-2">{turn}</span>
                        {playerName}
                    </li>)}
                </ul>
            </div>
            <ul className={classes.ground}>
                <li className={classes.of1}>
                    <span className={classes.blind}>좌익수</span>
                    {getPlayer(playerSet.of[0])}
                </li>
                <li className={classes.of2}>
                    <span className={classes.blind}>중견수</span>
                    {getPlayer(playerSet.of[1])}
                </li>
                <li className={classes.of3}>
                    <span className={classes.blind}>우익수</span>
                    {getPlayer(playerSet.of[2])}
                </li>
                <li className={classes.ss}>
                    <span className={classes.blind}>유격수</span>
                    {getPlayer(playerSet.bs[0])}
                </li>
                <li className={classes.fb}>
                    <span className={classes.blind}>1루수</span>
                    {getPlayer(playerSet.bs[1])}
                </li>
                <li className={classes.sb}>
                    <span className={classes.blind}>2루수</span>
                    {getPlayer(playerSet.bs[2])}
                </li>
                <li className={classes.tb}>
                    <span className={classes.blind}>3루수</span>
                    {getPlayer(playerSet.bs[3])}
                </li>
                <li className={classes.p}>
                    <span className={classes.blind}>투수</span>
                    {getPlayer(playerSet.p, true)}
                </li>
                <li className={classes.c}>
                    <span className={classes.blind}>포수</span>
                    {getPlayer(playerSet.c)}
                </li>
                {currentStatus && currentStatus.batterPNo && players[currentStatus.batterPNo] && <PlayGroundBatter key={currentStatus.batterPNo} currentStatus={currentStatus} player={players[currentStatus.batterPNo]} getPlayer={getPlayer} finished={finished} />}
                {baseRunners.map((pno) => pno !== currentStatus.batterPNo && players[pno] && <PlayGroundBase key={pno} currentStatus={currentStatus} player={players[pno]} getPlayer={getPlayer} finished={finished} />)}
            </ul>

        </div>
    </>
}