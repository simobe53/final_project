import { useEffect, useState } from "react";
import Calendar from "/components/Calendar";
import Loading from "/components/Loading";
import Empty from "/components/Empty";
import { getHighlights } from "/services/news";
import HighlightsListItem from "./components/HighlightsListItem";

export default function Highlights() {
    const [date, setDate] = useState(null);
    const [data, setData] = useState([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (date) loadData();
    }, [date]);

    const loadData = () => {
        setLoading(true);
        getHighlights(date)
            .then(data => setData(data || []))
            .catch(error => {
                console.error('하이라이트 조회 실패:', error);
                setData([]);
            })
            .finally(() => setLoading(false));
    };

    return <>
        <Calendar onChange={setDate} />
        <div className="overflow-y-auto p-3" style={{ height: 'calc(100% - 114px)' }}>
            {/* 하이라이트 리스트 */}
            <div className="d-flex flex-column gap-8" style={{ height: '100%' }}>
                {loading ? <>
                    <Loading />
                    <p className="mt-2 text-gray small text-center">하이라이트를 불러오는 중...</p>
                </> : data.length === 0 ? <Empty message="하이라이트가 없습니다" />
                : (
                    data.map(item => <HighlightsListItem key={item.masterVid} {...item} />)
                )}
            </div>
        </div>
    </>
}


