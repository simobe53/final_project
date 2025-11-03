import { useState, useEffect, useRef } from 'react';
import axios from '/config/axios';
import BaseModal from './BaseModal';
import usePasswordlessWebSocket from './hooks/usePasswordlessWebSocket';

export default function PasswordlessRegistrationModal({ account, onClose, onSuccess }) {
    const [step, setStep] = useState('qr'); // qr, complete
    const [qrCode, setQrCode] = useState('');
    const [sessionId, setSessionId] = useState('');
    const [loading, setLoading] = useState(false);
    const { connect, disconnect } = usePasswordlessWebSocket();
    const stepRef = useRef(step); // step의 최신 값을 추적

    // step 변경 시 ref 업데이트
    useEffect(() => {
        stepRef.current = step;
    }, [step]);

    // 바로 QR 생성 시작
    useEffect(() => {
        startPasswordlessRegistration();
    }, [account]);

    // Passwordless 등록 시작 - 바로 QR 생성
    const startPasswordlessRegistration = async () => {
        setLoading(true);
        try {
            // QR 코드 생성 요청 (세션에서 검증)
            const { data: registerData } = await axios.post('/api/auth/passwordless/register', {
                account
            });

            if (registerData.success) {
                setQrCode(registerData.qr);
                setSessionId(registerData.sessionId);
                setStep('qr');

                // WebSocket 연결
                if (registerData.wsUrl && registerData.pushConnectorToken) {
                    connectWebSocket(registerData.wsUrl, registerData.pushConnectorToken, registerData.sessionId);
                }
            } else {
                alert(registerData.message || 'QR 코드 생성에 실패했습니다.');
                onClose();
            }
        } catch (error) {
            alert('QR 코드 생성 중 오류가 발생했습니다.');
            onClose();
        } finally {
            setLoading(false);
        }
    };

    // WebSocket 연결
    const connectWebSocket = (wsUrl, pushConnectorToken, sessionId) => {
        connect(wsUrl, pushConnectorToken, async (result) => {
            // 등록 완료 확인
            await checkRegistrationComplete(sessionId);
        });

        // 3분 타임아웃
        setTimeout(() => {
            if (stepRef.current === 'qr') {
                alert('등록 시간이 초과되었습니다.');
                onClose();
            }
        }, 180000);
    };

    // 등록 완료 확인
    const checkRegistrationComplete = async (sessionId) => {
        try {
            const { data } = await axios.post('/api/auth/passwordless/register/status', {
                sessionId
            });

            if (data.success) {
                // WebSocket 연결 종료
                disconnect();
                setStep('complete');
            }
        } catch (error) {
            console.error('Registration complete check failed:', error);
        }
    };

    // 모달 닫기
    const handleClose = () => {
        disconnect();
        onClose();
    };

    // 완료 후 로그인 화면으로
    const handleCompleteClose = () => {
        handleClose();
        if (onSuccess) {
            onSuccess();
        }
    };

    return (
        <BaseModal
            title={step === 'qr' ? 'QR 코드 스캔' : 'Passwordless 등록 완료'}
            onClose={handleClose}
            showCloseButton={step !== 'complete'}
        >
            <div className="modal-body">
                        {step === 'qr' && (loading ? (
                            <div className="text-center py-5">
                                <div className="spinner-border text-primary" role="status">
                                    <span className="visually-hidden">로딩중...</span>
                                </div>
                                <p className="mt-3">QR 코드 생성 중...</p>
                            </div>
                        ) : (
                            <div className="text-center">
                                <h6>모바일 앱에서 QR 코드를 스캔하세요</h6>
                                <img
                                    src={qrCode}
                                    alt="QR Code"
                                    style={{ width: 200, height: 200 }}
                                    className="my-3"
                                />
                                <p className="text-muted">
                                    Passwordless X1280 앱을 사용하여 등록하세요
                                </p>
                                <button
                                    className="btn btn-outline-secondary mt-3"
                                    onClick={handleClose}
                                >
                                    취소
                                </button>
                            </div>
                        ))}

                        {step === 'complete' && (
                            <div className="text-center py-3">
                                <div className="mb-4">
                                    <i className="bi bi-check-circle text-success" style={{ fontSize: '4rem' }}></i>
                                </div>
                                <p className="mb-2">
                                    <strong>Passwordless 서비스가 등록되었습니다.</strong>
                                </p>
                                <p className="mb-4">
                                    안전하고 편리한 Passwordless X1280 앱으로 로그인하세요.
                                </p>
                                <div className="alert alert-warning text-start">
                                    <p className="mb-2">
                                        <strong>Passwordless 서비스가 등록되어<br/>
                                        비밀번호가 임의의 값으로 변경되었습니다.</strong>
                                    </p>
                                    <p className="mb-0 small">
                                        아이디/비밀번호로 다시 로그인 하시려면
                                        회원정보 수정에서 passwordless설정을 해지하셔야 합니다.
                                    </p>
                                </div>
                                <button
                                    className="btn btn-primary px-5 mt-3"
                                    onClick={handleCompleteClose}
                                >
                                    확인
                                </button>
                            </div>
                        )}
            </div>
        </BaseModal>
    );
}
