import { useEffect, useState } from "react";
import axios from "/config/axios";


/* 추천 플레이스 스크랩 버튼 */
export default function ScrapBtn({ placeId }) {
    const [isScrap, setIsScrap] = useState(false);

    useEffect(() => {
        if (!placeId) return;
        //찜한 장소인지 확인하고 버튼 표시
        axios.get("/api/scrap/exists", { params: { placeId } })
        .then(res => {
            const isScrapped = res.data;
            setIsScrap(isScrapped);
        });

    }, [placeId]);

    const onScrap = (e) => {
        e.preventDefault();
        /* TODO:: 스크랩 하기 / 취소 */
        axios.get("/api/scrap/toggle", { params: { placeId } })
        .then(res => {
            const isScrapped = res.data;
            setIsScrap(isScrapped);
        })
        e.stopPropagation();
    };

    return <>
    <button className={`btn border-radius-20 border bg-white border-${isScrap ? "primary" : "gray"}`} aria-label="스크랩하기" onClick={onScrap}>
        <i className={`fas fa-bookmark ${isScrap ? "point" : "text-gray"}`} />
    </button>
    </>
}
