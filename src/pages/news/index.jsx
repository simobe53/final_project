import { useEffect, useState } from "react";
import { Outlet, useSearchParams } from "react-router-dom";
import { useSearchParamsReplace } from "/components/hooks/useSearchParamsReplace";
import News from "./News";
import Highlights from "./Highlights";


const tabs = [
    { id: 'news', label: '뉴스' },
    { id: 'highlights', label: '하이라이트' }
];

export default function NewsHighLights() {
    const [params, setParamsReplace] = useSearchParamsReplace();
    const [tab, setTab] = useState(params.get("tab") || tabs[0].id);
    
    useEffect(() => {
        setParamsReplace({ tab });
    }, [tab])

    return <>
        <div className="d-flex gap-8 border-bottom">
            {tabs.map(({ id, label}) => <button key={id} className="btn btn-sm btn-none p-2 ps-3 pe-3 border-none border-radius-0" style={tab === id ? { color: 'var(--point-color) !important', borderBottom: '2px solid var(--point-color) !important' } : {}} onClick={() => setTab(id)}>{label}</button>)}
        </div>
        <section className="overflow-hidden" style={{ height: 'calc(100% - 40px)' }}>
            {tab === 'highlights' && <Highlights />}
            {tab === 'news' && <News />}
        </section>
        <Outlet />
    </>
}