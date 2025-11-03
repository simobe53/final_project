import { useState, useRef, useEffect } from 'react';
import axios from '/config/axios';
import { useAuth } from '/context/AuthContext';
import BaseModal from '/components/BaseModal';
import usePasswordlessWebSocket from '../../components/hooks/usePasswordlessWebSocket';

export default function PasswordlessManageBtn() {
    const { auth } = useAuth();
    const [showModal, setShowModal] = useState(false);
    const [isRegistered, setIsRegistered] = useState(null);
    const [loading, setLoading] = useState(false);
    const [password, setPassword] = useState('');
    const [qrCode, setQrCode] = useState('');
    const [sessionId, setSessionId] = useState('');
    const [passwordlessToken, setPasswordlessToken] = useState('');
    const [status, setStatus] = useState(''); // idle, registering, unregistering
    const { connect, disconnect } = usePasswordlessWebSocket();
    const pollingInterval = useRef(null);
    const statusRef = useRef(status); // status의 최신 값을 추적

    // status 변경 시 ref 업데이트
    useEffect(() => {
        statusRef.current = status;
    }, [status]);

    // Passwordless 관리 모달 열기
    const openManageModal = () => {
        setIsRegistered(null);
        setShowModal(true);
    };

    // WebSocket 연결
    const connectWebSocket = (wsUrl, pushConnectorToken, sessionId) => {
        console.log('WebSocket 연결 시도:', wsUrl);
        console.log('pushConnectorToken:', pushConnectorToken);

        connect(wsUrl, pushConnectorToken, async (result) => {
            console.log('등록 완료 확인됨 - 백엔드에 완료 처리 요청');

            // 백엔드에 등록 완료 알림
            try {
                const response = await axios.post('/api/auth/passwordless/register/status', {
                    sessionId: sessionId
                });

                if (response.data.success) {
                    alert('Passwordless 등록이 완료되었습니다.');
                    setIsRegistered(true);
                    setStatus('');
                    setQrCode('');
                    setPassword('');
                } else {
                    alert('등록 완료 처리 중 오류가 발생했습니다.');
                }
            } catch (error) {
                console.error('등록 완료 처리 실패:', error);
                alert('등록 완료 처리 중 오류가 발생했습니다.');
            }

            // WebSocket 연결 종료
            disconnect();
        });

        // 3분 타임아웃 설정
        setTimeout(() => {
            if (statusRef.current === 'registering') {
                alert('등록 시간이 초과되었습니다.');
                setStatus('');
                setQrCode('');
            }
        }, 180000);
    };

    // Passwordless 관리 - 비밀번호 검증 및 토큰 생성
    const handleManageCheck = async () => {
        if (!password) {
            alert('비밀번호를 입력하세요.');
            return;
        }

        setLoading(true);
        try {
            // Step 1: 비밀번호 검증 및 토큰 생성
            const { data } = await axios.post('/api/auth/passwordless/manage-check', {
                account: auth.account,
                password
            });

            if (data.success) {
                const token = data.PasswordlessToken;
                setPasswordlessToken(token);
                console.log('PasswordlessToken received:', token);

                // Step 2: 등록 여부 확인
                const statusResponse = await axios.get(`/api/auth/passwordless/status?account=${auth.account}`);
                const registered = statusResponse.data.registered;
                setIsRegistered(registered);

                if (registered) {
                    // 이미 등록됨 → 해제 화면
                    console.log('Already registered - Show unregister screen');
                } else {
                    // 미등록 → QR 등록 화면으로 이동
                    console.log('Not registered - Starting registration');
                    await startRegistration(token);
                }
            } else {
                alert(data.message || '비밀번호 검증 실패');
            }
        } catch (error) {
            alert('비밀번호 검증 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    // Passwordless 등록 시작
    const startRegistration = async (token) => {
        try {
            // Step 3: QR 코드 생성 요청 (토큰 포함)
            const { data } = await axios.post('/api/auth/passwordless/register', {
                account: auth.account,
                token: token
            });

            if (data.success) {
                setQrCode(data.qr);
                setSessionId(data.sessionId);
                setStatus('registering');

                // WebSocket 연결 (pushConnector)
                if (data.wsUrl && data.pushConnectorToken) {
                    connectWebSocket(data.wsUrl, data.pushConnectorToken, data.sessionId);
                } else {
                    // WebSocket이 없으면 폴링 사용
                    pollRegistrationStatus(data.sessionId);
                }
            } else {
                alert(data.message || '등록 요청 실패');
            }
        } catch (error) {
            console.error('Registration failed:', error);
            alert('Passwordless 등록 요청 중 오류가 발생했습니다.');
        }
    };

    // 등록 상태 폴링
    const pollRegistrationStatus = (sessionId) => {
        pollingInterval.current = setInterval(async () => {
            try {
                const { data } = await axios.post('/api/auth/passwordless/register/status', {
                    sessionId
                });

                if (data.success) {
                    clearInterval(pollingInterval.current);
                    pollingInterval.current = null;
                    alert('Passwordless 등록이 완료되었습니다.');
                    setIsRegistered(true);
                    setStatus('');
                    setQrCode('');
                    setPassword('');
                }
            } catch (error) {
                // 폴링 중 에러는 무시
            }
        }, 2000);

        // 3분 후 자동 종료
        setTimeout(() => {
            if (pollingInterval.current) {
                clearInterval(pollingInterval.current);
                pollingInterval.current = null;
                if (statusRef.current === 'registering') {
                    alert('등록 시간이 초과되었습니다.');
                    setStatus('');
                    setQrCode('');
                }
            }
        }, 180000);
    };

    // Passwordless 해지
    const handleUnregister = async () => {
        if (!password) {
            alert('비밀번호를 입력하세요.');
            return;
        }

        if (!confirm('정말로 Passwordless를 해지하시겠습니까?')) {
            return;
        }

        setLoading(true);
        try {
            const { data } = await axios.post('/api/auth/passwordless/unregister', {
                account: auth.account,
                password
            });

            if (data.success) {
                alert('Passwordless가 해지되었습니다.');
                setIsRegistered(false);
                setPassword('');
            } else {
                alert(data.message || '해지 실패');
            }
        } catch (error) {
            alert('Passwordless 해지 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const closeModal = () => {
        // WebSocket 연결 종료
        disconnect();

        // 폴링 인터벌 정리
        if (pollingInterval.current) {
            clearInterval(pollingInterval.current);
            pollingInterval.current = null;
        }

        setShowModal(false);
        setPassword('');
        setQrCode('');
        setStatus('');
    };

    // 컴포넌트 언마운트시 정리
    useEffect(() => {
        return () => {
            if (pollingInterval.current) {
                clearInterval(pollingInterval.current);
                pollingInterval.current = null;
            }
        };
    }, []);

    return <>
        <button
            type="button"
            className="btn border-radius-0 p-0 w-100"
            onClick={openManageModal}
            style={{ height: '100%' }}
        >
            Passwordless 관리
        </button>

        {showModal && (
            <BaseModal
                title="Passwordless 관리"
                onClose={closeModal}
            >
                <div className="modal-body">
                            {status === 'registering' && qrCode ? (
                                // QR 코드 표시
                                <div className="text-center">
                                    <h6>모바일 앱에서 QR 코드를 스캔하세요</h6>
                                    <img
                                        src={qrCode}
                                        alt="QR Code"
                                        style={{ width: 200, height: 200 }}
                                    />
                                    <p className="text-muted mt-3">
                                        Passwordless X1280 앱을 사용하여 등록하세요
                                    </p>
                                </div>
                            ) : isRegistered === true ? (
                                // 이미 등록됨 - 해제 화면
                                <div>
                                    <div className="alert alert-info">
                                        현재 Passwordless: 등록됨
                                    </div>
                                    <div className="mb-3">
                                        <label htmlFor="password" className="form-label">
                                            본인 확인을 위한 비밀번호 입력
                                        </label>
                                        <input
                                            type="password"
                                            className="form-control"
                                            id="password"
                                            value={password}
                                            onChange={(e) => setPassword(e.target.value)}
                                            placeholder="현재 비밀번호를 입력하세요"
                                        />
                                    </div>
                                    <div className="d-flex gap-2">
                                        <button
                                            className="btn btn-danger flex-grow-1"
                                            onClick={handleUnregister}
                                            disabled={loading}
                                        >
                                            {loading ? 'Processing...' : 'Passwordless 해지'}
                                        </button>
                                        <button
                                            className="btn btn-secondary"
                                            onClick={closeModal}
                                        >
                                            취소
                                        </button>
                                    </div>
                                </div>
                            ) : (
                                // 비밀번호 입력
                                <div>
                                    <div className="mb-3">
                                        <label htmlFor="password" className="form-label">
                                            본인 확인을 위한 비밀번호 입력
                                        </label>
                                        <input
                                            type="password"
                                            className="form-control"
                                            id="password"
                                            value={password}
                                            onChange={(e) => setPassword(e.target.value)}
                                            placeholder="현재 비밀번호를 입력하세요"
                                        />
                                    </div>
                                    <div className="d-flex gap-2">
                                        <button
                                            className="btn btn-primary flex-grow-1"
                                            onClick={handleManageCheck}
                                            disabled={loading}
                                        >
                                            {loading ? 'Processing...' : '확인'}
                                        </button>
                                        <button
                                            className="btn btn-secondary"
                                            onClick={closeModal}
                                        >
                                            취소
                                        </button>
                                    </div>
                                </div>
                            )}
                </div>
            </BaseModal>
        )}
    </>;
}