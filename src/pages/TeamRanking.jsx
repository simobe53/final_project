import { useEffect, useState } from "react";
import { getTeamRanks } from "/services/teamRanks";
import styles from "./TeamRanking.module.scss";
import { useInit } from "/context/InitContext";

export default function TeamRanking() {
    const [rankings, setRankings] = useState([]);
    const [loading, setLoading] = useState(true);
    const { teams } = useInit();

    useEffect(() => {
        if (!teams) return;
        getTeamRanks("2025")
            .then(data => {
                const dt = data.map(d => ({
                    ...d,
                    idKey: teams.filter(({ name }) => name.includes(d.teamName))[0]?.idKey
                }))
                setRankings(dt || []);
            })
            .catch(error => {
                console.error('팀 순위 조회 실패:', error);
                setRankings([]);
            })
            .finally(() => setLoading(false));
    }, [teams]);

    if (loading) {
        return (
            <div className={styles.loading}>
                <p className="text-gray small text-center p-4">팀 순위를 불러오는 중...</p>
            </div>
        );
    }

    if (rankings.length === 0) {
        return (
            <div className={styles.empty}>
                <p className="text-gray small text-center p-4">팀 순위 정보가 없습니다.</p>
            </div>
        );
    }

    return (
        <div className="border border-gray border-radius-12 overflow-hidden">
            <div className={styles.table}>
                <div className={styles.tableHeader}>
                    <div className={styles.rank}>순위</div>
                    <div className={styles.stat}>팀</div>
                    <div className={styles.team} />
                    <div className={styles.stat}>경기</div>
                    <div className={styles.stat}>승</div>
                    <div className={styles.stat}>패</div>
                    <div className={styles.stat}>무</div>
                    <div className={styles.stat}>승률</div>
                    <div className={styles.stat}>게임차</div>
                    <div className={styles.stat} style={{ width: 100 }}>최근 10경기</div>
                </div>
                <div className={styles.tableBody}>
                    {rankings.map((team) => (
                        <div key={team.teamName} className={styles.tableRow} style={{ background: `url('/assets/icons/${team.idKey}.png') 130px center / auto 200%  no-repeat` }}>
                            <div className={styles.rank}>{team.rank}</div>
                            <div className={styles.stat}>{team.teamName}</div>
                            <div className={styles.team} />
                            <div className={styles.stat}>{team.gamesPlayed}</div>
                            <div className={styles.stat}>{team.wins}</div>
                            <div className={styles.stat}>{team.losses}</div>
                            <div className={styles.stat}>{team.ties}</div>
                            <div className={styles.stat}>{team.winPct}</div>
                            <div className={styles.stat}>{team.gb || '-'}</div>
                            <div className={styles.stat} style={{ width: 100 }}>{team.last10Games}</div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
