import { useState } from 'react';
import ProfileImg from '/components/ProfileImg';
import { useAuth } from '/context/AuthContext';
import { deleteComment } from '/services/feeds';
import { getDate } from '/components';

export default function Comment({ id, content, user, createdAt, onCommentDeleted }) {
    const { auth } = useAuth();
    const { id: userId, name: username, profileImage } = user;
    const [isDeleting, setIsDeleting] = useState(false);
    
    const isMyComment = auth.id === userId;
    
    const handleDelete = async () => {
        if (!confirm('댓글을 삭제하시겠습니까?')) {
            return;
        }
        
        setIsDeleting(true);
        try {
            await deleteComment(id, auth.id);
            if (onCommentDeleted) {
                onCommentDeleted();
            }
        } catch (error) {
            console.error('댓글 삭제 실패:', error);
            alert('댓글 삭제에 실패했습니다.');
        } finally {
            setIsDeleting(false);
        }
    };

    return <>
        <li className="p-2 ps-3 border-bottom border-gray d-flex align-items-start gap-8 position-relative comment" key={id}>
            <div className="d-flex align-items-center gap-8" style={{ height: 30 }}>
                <ProfileImg small src={profileImage} zoom={0.6} />
                <small className="text-nowrap" style={{ lineHeight: 1 }}>{username}</small>
            </div>
            <div className="flex-grow small p-1" style={{ maxWidth: 400, }}>
                {content}
            </div>
            <div className="small text-center" style={{ minWidth: 70, paddingTop: 2, fontSize: 13, lineHeight: 1 }}>
                <small className="text-gray text-nowrap">{getDate(createdAt, { nowrap: false })}</small>
            </div>
            {isMyComment && <>
                <button 
                    className="delete btn btn-danger"
                    onClick={handleDelete}
                    disabled={isDeleting}
                >
                    삭제하기
                </button>
            </>}
        </li>
    </>
}
