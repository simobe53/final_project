import { useState, useEffect, useMemo } from "react";
import classes from "./Scoreboard.module.scss";

export default function Scoreboard({ gameInfo = {
    homeTeam: {
        scores: [0,0,0,0,0,0,0,0,0],
        totalRuns: 0,
        hits: 0,
        errors: 0
    },
    awayTeam: {
        scores: [0,0,0,0,0,0,0,0,0],
        totalRuns: 0,
        hits: 0,
        errors: 0
    },
    outs: 0
}, homeTeam, awayTeam, finished }) {
    const getTotalScore = (scores = []) => scores.reduce((acc,curr) => acc + curr, 0);
    const [gameData, setGameData] = useState(gameInfo);

    // props로 받은 gameInfo가 있으면 사용, 없으면 기본값 사용

    const innings = useMemo(() => {
        const inns = ['1', '2', '3', '4', '5', '6', '7', '8', '9'];
        if (gameData.currentInning > 9) {
            for (let i=10; i<=gameData.currentInning; i++) inns.push(`${i}`);
        }
        return inns
    }, [gameData.currentInning]);


    const homename = homeTeam.name || '';
    const awayname = awayTeam.name || '';

    useEffect(() => {
        setTimeout(() => {
            setGameData(gameInfo)
        }, finished ? 0 : 3000);
    }, [gameInfo])

    return (
        <div>
            {/* 이닝별 득점 스코어보드 */}
            <div className="p-4" style={{ backgroundColor: '#222' }}>
                <div className="p-3 d-flex align-items-center justify-content-evenly">
                    <div className="d-flex flex-column align-items-center">
                        <img src={`/assets/icons/${awayTeam.idKey}.png`} width="60px" height="auto" />
                        <h6 className="m-2 text-white">{awayname}</h6>
                    </div>
                    <span className="h1 text-white">{getTotalScore(gameData.awayTeam?.scores)}</span>
                    <div className="d-flex flex-column align-items-center gap-1" style={{ width: 130 }}>
                        <small className="point">{gameData.currentInning}회{gameData.isTopInning === false ? "말" : "초"}</small>
                        <small className="text-white" style={{ opacity: 0.5 }}>{gameData.homeTeam?.stadium}</small>
                        <div className={classes.outcount}>
                            {Array.from({ length: 3 }, (_, i) => (
                                <span key={i} className={i < gameData.outs ? classes.active : ""}>
                                    {i+1}
                                </span>
                            ))}
                            <small className="text-white ms-1">OUT</small>
                        </div>
                    </div>
                    <span className="h1 text-white">{getTotalScore(gameData.homeTeam?.scores)}</span>
                    <div className="d-flex flex-column align-items-center">
                        <img src={`/assets/icons/${homeTeam.idKey}.png`} width="60px" height="auto" />
                        <h6 className="m-2 text-white"><i className="fas fa-house me-1" style={{ fontSize: 10 }} /> {homename}</h6>
                    </div>
                </div>
                <div className={classes.table}>
                    <div className="d-flex flex-column">
                        <p><small>팀</small></p>
                        <p><span>{awayname.split(' ')[0]}</span></p>
                        <p><span>{homename.split(' ')[0]}</span></p>
                    </div>
                    
                    <div>
                        <div className="d-flex">
                            {innings.map((inning) => (
                                <p key={inning} className={gameData.currentInning == inning ? classes.current : ""} style={{ width: 32 }}>
                                    {inning}
                                </p>
                            ))}
                        </div>

                        {/* 원정팀 (위) */}
                        <div className="d-flex">
                            {gameData.awayTeam?.scores.map((score, index) => <p key={index} style={{ width: 32 }}>{score}</p>)}
                        </div>
                        
                        {/* 홈팀 (아래) */}
                        <div className="d-flex">
                            {gameData.homeTeam?.scores.map((score, index) => <p key={index} style={{ width: 32 }}>{score}</p>)}
                        </div>
                        
                    </div>
                    <div className="d-flex flex-column">
                        <div className="d-flex">
                            {['R', 'H', 'E'].map(i => <p key={i} className="flex-grow-1">{i}</p>)}
                        </div>
                        {/* 원정팀 (위) */}
                        <div className="d-flex">
                            <p className="flex-grow-1">{gameData.awayTeam?.totalRuns}</p>
                            <p className="flex-grow-1">{gameData.awayTeam?.hits}</p>
                            <p className="flex-grow-1">{gameData.awayTeam?.errors}</p>
                        </div>
                        
                        {/* 홈팀 (아래) */}
                        <div className="d-flex">
                            <p className="flex-grow-1">{gameData.awayTeam?.totalRuns}</p>
                            <p className="flex-grow-1">{gameData.homeTeam?.hits}</p>
                            <p className="flex-grow-1">{gameData.homeTeam?.errors}</p>
                        </div>
                    </div>
                </div>
            </div>

        </div>
    );
}
