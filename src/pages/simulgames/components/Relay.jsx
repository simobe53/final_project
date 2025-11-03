import classes from "./Relay.module.scss"
import { useState, useEffect } from "react";
import { useInit } from "/context/InitContext";
import RelayBox from "./RelayBox";

const counts = [
    {label: '득점', path: 'score'},
    {label: '1회', path:'1'},
    {label: '2회', path:'2'},
    {label: '3회', path:'3'},
    {label: '4회', path:'4'},
    {label: '5회', path:'5'},
    {label: '6회', path:'6'},
    {label: '7회', path:'7'},
    {label: '8회', path:'8'},
    {label: '9회', path:'9'},
];

const makePRecord = (atBat, prev = {}) => {
    return {
        bat: prev.bat ? prev.bat + 1 : 1,
        hitter: prev.hitter ? prev.hitter + 1 : 1,
        safty: atBat.result === 'single' || atBat.result === 'double' || atBat.result === 'triple' || atBat.result === 'home_run' ? (prev.safty ? prev.safty + 1 : 1) : prev.safty || 0,
        score: prev.score ? prev.score + atBat.rbi : (atBat.rbi || 0),
        RBI: prev.RBI ? prev.RBI + atBat.rbi : (atBat.rbi || 0),
        HR: atBat.result === 'home_run' ? (prev.HR ? prev.HR + 1 : 1) : 0,
        BN: 0,
        SO: atBat.result === 'strikeout' ? (prev.SO ? prev.SO + 1 : 1) : 0
    };
};

export default function Relay({atBats = [], gameState = null, home, away}){
    const { players } = useInit();
    const [countsState, setCountsState] = useState(counts);
    const [activePath, setActivePath] = useState('score');
    const [pRecord, setPRecord] = useState({});

    // 이닝 자동 전환: gameState의 이닝이 변경되면 해당 이닝 탭으로 전환
    useEffect(() => {
        if (gameState && gameState.inning) {
            const currentInning = gameState.inning;
            // 현재 이닝 탭으로 자동 전환
            if (currentInning >= 1 && currentInning <= 9) {
                setActivePath(currentInning.toString());
            }
            else {
                // 득점 탭
                setActivePath('score')
            }
        }
    }, [gameState?.inning]);

    const makeBaseString = (pno, prevIdx, nextIdx) => `${prevIdx}루주자 ${players[pno]?.playerName} : ${nextIdx === 4 ? "홈인" : `${nextIdx}루까지 진루`}`;

    const makeBaseInfo = (prev, next, isScored, result) => {
        if (!prev.filter(e => !!e).length) return null;

        const ret = prev.reduce((acc, curr, idx) => {
            if (!curr) return acc;
            for (let i = idx+1; i<=prev.length; i++) {
                if (curr === next[i]) acc.push(makeBaseString(curr, idx+1, i+1));
                else if (!next.filter(o => o == curr).length && isScored) {
                    if (result === "병살타 아웃" && curr !== prev[prev.length - 1]) {
                        // 점수가 났는데 병살 나온 경우 : 3루주자만 홈인 그외 아웃.                    
                        acc.push(`${idx+1}루주자 ${players[curr]?.playerName} : 아웃`);
                    } else acc.push(makeBaseString(curr, idx+1, 4)); // 홈인
                    break;
                }
            }
            return acc;
        }, []);

        return ret;
    }

    // 실제 타석 데이터를 RelayBox 형식으로 변환
    const convertAtBatsToRelayData = () => {
        if (!atBats.length) return [];

        // atBats만 사용 (currentAtBat은 이미 atBats에 포함됨)
        const allAtBats = atBats;

        return allAtBats.reduce((acc, atBat, index) => {
            // 이닝 정보 추출
            const inningHalf = atBat.inningHalf || '';
            const inningNum = parseInt(inningHalf.match(/\d+/)?.[0]) || 1;
            const prevbs = [atBat.prevBase1, atBat.prevBase2, atBat.prevBase3];
            const nextbs = [atBat.newBase1, atBat.newBase2, atBat.newBase3];
            const isScored = atBat.inningHalf.includes("초") ? (atBat.newScoreAway - atBat.prevScoreAway) : (atBat.newScoreHome - atBat.prevScoreHome)
            const baseResult = makeBaseInfo(prevbs, nextbs, !!isScored, atBat.result_korean);
            const prev = acc.findLast(({ player: { pno }, bso }) => bso && pno === atBat.batterPNo) || {};
            const bso = makePRecord(atBat, prev.bso);
            return [...acc, {
                id: index + 1,
                isScored,
                inning: inningNum, // 이닝 정보 추가 (필터링용)
                inningHalf: inningHalf, // 구분선을 위한 이닝 반쪽 정보 추가
                player: {
                    pno: atBat.batterPNo,
                    image: players[atBat.batterPNo].imgUrl,
                    name: atBat.batter_name || '알 수 없음',
                    position: `${atBat.batting_order || '?'}번타자`,
                    average: atBat.batter_avg ? atBat.batter_avg.toFixed(3) : '0.000'
                },
                bso: bso,
                outStatus: {
                    name: atBat.batter_name || '알 수 없음',
                    Status: atBat.result_korean || atBat.result || '결과 없음'
                },
                persentage: {
                    team: atBat.inningHalf.includes("초") ? away : home,
                    odds: '50.0%',
                    calculate: '(0.0%p)'
                },
                probabilities: JSON.parse(atBat.probabilities),
                gameInfo: [
                    {
                        SBH: '결과',
                        CF: atBat.result_korean || atBat.result || '타석 결과',
                        score: inningHalf || '',
                        baseInfo: baseResult
                    }
                ],
            }];
        }, []);
    };

    const [currentData, setCurrentData] = useState(convertAtBatsToRelayData());

    // atBats가 변경될 때마다 데이터 업데이트
    useEffect(() => {
        setCurrentData(convertAtBatsToRelayData());
        if (atBats[atBats.length - 1] && parseInt(atBats[atBats.length - 1].inningHalf.replace(/초|말/gi, "")) > 9) {
            const inn = atBats[atBats.length - 1].inningHalf.replace(/초|말/gi, "");
            if (countsState.filter(({ path }) => path === inn).length === 0) setCountsState(prev => [...prev, {label: `${inn}회`, path: inn}]);
        }
    }, [atBats]);

    const handleEnter = (path) => {
        setActivePath(path);
        const newUrl = `${window.location.origin}${window.location.pathname}#${path}`;
        window.history.pushState({ path }, '', newUrl);
    }

    // 이닝별 필터링된 데이터
    const getFilteredData = () => {
        if (activePath === 'score') {
            // 득점
            return currentData.filter(o => o.isScored);
        } else {
            // 특정 이닝: 해당 이닝의 타석만 표시
            const selectedInning = parseInt(activePath);
            const filtered = currentData.filter(data => {
                return parseInt(data.inning) === selectedInning;
            });
            return filtered;
        }
    };

    const filteredData = getFilteredData();

    return<>
        <div className={classes.inning_count}>
                {countsState && countsState.map(({ path, label }) => (
                <button
                    key={label}
                    onClick={() => handleEnter(path)}
                    className={`${classes.items} ${activePath === path ? classes.active : ''}`}
                >
                    {label}
                </button>
            ))}
        </div>
        <h2 className="section_title" style={{ marginLeft: 32 }}>
            {activePath === 'score' ? '득점 현황' : `${activePath}회 공격`}
        </h2>
        <section>
            {filteredData && filteredData.length > 0 ? (
                filteredData.map((data, index) => {
                    // 다음 타석이 다른 이닝 반쪽이면 현재 타석 뒤에 구분선 표시
                    const prevData = filteredData[index - 1];
                    const showDividerAfter = index === 0 || (prevData &&
                                            data.inningHalf &&
                                            prevData.inningHalf &&
                                            data.inningHalf !== prevData.inningHalf);
                    const inning = data.inningHalf?.replace(/초|말/gi, "");
                    const inningTurn = data.inningHalf?.slice(data.inningHalf.length-1, data.inningHalf.length);

                    return (
                        <div key={data.id}>
                            {showDividerAfter && <div style={{
                                margin: '20px 0',
                                padding: '12px',
                                backgroundColor: '#fde3efff',
                                borderTop: '1px dashed var(--point-color)',
                                borderBottom: '1px dashed var(--point-color)',
                                textAlign: 'center',
                                fontWeight: 'bold',
                                color: 'var(--point-color)',
                                fontSize: '16px'
                            }}>
                                {`${inning}회${inningTurn} | ${inningTurn === "초" ? away.name : home.name} 공격`}
                            </div>}
                            <div className="p-2">
                                <RelayBox data={data} activePath={activePath} />
                            </div>
                        </div>
                    );
                })
            ) : (
                <div className="text-center p-4 text-muted">
                    {activePath === 'score' ? (
                        <p>게임을 시작하면 타석별 실시간 중계를 볼 수 있습니다</p>
                    ) : (
                        <p>{activePath}회 기록이 아직 없습니다</p>
                    )}
                </div>
            )}
        </section>
    </>

}