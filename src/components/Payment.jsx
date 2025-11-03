//npm install @bootpay/client-js
import { Bootpay } from '@bootpay/client-js';
import axios from "/config/axios";

export const requestPayment = async (amount, method, user) => {
    try {
        // 1️⃣ 먼저 서버에 이상거래 여부 확인
        const checkResponse = await axios.post('/api/payment/fraud', {
            price: amount,
            userId: user.id,
            userName: user.name
        }, { withCredentials: true });

        if (!checkResponse.data.allow) {
            alert(checkResponse.data.message || '⚠️ 이상거래로 결제가 불가합니다.');
            return { success: false, error: 'fraud detected' };
        }

        const response = await Bootpay.requestPayment({
            application_id: "68d544dcb96306619f55ba75", // 어플리케이션 ID
            price: amount,
            order_name: `포인트 충전 ${amount.toLocaleString()}P`,
            order_id: "POINT_" + new Date(),
            user,   // 유저
            method, // 결제 방법
            items: [{
                    id: 'POINT_CHARGE',     // 상품 고유 ID (필수)
                    name: '포인트 충전',   // 상품명
                    qty: 1,                 // 수량
                    price: amount             // 단가
            }],
            extra: {
                "open_type": "iframe",
                display_success_result: true, // 결제 완료 후에 부트페이 자체 성공 페이지 표시 여부
                popup: true, // 결제창을 팝업으로 띄울지 여부 (false는 현재 페이지에서 이동)
                // custom_key: 'custom_value' // 자유롭게 추가할 수 있는 커스텀 데이터
            }
        });

        if (response.event === 'done') {
            // 결제 성공 시에만 백엔드 검증 진행
            const verifyResponse = await axios.post('/api/payment/verify', {
                receiptId : response.data.receipt_id,
                orderId : response.data.order_id,
                name : response.data.order_name,
                price : response.data.price,
                userId: user.id,
                userName: user.name
            },
            { headers: { 'Content-Type' : 'application/json'}, withCredentials:true });
            
            if (verifyResponse.data.success) {
                return { success: true, amount: amount };
            } else {
                alert(verifyResponse.data.message || '결제 검증 실패');
                return { success: false, error: verifyResponse.data.message };
            }
        } else if (response.event === 'cancel') {
            return { success: false, cancelled: true };
        } else if (response.event === 'error') {
            throw new Error(response.message || '결제 오류');
        }

    } catch (err) {
        console.error("결제 처리 오류:", err);
        return { success: false, error: err.message };
    }
};