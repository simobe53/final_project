import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMyTeamNews, getMyTeamHighlights } from "/services/news";
import { formatDate } from "/components";
import { URL } from "/config/constants";
import { useAuth } from "/context/AuthContext";
import Weather from "./weather/Weather";
import NewsListItem from "./news/components/NewsListItem";
import HighlightsListItem from "./news/components/HighlightsListItem";
import Schedules from "./schedules/Schedules";
import TeamRanking from "./TeamRanking";

export default function Home() {
    const { auth } = useAuth();
    const [news, setNews] = useState([]);
    const [highlights, setHighLights] = useState([]);
    const [loading, setLoading] = useState({
        news: true,
        schedules: true,
        highlights: true
    });
    
    useEffect(() => {
        getMyTeamNews(5)
            .then(data => setNews(data || []))
            .catch(error => {
                console.error('뉴스 조회 실패:', error);
                setNews([]);
            })
            .finally(() => {
                setLoading(prev => ({ ...prev, news: false }));
            });
        getMyTeamHighlights(5)
            .then(data => setHighLights(data || []))
            .catch(error => {
                console.error('하이라이트 조회 실패:', error);
                setHighLights([]);
            })
            .finally(() => {
                setLoading(prev => ({ ...prev, highlights: false }));
            });
    }, [auth?.team]);
    
    return <>
<div className="overflow-y-auto p-4" style={{ height: '100%' }}>
   

    <h2 className="section_title mt-2">오늘 경기 일정</h2>
    <section>
        <Schedules date={formatDate(new Date())} />
    </section>

    <Weather />

    <h2 className="section_title">팀 순위</h2>
    <section>
        <TeamRanking />
    </section>

    <h2 className="section_title d-flex justify-content-between align-items-center">
        최신 뉴스 <small className="ms-2 mb-0 h6 point">{auth?.team?.name && ` - ${auth.team.name}`}</small>
        <Link to={URL.NEWS} className="btn btn-none ms-auto btn-sm">더보기 <i className="ms-2 fas fa-angle-right" /> </Link>
    </h2>
    <section className="d-flex flex-column gap-2">
        {loading.news ? (
            <p className="text-gray small text-center flex-grow p-4 border-radius-12">뉴스를 불러오는 중...</p>
        ) : news.length === 0 ? (
            <p className="text-gray small text-center flex-grow p-4 border-radius-12">응원 팀의 뉴스가 없습니다.</p>
        ) : news.slice(0, 3).map(item => <NewsListItem key={item.id} {...item} />)}
    </section>

    <h2 className="section_title d-flex align-items-center">
        최신 하이라이트 <small className="ms-2 mb-0 h6 point">{auth?.team?.name && ` - ${auth.team.name}`}</small>
    </h2>
    <section className="d-flex flex-column gap-8">
        {loading.highlights ? (
            <p className="text-gray small text-center flex-grow p-4 border-radius-12">하이라이트를 불러오는 중...</p>
        ) : highlights.length === 0 ? (
            <p className="text-gray small text-center flex-grow p-4 border-radius-12">응원 팀의 하이라이트가 없습니다.</p>
        ) : highlights.slice(0, 5).map(item => <HighlightsListItem key={item.id} {...item} />)}
    </section>

</div>
    </>
}
