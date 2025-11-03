import { useEffect, useState } from 'react';
import { useAuth } from "/context/AuthContext";
import StatusBar from '/components/StatusBar';
import { getMyPlaceReviews } from '/services/my';
import PlaceReview from '/pages/place/components/Review';
import Empty from '/components/Empty';

export default function MyReview () {
    const { auth } = useAuth();
    const [reviews, setReviews] = useState([]);

    useEffect(() => {
        getMyPlaceReviews(auth.id).then(data => setReviews(data));
    }, []);
    return <>
        <StatusBar title="내가 작성한 리뷰글" />
        {reviews.length == 0 ? <Empty message="리뷰글이 없습니다" /> :
        <ul className="d-flex flex-column gap-8 p-4 overflow-y-auto" style={{ maxHeight: '100%' }}>
            {reviews.map(({ place, ...review }) => <li key={review.id}>
                <div key={`${review.id}_place`} className="d-flex gap-20 mb-3 align-items-center">
                    <div style={{ width: 60 }}>
                        <div className="image-square" style={{ backgroundImage: `url('${place.image}')` }} />
                    </div>
                    <div>
                        <p className="h6 mb-1">{place.name}</p>
                        <p className="text-gray small">{place.address}</p>
                    </div>
                </div>
                <ul>
                    <PlaceReview {...review} isMine />
                </ul>
            </li>)}
        </ul>
        }
    </>
}