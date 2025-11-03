import { useState } from 'react';
import axios from "/config/axios";
import { getDate } from '/components';
import ProfileImg from '/components/ProfileImg';
import { useAuth } from '/context/AuthContext';
import { Rank } from './Rank';

export default function PlaceReview({ id, comments, user, rank, createdAt, isMine }) {
    const { auth } = useAuth();
    const [deleted, setDeleted] = useState(false);
    const { name: username, profileImage } = user || {};

    const onDelete = () => {
        if (confirm("삭제하시겠습니까?")) {
            axios.delete("/api/ranks/delete", { params: { rankId: id } })
            alert("삭제되었습니다!");
            setDeleted(true);
        }
    };

    if (deleted) return null;
    return <>
        <li className="border-radius-20 pt-3 p-4 mt-1 mb-1 d-flex flex-column align-items-stretch gap-20" key={id} style={{ background: 'rgba(0,0,0,0.05)' }}>
            <div className="d-flex justify-content-between">
                <i className='fas fa-quote-left text-gray' style={{ fontSize: 40 }} />
                <Rank value={rank} readOnly size={20} />
            </div>
            
            <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{comments}</div>
            
            <div className="mt-3 d-flex align-items-center gap-8">
                {user && <>
                    <div>
                    <ProfileImg src={profileImage} zoom={0.35} />
                    </div>
                    <small>{username} 님이 작성</small>
                </>}
                {(isMine || auth.account === user?.account) && <button className="btn btn-sm btn-danger" onClick={onDelete} style={{ padding: "2px 6px", fontSize: 12 }}>삭제</button>}
                <small className="text-gray ms-auto">{getDate(createdAt)}</small>
            </div>
        </li>
    </>
}
