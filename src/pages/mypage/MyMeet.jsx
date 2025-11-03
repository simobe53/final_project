import { useEffect, useMemo, useState } from 'react';
import StatusBar from '/components/StatusBar';
import { getMeetAll as getAllMeets } from '/services/meets';
import Meet from '/pages/meet/components/Meet';
import Empty from '/components/Empty';

export default function MyMeet () {
    const [tab, setTab] = useState('writer');
    const [meets, setMeets] = useState([]);

    useEffect(() => {
        getAllMeets().then(data => setMeets(data));
    }, []);

    const filteredMeets = useMemo(() => meets.filter(meet => meet[tab] == true), [meets, tab]);

    return <>
        <StatusBar title="나의 직관 모집글" />
        <div className="full-height m-0">
            <div className="btn-group mt-4 mb-2 d-flex m-auto" style={{ width: 250, justifySelf: 'center' }} role="group">
                <button className={`btn btn-sm ${tab === 'writer' ? "btn-primary" : "border border-gray"}`} onClick={() => setTab('writer')}>내가모집한글</button>
                <button className={`btn btn-sm ${tab === 'apply' ? "btn-primary" : "border border-gray "}`} onClick={() => setTab('apply')}>내가지원한글</button>
            </div>
            {filteredMeets.length == 0 ? <Empty message="모집글이 없습니다" /> :
            <ul className="d-flex flex-column gap-8 p-3 overflow-y-auto" style={{ maxHeight: '100%' }}>
                {filteredMeets.map(meet => <Meet key={meet.id} {...meet} />)}
            </ul>
            }
        </div>
    </>
}