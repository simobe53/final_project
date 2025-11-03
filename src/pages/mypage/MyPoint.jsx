import { useState, useEffect } from "react";
import { useAuth } from "/context/AuthContext";
import { requestPayment } from '/components/Payment.jsx'; // 결제용 컴포넌트
import axios from "/config/axios";
import './MyPoint.scss';

export default function MyPoint() {
    const { auth } = useAuth();
    const [point, setPoint] = useState(0);
    const [showChargeModal, setShowChargeModal] = useState(false);
    const [showHistoryModal, setShowHistoryModal] = useState(false);
    const [chargeAmount, setChargeAmount] = useState(0);
    const [paymentHistory, setPaymentHistory] = useState([]);

    const [selectedAmount, setSelectedAmount] = useState(null);
    const [selectedPaymentMethod, setSelectedPaymentMethod] = useState(null);
    
    const MAX_CHARGE_AMOUNT = 500000; // 최대 충전 금액

    // 포인트 조회
    const fetchPoint = async () => {
        if (auth?.id) {
            try {
                const response = await axios.get(`/api/users/${auth.id}/point`);
                setPoint(response.data.point || 0);
            } catch (error) {
                console.error("포인트 조회 실패:", error);
            }
        }
    };

    // 포인트 내역 조회
    const fetchPaymentHistory = async () => {
        if (auth?.id) {
            try {
                const response = await axios.get(`/api/payment/history/${auth.id}`);
                setPaymentHistory(response.data || []);
            } catch (error) {
                console.error("포인트 내역 조회 실패:", error);
            }
        }
    };

    // 컴포넌트 마운트 시 포인트 조회
    useEffect(() => {
        fetchPoint();
    }, [auth?.id]);

    // 포인트 클릭 이벤트
    const handlePointClick = () => {
        fetchPaymentHistory();
        setShowHistoryModal(true);
    };

    // 내역 모달 닫기
    const handleCloseHistoryModal = () => {
        setShowHistoryModal(false);
    };

    const handleChargeClick = () => {
        setShowChargeModal(true);
    };

    const handleCloseModal = () => {
        setShowChargeModal(false);
        setChargeAmount(0);

        setSelectedAmount(null);

    };

    const handleAmountChange = (e) => {
        const value = parseInt(e.target.value.replace(/,/g, '')) || 0;
        
        if (value > MAX_CHARGE_AMOUNT) {
            alert(`최대 충전 금액은 ${MAX_CHARGE_AMOUNT.toLocaleString()}원입니다.`);
            return;
        }
        
        setChargeAmount(value);
        setSelectedAmount(null); // 직접 입력 시 선택 해제
    };

    const handleSetAmount = (amount) => {
        if (amount > MAX_CHARGE_AMOUNT) {
            alert(`최대 충전 금액은 ${MAX_CHARGE_AMOUNT.toLocaleString()}원입니다.`);
            return;
        }
        
        setChargeAmount(amount);
        setSelectedAmount(amount); // 선택된 금액 저장
    };

    const handleResetAmount = () => {
        setChargeAmount(0);
        setSelectedAmount(null); // 초기화 시 선택 해제

    };

    const handlePaymentMethodSelect = (method) => {
        setSelectedPaymentMethod(method);
    };

    const handleProceedPayment = async () => {
        if (chargeAmount < 1000) {
            alert('최소 충전 금액은 1,000원입니다.');
            return;
        }

        if (chargeAmount > MAX_CHARGE_AMOUNT) {
            alert(`최대 충전 금액은 ${MAX_CHARGE_AMOUNT.toLocaleString()}원입니다.`);
            return;
        }

        if (!selectedPaymentMethod) {
            alert('충전수단을 선택해주세요.');
            return;
        }
        
        handleCloseModal();
        try {
            const result = await requestPayment(chargeAmount, selectedPaymentMethod, auth);
            
            if (result?.success) {
                // 결제 성공: 약간의 딜레이 후 포인트 갱신 (트랜잭션 커밋 대기)
                setTimeout(async () => {
                    await fetchPoint();
                    alert(`${chargeAmount.toLocaleString()}P 충전이 완료되었습니다!`);
                }, 500);
            } else if (result?.cancelled) {
                alert('결제가 취소되었습니다.');
            } else {
                alert('결제 처리 중 오류가 발생했습니다.');
            }
        } catch (error) {
            console.error("결제 처리 중 오류:", error);
            alert('결제 처리 중 오류가 발생했습니다.');
        }
    };

    return <>
    <div className="d-flex gap-20 border-bottom border-gray align-items-center p-3 ps-4 pe-4 m-3 mt-1 border-radius-12 bg-white">
        <span>나의 포인트 :</span> 
        <b className="h5 point m-0 clickable-point" onClick={handlePointClick} title="포인트 내역 보기">
            {point.toLocaleString()} P
        </b>
        <button className="ms-auto btn btn-sm btn-outline-secondary border-radius-20" onClick={(e) => { e.stopPropagation(); handleChargeClick(); }}> 충전하기</button>
    </div>

        {/* 포인트 충전 모달 */}
        {showChargeModal && (
            <div className="point-charge-modal-overlay" onClick={handleCloseModal}>
                <div className="point-charge-modal" onClick={(e) => e.stopPropagation()}>
                    <div className="modal-header">
                        <button className="back-button" onClick={handleCloseModal}>
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                                <path d="M15 18L9 12L15 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                            </svg>
                        </button>
                        <h2 className="modal-title">포인트 충전</h2>
                    </div>

                    <div className="modal-content">
                        <div className="charge-section">
                            <div className="section-header">
                                <span className="section-title">충전금액</span>
                                <span className="current-points">충전 가능 금액 <span className="amount">{MAX_CHARGE_AMOUNT.toLocaleString()}</span></span>
                            </div>
                            
                            <div className="amount-input-wrapper">
                                <input 
                                    type="text" 
                                    className="amount-input" 
                                    value={chargeAmount ? chargeAmount.toLocaleString() : ''}
                                    onChange={handleAmountChange}
                                    placeholder="0"
                                />
                                <span className="input-suffix">P</span>
                            </div>
                            <p className="input-hint">충전할 포인트 입력 (1,000P 단위 입력 가능)</p>

                            <div className="quick-amount-buttons">
                                <button className={`quick-button ${selectedAmount === 1000 ? 'selected' : ''}`} onClick={() => handleSetAmount(1000)}>1천P</button>
                                <button className={`quick-button ${selectedAmount === 3000 ? 'selected' : ''}`} onClick={() => handleSetAmount(3000)}>3천P</button>
                                <button className={`quick-button ${selectedAmount === 5000 ? 'selected' : ''}`} onClick={() => handleSetAmount(5000)}>5천P</button>
                                <button className={`quick-button ${selectedAmount === 10000 ? 'selected' : ''}`} onClick={() => handleSetAmount(10000)}>1만P</button>
                                <button className="quick-button reset" onClick={handleResetAmount} title="초기화">
                                    <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                                        <path d="M1 4V10H7" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                        <path d="M23 20V14H17" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                        <path d="M20.49 9C19.9828 7.56678 19.1209 6.28542 17.9845 5.27542C16.8482 4.26541 15.4745 3.55976 13.9917 3.22426C12.5089 2.88875 10.9652 2.93434 9.50481 3.35677C8.04437 3.77921 6.71475 4.56471 5.64 5.64L1 10M23 14L18.36 18.36C17.2853 19.4353 15.9556 20.2208 14.4952 20.6432C13.0348 21.0657 11.4911 21.1112 10.0083 20.7757C8.52547 20.4402 7.1518 19.7346 6.01547 18.7246C4.87913 17.7146 4.01717 16.4332 3.51 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                                    </svg>
                                </button>
                            </div>
                        </div>

                        <div className="payment-method-section">
                            <div className="section-header">
                                <span className="section-title">충전수단 선택</span>
                                <svg width="16" height="16" viewBox="0 0 16 16" fill="none" className="info-icon">
                                    <circle cx="8" cy="8" r="7" stroke="currentColor" strokeWidth="1.5"/>
                                    <path d="M8 7V11M8 5V5.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round"/>
                                </svg>
                            </div>

                            <div className="payment-methods">
                                <div 
                                    className={`payment-method-item ${selectedPaymentMethod === 'kakao' ? 'selected' : ''}`}
                                    onClick={() => handlePaymentMethodSelect('kakao')}
                                >
                                    <div className="method-icon kakao">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                            <path d="M12 3C6.5 3 2 6.6 2 11c0 2.8 1.9 5.3 4.8 6.7-.2.7-.6 2.1-.7 2.5 0 .3.1.6.4.7.2.1.5 0 .7-.1.4-.3 2.4-1.6 3.5-2.3.4.1.9.1 1.3.1 5.5 0 10-3.6 10-8S17.5 3 12 3z"/>
                                        </svg>
                                    </div>
                                    <span className="method-name">카카오페이</span>
                                    <svg className="arrow-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
                                        <path d="M7.5 5L12.5 10L7.5 15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                                    </svg>
                                </div>

                                <div 
                                    className={`payment-method-item ${selectedPaymentMethod === 'naver' ? 'selected' : ''}`}
                                    onClick={() => handlePaymentMethodSelect('naver')}
                                >
                                    <div className="method-icon naver">
                                        <span className="naver-n">N</span>
                                    </div>
                                    <span className="method-name">네이버페이</span>
                                    <svg className="arrow-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
                                        <path d="M7.5 5L12.5 10L7.5 15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                                    </svg>
                                </div>

                                <div 
                                    className={`payment-method-item ${selectedPaymentMethod === 'card' ? 'selected' : ''}`}
                                    onClick={() => handlePaymentMethodSelect('card')}
                                >
                                    <div className="method-icon card">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                                            <rect x="2" y="5" width="20" height="14" rx="2" stroke="currentColor" strokeWidth="1.5"/>
                                            <path d="M2 9H22" stroke="currentColor" strokeWidth="1.5"/>
                                        </svg>
                                    </div>
                                    <span className="method-name">신용카드</span>
                                    <svg className="arrow-icon" width="20" height="20" viewBox="0 0 20 20" fill="none">
                                        <path d="M7.5 5L12.5 10L7.5 15" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
                                    </svg>
                                </div>
                            </div>
                        </div>

                        <button 
                            className="proceed-button" 
                            onClick={handleProceedPayment}
                            disabled={chargeAmount < 1000 || chargeAmount > MAX_CHARGE_AMOUNT || !selectedPaymentMethod}
                        >
                            {chargeAmount > 0 ? `${chargeAmount.toLocaleString()}P 충전하기` : '충전하기'}
                        </button>
                    </div>
                </div>
    </div>
        )}

        {/* 포인트 내역 모달 */}
        {showHistoryModal && (
            <div className="point-charge-modal-overlay" onClick={handleCloseHistoryModal}>
                <div className="point-charge-modal" onClick={(e) => e.stopPropagation()}>
                    <div className="modal-header">
                        <button className="back-button" onClick={handleCloseHistoryModal}>
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                                <path d="M15 18L9 12L15 6" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
                            </svg>
                        </button>
                        <h2 className="modal-title">포인트 충전 내역</h2>
                    </div>

                    <div className="modal-content">
                        {paymentHistory.length === 0 ? (
                            <div className="empty-history">
                                <p>포인트 충전 내역이 없습니다.</p>
                            </div>
                        ) : (
                            <div className="history-list">
                                {paymentHistory.map((item) => (
                                    <div key={item.id} className="history-item">
                                        <div className="history-header">
                                            <span className="history-title">{item.itemName}</span>
                                            <span className="history-amount">+{item.price.toLocaleString()}P</span>
                                        </div>
                                        <div className="history-details">
                                            <span className="history-date">
                                                {new Date(item.createdAt).toLocaleString('ko-KR')}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        )}
</>
}
