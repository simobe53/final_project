import { useEffect, useState } from "react";
import { getTeamRanks } from "/services/teamRanks";
import TeamLogo from "./TeamLogo";
import styles from "./TeamRanking.module.scss";

export default function TeamRanking() {
    const [rankings, setRankings] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        getTeamRanks("2025")
            .then(data => setRankings(data || []))
            .catch(error => {
                console.error('팀 순위 조회 실패:', error);
                setRankings([]);
            })
            .finally(() => setLoading(false));
    }, []);

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
        <div className={styles.teamRanking}>
            <div className={styles.header}>
                <div className={styles.titleBar}></div>
                <h3 className={styles.title}>팀 순위</h3>
            </div>
            <div className={styles.table}>
                <div className={styles.tableHeader}>
                    <div className={styles.rank}>순위</div>
                    <div className={styles.team}>팀</div>
                    <div className={styles.stat}>경기</div>
                    <div className={styles.stat}>승</div>
                    <div className={styles.stat}>패</div>
                    <div className={styles.stat}>무</div>
                    <div className={styles.stat}>승률</div>
                    <div className={styles.stat}>게임차</div>
                </div>
                <div className={styles.tableBody}>
                    {rankings.map((team) => (
                        <div key={team.teamName} className={styles.tableRow}>
                            <div className={styles.rank}>{team.rank}</div>
                            <div className={styles.team}>
                                <TeamLogo name={team.teamName} small zoom={0.6} className={styles.teamLogo} />
                                <span className={styles.teamName}>{team.teamName}</span>
                            </div>
                            <div className={styles.stat}>{team.gamesPlayed}</div>
                            <div className={styles.stat}>{team.wins}</div>
                            <div className={styles.stat}>{team.losses}</div>
                            <div className={styles.stat}>{team.ties}</div>
                            <div className={styles.stat}>{team.winPct}</div>
                            <div className={styles.stat}>{team.gb || '-'}</div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}

