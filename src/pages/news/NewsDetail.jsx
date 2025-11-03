import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { OverlayPage } from "/components";
import { getNewsById } from "/services/news";
import { useInit } from "/context/InitContext";

export default function NewsDetail() {
    const { id } = useParams();
    const [news, setNews] = useState(null);
    const [loading, setLoading] = useState(true);
    const { activateChat, setQuestionType, setPlaceRedirect } = useInit();
    const [summary, setSummary] = useState(null);
    const [summaryLoading, setSummaryLoading] = useState(false);

    const handleSummaryClick = (e) => {
        e.preventDefault();
        e.stopPropagation();

        // 뉴스 요약 시에는 placeRedirect 초기화
        setPlaceRedirect(null);

        // 챗봇 열기 + 자동 메시지 전송
        // 실제 메시지: "뉴스 {id} 요약해줘", 화면 표시: "뉴스 요약해줘"
        setQuestionType('free');
        activateChat(true, `야구 뉴스 ${id} 요약해줘`, '뉴스 요약해줘');
    };
    
    useEffect(() => {
        getNewsById(id)
        .then(data => {
            setNews(data);
            // DB에 이미 요약이 있으면 표시
            if (data.summary) {
                setSummary(data.summary);
            }
        })
        .catch(e => console.log("뉴스 상세 조회 실패:", e))
        .finally(() => setLoading(false));
    }, [id]);

    if (loading) {
        return (
            <OverlayPage title="뉴스">
                <div className="overflow-y-auto p-4" style={{ height: '100%' }}>
                    <p className="text-gray">로딩 중...</p>
                </div>
            </OverlayPage>
        );
    }

    if (!news) {
        return (
            <OverlayPage title="뉴스">
                <div className="overflow-y-auto p-4" style={{ height: '100%' }}>
                    <p className="text-gray">뉴스를 찾을 수 없습니다.</p>
                </div>
            </OverlayPage>
        );
    }

    return (
        <>
            <OverlayPage title="뉴스">
                <div className="overflow-y-auto p-4" style={{ height: '100%' }}>
                    <article className="pb-3">
                        <h1 className="section_title mt-2 mb-0">{news.title}</h1>
                        <div className="d-flex align-items-center justify-content-between p-3 ps-0 pe-0">
                            {news.teamName && <p className="text-gray small m-0">{news.teamName}</p>}
                            <div className="d-flex gap-2">
                                {!summary && (
                                    <button 
                                        className="btn btn-outline-secondary btn-sm"
                                        onClick={handleSummaryClick}
                                        disabled={summaryLoading}
                                    >
                                        {summaryLoading ? (
                                            <>
                                                <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                                요약 중...
                                            </>
                                        ) : (
                                            <>
                                                <i className="fas fa-magic me-2"></i>
                                                AI 뉴스 요약
                                            </>
                                        )}
                                    </button>
                                )}
                                {news.link && (
                                    <a href={news.link} target="_blank" rel="noopener noreferrer" className="btn btn-outline-secondary btn-sm">
                                        KBO 원문 보기
                                    </a>
                                )}
                            </div>
                        </div>
                        {news.imageUrl && (
                            <img 
                                src={news.imageUrl} 
                                alt={news.title}
                                className="border-gray border-radius-12 mb-4"
                                style={{ width: '100%', objectFit: 'cover' }}
                                onError={(e) => e.target.style.display = 'none'}
                            />
                        )}

                        {news.content ? (
                            <div className="news-content" style={{ whiteSpace: 'pre-wrap', lineHeight: '1.8' }}>
                                {news.content}
                            </div>
                        ) : (
                            <p className="text-gray">본문 내용이 없습니다.</p>
                        )}
                    </article>
                </div>
            </OverlayPage>
        </>
    );
}

