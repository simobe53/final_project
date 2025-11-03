import { useEffect, useState } from "react";
import { getArticlesBySimulation } from "/services/article";
import { useInit } from "/context/InitContext";
import TeamLogo from "/components/TeamLogo";

function ArticleItem({ team, data }) {
    const [open, setOpen] = useState({});
   
    return (
        <div className="border border-radius-20 border-gray overflow-hidden">
            <div className="d-flex align-items-center gap-8 p-1 ps-2 border-bottom bg-gray border-gray">
                <TeamLogo name={team.idKey} zoom={0.8} />
                <b>{team.name} 기사</b>
            </div>
            {data.map((article, index) => <>
                <div key={index} className={`p-4 border-gray pointer ${index !== data.length-1 ? "border-bottom" : ""}`} onClick={() => setOpen(prev => ({ ...prev, [index]: !prev[index] }))}>
                    <h4 className="font-semibold h6 mb-3">
                        {article.title || `기사 ${index + 1}`} <span className="small text-gray-400"></span>
                    </h4>
                    <div className="overflow-hidden">
                        <p className={open[index] ? "" : "text-truncate-2"} style={{ fontSize: 15 }}>{article.content}</p>
                    </div>
                </div>
            </>)}
        </div>
    );
}

export default function Article({ simulationId }) {
    const { teams } = useInit();
    const [articles, setArticles] = useState([]);
    const [home, setHome] = useState({});
    const [away, setAway] = useState({});
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!simulationId) return;

        const fetchArticles = async () => {
            try {
                setLoading(true);
                const data = await getArticlesBySimulation(simulationId);
                if (data.length > 0) {
                    const [d1,d2,d3,d4,d5,d6] = data;
                    const [hometeam] = teams.filter(({ name }) => name === d6.teamName);
                    const [awayteam] = teams.filter(({ name }) => name === d1.teamName);
                    setHome({ team: hometeam, data: [d4,d5,d6] });
                    setAway({ team: awayteam, data: [d1,d2,d3] });
                }
            } catch (error) {
                console.error("기사 로드 실패:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchArticles();
    }, [simulationId]);

    if (loading) return <div className="p-4 text-center" style={{ lineHeight: '300px' }}>기사 로딩 중...</div>;
    if (!home?.data?.length && !away?.data?.length) return <div className="p-4 text-center" style={{ lineHeight: '300px' }}>생성된 기사가 없습니다.</div>;

    return (
        <section className="p-4 pb-0">
            <div className="d-flex flex-column gap-4">
                <ArticleItem team={home.team} data={home.data} />
                <ArticleItem team={away.team} data={away.data} />
            </div>
        </section>
    );
}