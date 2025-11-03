import { useEffect, useState } from 'react';
import { useAuth } from "/context/AuthContext";
import StatusBar from '/components/StatusBar';
import Empty from '/components/Empty';
import { getMyPlaces } from '/services/my';
import Place from '/pages/place/components/Place';

export default function MyPlace () {
    const { auth } = useAuth();
    const [places, setPlaces] = useState([]);

    useEffect(() => {
        getMyPlaces(auth.id).then(data => setPlaces(data));
    }, []);
    return <>
        <StatusBar title="내가 추천한 맛집" />
        {places.length == 0 ? <Empty message="내가 추천한 맛집이 없습니다" /> :
        <ul className="d-grid gap-20 p-4 overflow-y-auto" style={{ gridTemplateColumns: '1fr 1fr', maxHeight: '100%' }}>
            {places.map(place => <Place key={place.id} {...place} />)}
        </ul>
        }
    </>
}