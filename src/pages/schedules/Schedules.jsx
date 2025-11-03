import { useEffect, useRef, useState } from "react";
import { getSchedules } from "/services/news";
import useTouchScroll from "/components/hooks/useTouchScroll";
import Loading from "/components/Loading";
import Schedule from "./Schedule";

export default function Schedules({ date }) {
    const containerRef = useRef();
    const { setTranslateX } = useTouchScroll(containerRef);
    const [schedules, setSchedules] = useState([]);
    const [loading, setLoading] = useState(true);
    
    useEffect(() => {
        setLoading(true);
        getSchedules(date)
            .then(data => setSchedules(data || []))
            .catch(error => {
                console.error('스케쥴 조회 실패:', error);
                setSchedules([]);
            })
            .finally(() => {
                setLoading(false);
            });
    }, [date]);

    return <div ref={containerRef} onDrag={setTranslateX} style={{ height: 128, transform: `translateX(0px)` }}>
    {loading ? <Loading />
    : schedules.length === 0 ? <p className="text-gray d-flex flex-column justify-content-center small text-center flex-grow p-4 border-radius-12" style={{ height: '100%' }}>예정된 경기가 없습니다.</p>
    : <div className="d-flex gap-8 align-items-stretch justify-content-start">
        {schedules.map((sch) => <Schedule key={sch.id} {...sch} />)}
    </div>}
</div>
}