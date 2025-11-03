import { useState } from 'react';
import { useAuth } from '/context/AuthContext';
import { useParams } from 'react-router-dom';
import { createComment } from '/services/feeds';

export default function FeedCommentWrite({ onCommentAdded }) {
    const { id: feedId } = useParams();
    const { auth } = useAuth();
    const [content, setContent] = useState('');
    
    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (!content.trim()) {
            alert('댓글 내용을 입력해주세요.');
            return;
        }
        
        if (!auth.id) {
            alert('로그인이 필요합니다.');
            return;
        }
        
        try {
            await createComment(feedId, {
                content: content.trim(),
                user: { id: auth.id }
            });
            setContent('');
            if (onCommentAdded) {
                onCommentAdded();
            }
        } catch (error) {
            console.error('댓글 작성 실패:', error);
            alert('댓글 작성에 실패했습니다.');
        }
    };

    return <>
        <div className="p-2 ps-3 border-top border-gray position-absolute bg-white" style={{ width: '100%', bottom: 0, left: 0 }}>
            <form className="d-flex small pt-1 gap-8" onSubmit={handleSubmit}>
                <input 
                    type="text" 
                    name="content" 
                    className="form-control border-radius-20" 
                    style={{ fontSize: 14 }}
                    value={content}
                    onChange={(e) => setContent(e.target.value)}
                    placeholder="댓글을 입력하세요..."
                />
                <button 
                    type="submit"
                    className="btn btn-primary btn-sm text-nowrap p-2 border-radius-20" 
                    style={{ minWidth: 40, height: 40 }}
                >
                    <i className="fas fa-paper-plane" style={{ fontSize: 16 }} />
                </button>
            </form>
        </div>
    </>
}
