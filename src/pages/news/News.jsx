import { useEffect, useState, useRef } from "react";
import { useAuth } from "/context/AuthContext";
import { getMyTeamNews, getLatestNews } from "/services/news";
import NewsListItem from "./components/NewsListItem";

export default function News() {
    const { auth } = useAuth();
    const [news, setNews] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadingMore, setLoadingMore] = useState(false);
    const [filter, setFilter] = useState('my-team'); // 'my-team' or 'all'
    const [hasMore, setHasMore] = useState(true); // 더 불러올 뉴스가 있는지
    const [offset, setOffset] = useState(0);
    const scrollContainerRef = useRef(null);
    const isLoadingRef = useRef(false); // 중복 요청 방지

    // 필터나 팀이 변경되면 처음부터 다시 로드
    useEffect(() => {
        setNews([]);
        setOffset(0);
        setHasMore(true);
        isLoadingRef.current = false; // 로딩 상태 초기화
        loadNews(0);
    }, [filter, auth?.team]);

    const loadNews = (currentOffset) => {
        // 이미 로딩 중이면 중복 요청 방지
        if (isLoadingRef.current) {
            return;
        }
        
        isLoadingRef.current = true;
        
        if (currentOffset === 0) {
            setLoading(true);
        } else {
            setLoadingMore(true);
        }
        
        const fetchFunc = filter === 'my-team' 
            ? getMyTeamNews(20, currentOffset) 
            : getLatestNews(20, currentOffset);
        
        fetchFunc
            .then(data => {
                if (data && data.length > 0) {
                    setNews(prev => {
                        if (currentOffset === 0) return data;
                        
                        // 중복 제거: 기존 뉴스 ID 목록
                        const existingIds = new Set(prev.map(item => item.id));
                        const newItems = data.filter(item => !existingIds.has(item.id));
                        
                        return [...prev, ...newItems];
                    });
                    setOffset(currentOffset + data.length);
                    // 받아온 데이터가 20개 미만이면 더 이상 없음
                    if (data.length < 20) {
                        setHasMore(false);
                    }
                } else {
                    setHasMore(false);
                }
            })
            .catch(error => {
                console.error('뉴스 조회 실패:', error);
                if (currentOffset === 0) {
                    setNews([]);
                }
            })
            .finally(() => {
                setLoading(false);
                setLoadingMore(false);
                isLoadingRef.current = false;
            });
    };

    // 무한 스크롤: 스크롤 이벤트로 직접 감지
    useEffect(() => {
        const container = scrollContainerRef.current;
        if (!container) return;

        const handleScroll = () => {
            const { scrollTop, scrollHeight, clientHeight } = container;
            const scrolledToBottom = scrollHeight - scrollTop - clientHeight < 100; // 하단 100px 이내
            
            if (scrolledToBottom && !loading && !loadingMore && hasMore) {
                loadNews(offset);
            }
        };

        container.addEventListener('scroll', handleScroll);
        return () => container.removeEventListener('scroll', handleScroll);
    }, [loading, loadingMore, hasMore, offset])

    return <>
        {/* 필터 버튼 */}
        {auth?.team && <div className="d-flex justify-content-center border-bottom">
            <div className="btn-group p-2 m-auto" style={{ width: 250, justifySelf: 'center' }} role="group">
                <button className={`btn btn-sm ${filter === 'my-team' ? "btn-primary" : "border border-gray"}`} onClick={() => setFilter('my-team')}>우리팀 뉴스</button>
                <button className={`btn btn-sm ${filter === 'all' ? "btn-primary" : "border border-gray "}`} onClick={() => setFilter('all')}>전체 뉴스</button>
            </div>
        </div>}
        
        <div ref={scrollContainerRef} className="overflow-y-auto p-3" style={{ height: 'calc(100% - 40px)' }}>
            {/* 뉴스 리스트 */}
            <div className="d-flex flex-column gap-8" style={{ paddingBottom: '40px' }}>
                {loading ? (
                    <p className="text-gray small">뉴스를 불러오는 중...</p>
                ) : news.length === 0 ? (
                    <p className="text-gray small">뉴스가 없습니다.</p>
                ) : (
                    <>
                        {news.map(item => <NewsListItem key={item.id} {...item} />)}
                        {loadingMore && (
                            <p className="text-gray small text-center mt-3 mb-3">더 불러오는 중...</p>
                        )}
                        {!hasMore && news.length > 0 && (
                            <p className="text-gray small text-center mt-3 mb-3">모든 뉴스를 불러왔습니다.</p>
                        )}
                    </>
                )}
            </div>
        </div>
    </>;
}


