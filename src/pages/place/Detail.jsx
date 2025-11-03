import { useEffect, useMemo, useRef, useState } from "react";
import { Link, useOutletContext, useParams } from "react-router-dom";
import axios from "/config/axios";
import { URL } from "/config/constants";
import { OverlayPage } from "/components";
import { getMapfromAddress, setMarkerfromAddress } from "/components/Map";
import { getPlaceById, summarizeReviews } from "/services/places";
import PlaceReview from './components/Review';
import { Rank } from "./components/Rank";
import ScrapBtn from "./components/ScrapBtn";


/* 장소에 대한 별점 + 한줄평 페이지 */
export default function DetailPlace() {
    const { id } = useParams();
    const [place, setPlace] = useState({});   // 플레이스 객체 정보 Object
    const { ranks, setRanks } = useOutletContext();   // 플레이스 리뷰 배열 List
    const { scraps = 0, address, image } = place;
    const mapRef = useRef();

    // 리뷰 요약 관련 state
    const [reviewSummary, setReviewSummary] = useState('');
    const [isSummarizing, setIsSummarizing] = useState(false);
    const [summaryError, setSummaryError] = useState('');

    // 만족도 (4점 이상 준 사람들 백분율로 계산)
    const avgRank = useMemo(() => ranks.reduce((acc, curr) => acc + curr.rank, 0) / ranks.length, [ranks]);

    // 만족도 (4점 이상 준 사람들 백분율로 계산)
    const satisfiedCount = useMemo(() => ranks.reduce((acc, curr) => curr.rank >= 4 ? acc+1 : acc, 0), [ranks]);

    useEffect(() => {
        getPlaceById(id).then(data => {
            if (data.id) {
                setPlace(data);
                //setIsScrap(data.isScrap);
            }
        })
        
        /* TODO:: 여기서 별점평(리뷰) 불러와서 setRanks를 통해 state에 저장하기 */
        axios.get("/api/ranks/check", {
            params: { placeId: id }
        })
        .then(res => {
            if (Array.isArray(res.data)) setRanks(res.data || []);
        })
        .catch(err => {
            console.error("에러 발생:", err);
        });

    }, []);

    // 리뷰 요약 생성 함수
    const generateReviewSummary = async () => {
        if (ranks.length === 0) {
            setSummaryError('요약할 리뷰가 없습니다.');
            return;
        }

        setIsSummarizing(true);
        setSummaryError('');
        
        try {
            const result = await summarizeReviews(ranks);
            if (result.success) {
                setReviewSummary(result.summary);
            } else {
                setSummaryError(result.error || '리뷰 요약에 실패했습니다.');
            }
        } catch (error) {
            setSummaryError('리뷰 요약 중 오류가 발생했습니다.');
            console.error('리뷰 요약 오류:', error);
        } finally {
            setIsSummarizing(false);
        }
    };

    useEffect(() => {
        getMapfromAddress(mapRef.current, address, (map) => {
            setMarkerfromAddress(map, address);
        });
    }, [address]);

    return <>
        <OverlayPage title={place.name}>
            <div className="d-flex flex-column overflow-y-auto" style={{ height: '100%' }}>
                <div className="position-relative d-flex align-items-stretch">
                    <div style={{ width: 200 }}>
                        <div className="image-square border-radius-0" style={{ backgroundImage: `url('${image}')`, borderTop: 'none', borderBottom: 'none' }} />
                    </div>
                    <div className="flex-grow" ref={mapRef}></div>
                </div>
                <div className="border-top border-bottom border-gray p-3 ps-4">
                    <div className="d-flex justify-content-between align-items-center">
                        <div>
                            <h4 className="m-0 d-inline-flex align-items-center gap-8">{place.name} <small className="text-gray" style={{ fontSize: 16 }}>{place.category}</small></h4>
                            <p className="mt-1 mb-1 d-flex align-items-center gap-8" style={{ color: '#aaa' }}>
                                <i className="fas fa-location-dot" />
                                <span>{address}</span>
                            </p>
                        </div>
                        <ScrapBtn placeId={id} />
                    </div>
                    <div className="d-flex">
                        <div className="d-flex align-items-start mt-2">
                            <small className="mt-2 me-2 border-radius-20 ps-2 pe-2" style={{ background: 'rgba(0,0,0,0.05)' }}>총 평점</small>
                            <div>
                                <h3 className="m-0">
                                    <i>{(avgRank || 0)?.toFixed(1)}</i>
                                </h3>
                                <Rank value={avgRank || 0} readOnly size={20} />
                            </div>
                        </div>
                        <div className="d-flex align-items-start mt-2 m-auto">
                            <small className="mt-2 me-2 border-radius-20 ps-2 pe-2" style={{ background: 'rgba(0,0,0,0.05)' }}>만족도</small>
                            <div>
                                <h3 className="m-0 mb-2">
                                    <i>{satisfiedCount ? (satisfiedCount / ranks.length * 100).toFixed(1) : 0}%</i>
                                </h3>
                                <div className="small text-gray">총 {ranks.length}명중 {satisfiedCount}명 만족</div>
                            </div>
                        </div>
                    </div>
                </div>

                {/* 리뷰 요약 섹션 */}
                <div className="border-top border-bottom border-gray p-3 ps-4">
                    <div className="d-flex justify-content-between align-items-center mb-2">
                        <h5 className="m-0">리뷰 요약</h5>
                        <button 
                            className="btn btn-sm btn-outline-primary" 
                            onClick={generateReviewSummary}
                            disabled={isSummarizing || ranks.length === 0}
                        >
                            {isSummarizing ? (
                                <>
                                    <span className="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>
                                    요약 중...
                                </>
                            ) : (
                                'AI 요약 생성'
                            )}
                        </button>
                    </div>
                    
                    {summaryError && (
                        <div className="alert alert-warning alert-sm mb-2">
                            <small>{summaryError}</small>
                        </div>
                    )}
                    
                    {reviewSummary && (
                        <div className="p-3 border-radius-20" style={{ background: 'rgba(0,123,255,0.1)' }}>
                            <div className="d-flex align-items-start gap-2">
                                <i className="fas fa-robot text-primary mt-1"></i>
                                <div>
                                    <small className="text-primary fw-bold">AI 요약</small>
                                    <p className="m-0 mt-1" style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>
                                        {reviewSummary}
                                    </p>
                                </div>
                            </div>
                        </div>
                    )}
                    
                    {!reviewSummary && !summaryError && ranks.length > 0 && (
                        <div className="text-center text-gray py-3">
                            <i className="fas fa-lightbulb me-2"></i>
                            <small>AI가 리뷰를 분석하여 요약해드립니다</small>
                        </div>
                    )}
                </div>
                
                <div className="flex-grow position-sticky" style={{ top: 60, paddingBottom: 60 }}>
                    <ul className="p-4 d-flex flex-column gap-20">
                        {ranks.length === 0 && <li className="p-4 justify-self-center align-self-center text-center">등록된 리뷰가 없습니다.</li>}
                        {ranks.map(rank => <PlaceReview key={rank.id} {...rank} />)}

                    </ul>
                </div>
                <Link to={`${URL.PLACE}/${id}/createReview?placename=${place.name}`} className="btn btn-primary border-radius-0 position-absolute p-0" style={{ bottom: 0, width: '100%', lineHeight: "60px" }}>리뷰 작성하기</Link>
            </div>
        </OverlayPage>
    </>
}
