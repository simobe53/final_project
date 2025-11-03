import { useEffect, useRef, useState } from "react"
import { Link, Outlet } from "react-router-dom";
import { URL } from "/config/constants";
import { useAuth } from "/context/AuthContext";
import { useInit } from "/context/InitContext";
import { getMapfromAddress, setMarkerfromAddress, removeMarkers } from "/components/Map";
import { getAverageRank } from "/services/places";
import { Rank } from "./components/Rank";
import ScrapBtn from "./components/ScrapBtn";


export default function Places({ places, setPlaces }) {
    const { auth: { id, team: { location } = {} } } = useAuth();
    const { chat: { placeRedirect }, setPlaceRedirect } = useInit();
    const [map, setMap] = useState(null);
    const [ranks, setRanks] = useState([]);   // 플레이스 리뷰 배열 List
    const [clickedMap, setClickedMap] = useState({});
    const [avgRank, setAvgRank] = useState(0);
    const mapRef = useRef();

    useEffect(() => {
        // 사용자 주소로 지도 불러오기
        getMapfromAddress(mapRef.current, location, (m) => {
            setMap(m);
        });
    }, [location]);

    useEffect(() => {
        if (!map) return;
        removeMarkers();  // 마커 리셋

        // 지도 그리고 마커 찍기
        places.map((place) => {
            setMarkerfromAddress(map, place.address, () => setClickedMap(place));
        })
    }, [map, places]);

    // 챗봇에서 추천한 맛집으로 자동 이동
    useEffect(() => {
        if (placeRedirect && places.length > 0) {
            console.log('[Places] placeRedirect (place_id) 감지:', placeRedirect);

            // placeRedirect는 place_id (숫자)
            const targetPlace = places.find(place => place.id === placeRedirect);

            if (targetPlace) {
                console.log('[Places] 맛집 찾음:', targetPlace.name);
                setClickedMap(targetPlace);

                // 지도 중심을 해당 맛집으로 이동
                if (map && targetPlace.address) {
                    const geocoder = new kakao.maps.services.Geocoder();
                    geocoder.addressSearch(targetPlace.address, function(result, status) {
                        if (status === kakao.maps.services.Status.OK) {
                            const moveLatLng = new kakao.maps.LatLng(result[0].y, result[0].x);
                            map.setCenter(moveLatLng);  // 지도 중심 이동
                            console.log('[Places] 지도 중심 이동 완료:', targetPlace.name);
                        }
                    });
                }
            } else {
                console.log('[Places] ID', placeRedirect, '에 해당하는 맛집을 찾지 못함');
            }

            // placeRedirect 초기화
            setPlaceRedirect(null);
        }
    }, [placeRedirect, places, map]);

    useEffect(() => {
        if (clickedMap.id) getAverageRank(clickedMap.id).then((data) => setAvgRank(data));
    }, [clickedMap])

    return <>
        <div ref={mapRef} id="map" style={{ minHeight: '100%' }}></div>
        {!!id && <Link to={`${URL.PLACE}/create`} className="create_button" />}
        <div className={`position-absolute bg-white bottom-window ${clickedMap.id ? 'active' : ''}`}>
            <div className="d-flex gap-8 align-items-center p-3 border-top border-gray">
                <button className="bg-white fold-btn" hidden={!clickedMap.id} onClick={() => setClickedMap({})}>
                    <i className="fas fa-angle-down" />
                </button>
                <div className="border border-gray border-radius-12" style={{ borderBottom: 'none !important', minWidth: 100, height: 100, background: `url('${clickedMap.image || ''}') center / cover no-repeat` }}></div>
                <div className="p-2 pe-0 flex-grow">
                    <div className="d-flex gap-20 justify-content-between align-items-start">
                        <div>
                            <Link to={`${URL.PLACE}/${clickedMap.id}`} className="h6 text-nowrap">{clickedMap.name || ''}</Link>
                            <p className="small mt-1 text-gray text-nowrap">{clickedMap.category || ''}</p>
                        </div>
                        <ScrapBtn placeId={clickedMap.id} />
                    </div>
                    <Rank value={avgRank} readOnly />
                    <p className="text-gray"><small>{clickedMap.address || ''}</small></p>
                </div>
            </div>
        </div>
        <Outlet context={{ setPlaces, ranks, setRanks }} />
    </>
}