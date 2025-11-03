import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '/context/AuthContext';
import axios from '/config/axios';
import './Complete.scss';

export default function PaymentComplete() {
    const navigate = useNavigate();
    const { fetchAuth } = useAuth();
    const [paymentInfo, setPaymentInfo] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // 사용자 정보 새로고침 함수
        const refreshUserInfo = async () => {
            try {
                const response = await axios.get('/api/auth/login', {
                    withCredentials: true
                });
                if (response.status === 200) {
                    fetchAuth(response.data);
                    console.log('✅ 포인트 정보가 업데이트되었습니다.');
                }
            } catch (error) {
                console.error('사용자 정보 새로고침 실패:', error);
            }
        };

        // sessionStorage에서 결제 정보 가져오기
        const paymentData = sessionStorage.getItem('paymentComplete');
        
        if (!paymentData) {
            // 결제 정보가 없으면 홈으로 리다이렉트
            console.error('결제 정보가 없습니다.');
            navigate('/');
            return;
        }

        try {
            const data = JSON.parse(paymentData);
            setPaymentInfo(data);
            
            // 데이터를 읽은 후 sessionStorage 정리
            sessionStorage.removeItem('paymentComplete');
            
            // 사용자 정보 새로 불러오기 (포인트 업데이트 반영)
            refreshUserInfo();
        } catch (error) {
            console.error('결제 정보 파싱 실패:', error);
            navigate('/');
        } finally {
            setLoading(false);
        }
    }, [navigate, fetchAuth]);

    const handleGoToMyPage = () => {
        navigate('/mypage');
    };

    if (loading) {
        return (
            <div className="payment-complete-container">
                <div className="verification-loading">
                    <div className="spinner"></div>
                    <h2>결제 정보 확인 중...</h2>
                    <p>잠시만 기다려주세요.</p>
                </div>
            </div>
        );
    }

    if (!paymentInfo) {
        return null; // 이미 navigate로 리다이렉트되었으므로 null 반환
    }

    return (
        <div className="payment-complete-container">
            <div className="payment-success">
                <div className="icon-success">✓</div>
                <h2>결제가 완료되었습니다!</h2>
                
                <div className="payment-details">
                    <div className="detail-row">
                        <span className="label">충전 포인트</span>
                        <span className="value point">
                            {paymentInfo?.amount?.toLocaleString()}P
                        </span>
                    </div>
                    <div className="detail-row">
                        <span className="label">결제 금액</span>
                        <span className="value">
                            {paymentInfo?.amount?.toLocaleString()}원
                        </span>
                    </div>
                    <div className="detail-row">
                        <span className="label">결제 수단</span>
                        <span className="value">
                            {paymentInfo?.paymentMethod === 'kakao' ? '카카오페이' : 
                             paymentInfo?.paymentMethod === 'naver' ? '네이버페이' : 
                             paymentInfo?.paymentMethod === 'card' ? '신용카드' : 
                             paymentInfo?.itemName || '포인트 충전'}
                        </span>
                    </div>
                    <div className="detail-row">
                        <span className="label">주문 번호</span>
                        <span className="value order-id">
                            {paymentInfo?.orderId}
                        </span>
                    </div>
                </div>

                <div className="button-group">
                    <button className="btn-mypage" onClick={handleGoToMyPage}>
                        내 포인트 확인하기
                    </button>
                    <button className="btn-home" onClick={() => navigate('/')}>
                        홈으로 가기
                    </button>
                </div>
            </div>
        </div>
    );
}