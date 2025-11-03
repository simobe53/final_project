import { useNavigate, useOutletContext, useParams, useSearchParams } from 'react-router-dom';
import { useAuth } from '/context/AuthContext';
import { OverlayPage } from "/components";
import { Rank } from './components/Rank';
import { useMemo, useState } from 'react';
import axios from "/config/axios";

export default function CreatePlaceReview() {
    const [rank, setRank] = useState(null);
    const [comments, setComments] = useState("");
    const { id: placeId } = useParams();
    const [searchParams] = useSearchParams();
    const { setRanks } = useOutletContext();
    const { auth } = useAuth();
    const userId = auth.id;

    const message = useMemo(() => {
        switch (rank) {
            case 5: return <><b>5점</b> <span>(최고예요)</span></>;
            case 4.5: return <><b>4.5점</b> <span>(완전 만족했어요)</span></>;
            case 4: return <><b>4점</b> <span>(만족했어요)</span></>;
            case 3.5: return <><b>3.5점</b> <span>(좋아요)</span></>;
            case 3: return <><b>3점</b> <span>(보통이에요)</span></>;
            case 2.5: return <><b>2.5점</b> <span>(보통이에요)</span></>;
            case 2: return <><b>2점</b> <span>(그냥 그랬어요)</span></>;
            case 1.5: return <><b>1.5점</b> <span>(그냥 그랬어요)</span></>;
            case 1: return <><b>1점</b> <span>(별로였어요)</span></>;
            case 0.5: return <><b>0.5점</b> <span>(완전 별로였어요)</span></>;
            default: return <><b>0점</b> <span>(다신 안가고 싶어요)</span></>;
        }
    }, [rank]);

    const placename = searchParams.get('placename');
    const navigate = useNavigate();

    const onSubmit = () => {
        const form = {userId, placeId, rank, comments};
        axios.post("/api/ranks/create", form)
        .then(() => {
            alert("생성되었습니다!");
            setRanks(prev => [{ ...form, user: auth }, ...prev]);
            navigate(`/places/${placeId}`);
        })
        .catch((error) => {
            console.error("에러 발생:", error.response?.data || error.message);
            alert("리뷰 등록 중 문제가 발생했습니다.");
        });
    };

    return <>
        <OverlayPage title={`${placename} 리뷰 작성`}>
            <div className="d-flex flex-column align-items-stretch gap-20 pt-4" style={{ height: '100%' }}>
                <div className="d-flex flex-column align-items-center mt-4 mb-4 p-4">
                    <h5 className="mb-3">이 장소에 만족하셨나요?</h5>
                    <Rank size="50" onChange={setRank} value={rank} />
                    <p className="text-danger d-inline-flex gap-8 m-1">{rank && message}</p>
                </div>
                <div className="p-4 flex-grow">
                    <textarea className="form-control" name="comments" onChange={e => setComments(e.target.value)} rows={10} value={comments} style={{ height: '100%' }}></textarea>
                </div>
                <button className="btn btn-primary border-radius-0" style={{ height: 60 }} onClick={onSubmit}>
                    리뷰 등록하기
                </button>
            </div>
        </OverlayPage>
    </>
}
