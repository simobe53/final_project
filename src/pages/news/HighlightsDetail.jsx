import { useEffect, useState } from "react";
import { useParams,useNavigate } from "react-router-dom";
import { OverlayPage } from "/components";
import { getScheduleById, generateHighlightSummary } from "/services/news";
import {useAuth} from "/context/AuthContext";
import {URL} from "/config/constants";


export default function HighlightsDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const {auth} = useAuth();
    const [loading, setLoading] = useState(true);
    const [data, setData] = useState(null);
    const [summary, setSummary] = useState(null);
    const [summaryLoading, setSummaryLoading] = useState(false);
    const [summaryError, setSummaryError] = useState(null);
    
    
    useEffect(() => {
        getScheduleById(id, { isHighlight: true })
            .then(data => {
                setData(data);
               
            })
            .catch(e => console.log("뉴스 상세 조회 실패:", e))
            .finally(() => setLoading(false));
    }, [id]);

    // AI 요약 생성 요청
    const handleGenerateSummary = async () => {
        if(!auth?.id){
            alert("로그인이 필요합니다.");
            navigate(URL.LOGIN);
            return;
        }
        let timer;
        setSummaryLoading(true);
        setSummaryError(null);
        setSummary(null);

        if (data && data.highlightSummary) {
            // ⭐️ 분기 A: DB에 요약이 있는 경우 (5초 딜레이 적용 - 수정된 부분)

            // 5초(5000ms) 후에 요약을 표시하도록 타이머 설정
            timer = setTimeout(() => {
                setSummary(data.highlightSummary);
                setSummaryLoading(false); // 딜레이 후 로딩 해제
            }, 5000); 

        } else {
            // ⭐️ 분기 B: 요약이 없는 경우 (실제 AI 요청)
            
            const result = await generateHighlightSummary(id);

            if (result.success) {
                setSummary(result.summary);
                
                // DB 데이터 동기화 
                const updatedData = await getScheduleById(id, { isHighlight: true });
                setData(updatedData);

            } else {
                setSummaryError(result.error || '요약 생성에 실패했습니다.');
            }

            // AI 요청 완료 후 로딩 해제
            setSummaryLoading(false);
        }
    };

    if (loading) {
        return (
            <OverlayPage title="하이라이트">
                <div className="overflow-y-auto p-4" style={{ height: '100%' }}>
                    <p className="text-gray">로딩 중...</p>
                </div>
            </OverlayPage>
        );
    }

    if (!data) {
        return (
            <OverlayPage title="뉴스">
                <div className="overflow-y-auto p-4" style={{ height: '100%' }}>
                    <p className="text-gray">하이라이트를 찾을 수 없습니다.</p>
                </div>
            </OverlayPage>
        );
    }

    const { gameDate, homeTeam, awayTeam, highlightUrl } = data || {};

    // YouTube URL에 광고 추적 비활성화 매개변수 추가
    const getOptimizedYoutubeUrl = (url) => {
        if (!url) return url;

        // 이미 매개변수가 있는지 확인
        const hasParams = url.includes('?');
        const separator = hasParams ? '&' : '?';

        // 광고 및 추천 영상 비활성화 매개변수 추가
        return `${url}${separator}rel=0&modestbranding=1&iv_load_policy=3`;
    };

    return <>
        <OverlayPage title={`${gameDate} ${homeTeam} VS ${awayTeam}`}>
            <div className="d-flex flex-column align-items-center p-4 mt-4 mb-4">
                <iframe
                    src={getOptimizedYoutubeUrl(highlightUrl)}
                    width="640"
                    height="360"
                    allowFullScreen
                    frameBorder="0"
                    title="하이라이트 영상"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                ></iframe>

                <div className="d-flex flex-column align-items-stretch mt-4 p-4 gap-3" style={{ width: '100%', maxWidth: '800px' }}>
                    <div className="d-flex justify-content-between align-items-center">
                        <h2 className="section_title m-0">⚾ AI 경기 요약</h2>
                        
                        
                            <button 
                                className="btn btn-primary btn-sm"
                                onClick={handleGenerateSummary}
                                disabled={summaryLoading}
                                title={!auth?.id ? '로그인 후 이용 가능합니다' : undefined}
                            >
                                {summaryLoading ? (
                                    <>
                                        <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                        생성 중...
                                    </>
                                ) : (
                                    <>
                                        <i className="fas fa-magic me-2"></i>
                                        {auth?.id ? 'AI 요약 생성' : '로그인 후 이용'}
                                    </>
                                )}
                            </button>
                        
                    </div>

                    {summaryLoading && (
                        <div className="bg-light border-radius-20 p-4 text-center">
                            <div className="spinner-border text-primary mb-3" role="status">
                                <span className="visually-hidden">로딩 중...</span>
                            </div>
                            <p className="text-muted mb-2">AI가 경기를 분석하고 있습니다...</p>
                            <small className="text-muted">약 2~3분 소요됩니다</small>
                        </div>
                    )}

                    {summaryError && !summaryLoading && (
                        <div className="alert alert-danger d-flex align-items-center" role="alert">
                            <i className="fas fa-exclamation-circle me-2"></i>
                            <div>
                                <strong>요약 생성 실패</strong>
                                <div className="small">{summaryError}</div>
                            </div>
                            <button 
                                className="btn btn-sm btn-outline-danger ms-auto text-nowrap"
                                onClick={handleGenerateSummary}
                            >
                                재시도
                            </button>
                        </div>
                    )}

                    {summary && !summaryLoading && (
                        <div className="bg-white border border-gray border-radius-20 p-4 shadow-sm">
                            <div className="d-flex align-items-center mb-3">
                                <i className="fas fa-robot text-primary me-2"></i>
                                <small className="text-muted">AI가 생성한 경기 요약입니다</small>
                            </div>
                            <div style={{ whiteSpace: 'pre-line', lineHeight: '1.8' }}>
                                {summary}
                            </div>
                        </div>
                    )}

                    {!summary && !summaryLoading && !summaryError && (
                        <div className="bg-light border-radius-20 p-4 text-center">
                            <i className="fas fa-lightbulb fa-2x text-warning mb-3"></i>
                            <p className="text-muted mb-0">
                                AI가 경기 하이라이트 영상의 자막을 분석하여<br />
                                경기 개요, 주요 장면, MVP 등을 요약해드립니다.
                            </p>
                        </div>
                    )}
                </div>
            </div>
        </OverlayPage>
    </>
}