import axios from "/config/axios";
import { useRef, useState, useEffect } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "/context/AuthContext";
import Logo from '/components/Logo';
import { URL } from "/config/constants";
import PasswordlessRegistrationModal from '/components/PasswordlessRegistrationModal';
import PasswordResetModal from '/components/PasswordResetModal';
import usePasswordlessWebSocket from '/components/hooks/usePasswordlessWebSocket';
import SNSLogin from "./SNSLogin";

export default function Login() {
    const { login } = useAuth();
    const accountRef = useRef();
    const passwordRef = useRef();

    // ==================== [Passwordless 구현] Refs ====================
    const setupPasswordRef = useRef(); // Passwordless 설정용 비밀번호
    const { connect, disconnect } = usePasswordlessWebSocket();
    const timerRef = useRef(null);

    // ==================== 기본 State ====================
    const [loginMethod, setLoginMethod] = useState('password'); // 'password' or 'passwordless'
    const [showResetModal, setShowResetModal] = useState(false); // 비밀번호 찾기 모달

    // ==================== [Passwordless 구현] State ====================
    const [showSetupScreen, setShowSetupScreen] = useState(false); // Passwordless 설정 화면 표시 여부
    const [authNumber, setAuthNumber] = useState(''); // Passwordless 인증번호
    const [sessionId, setSessionId] = useState(''); // Passwordless 세션 ID
    const [maxTime, setMaxTime] = useState(60); // 인증 제한 시간 (초) - 서버에서 받아온 term 값
    const [timeLeft, setTimeLeft] = useState(60); // 남은 시간 (초)
    const [showPasswordlessRegistrationModal, setShowPasswordlessRegistrationModal] = useState(false);

    // ==================== 로그인 Submit 핸들러 ====================
    const submit = (e) => {
        e.preventDefault();

        const account = accountRef.current.value;

        if (!account) {
            alert("아이디를 입력해주세요!");
            return;
        }

        // ==================== [Passwordless 구현] 설정 화면 처리 ====================
        if (showSetupScreen) {
            const password = setupPasswordRef.current.value;
            if (!password) {
                alert("비밀번호를 입력해주세요!");
                return;
            }

            // 먼저 등록 여부 확인
            axios.get(`/api/auth/passwordless/status?account=${account}`)
                .then(({ data }) => {
                    if (data.registered) {
                        // 이미 등록됨
                        alert('이미 Passwordless가 등록되어 있습니다.\nPasswordless 로그인으로 진행해주세요.');
                        setShowSetupScreen(false);
                        setLoginMethod('passwordless');
                    } else {
                        // 미등록 - 비밀번호 검증 후 토큰 받아오기
                        return axios.post('/api/auth/passwordless/manage-check', {
                            account,
                            password
                        });
                    }
                })
                .then((response) => {
                    if (response && response.data) {
                        const { data } = response;
                        if (data.success) {
                            // 세션에 저장됨 - 모달 열기
                            setShowPasswordlessRegistrationModal(true);
                        } else {
                            alert('비밀번호 검증에 실패했습니다.');
                        }
                    }
                })
                .catch((error) => {
                    if (error.response?.status === 401) {
                        alert('비밀번호가 올바르지 않습니다.');
                    } else if (error.message !== 'ALREADY_REGISTERED') {
                        alert('오류가 발생했습니다.');
                    }
                });
            return;
        }

        // ==================== 일반 Password 로그인 처리 ====================
        if (loginMethod === 'password') {
            const password = passwordRef.current.value;

            if (!password) {
                alert("비밀번호를 입력해주세요!");
                return;
            }

            // 사전 체크 제거 - 백엔드에서 처리
            axios.post("/api/auth/login", { account, password }, {
                withCredentials: true // 쿠키 포함하여 요청
            })
                .then((response) => {
                    if (response && response.data?.id) {
                        login(response.data);
                    }
                })
                .catch((error) => {
                    if (error.response?.status === 403) {
                        // 백엔드에서 받은 메시지 그대로 표시
                        alert(error.response.data.message || "Passwordless가 등록된 사용자는 일반 로그인을 사용할 수 없습니다.");
                        setLoginMethod('passwordless');
                    } else {
                        alert("로그인에 실패하였습니다!");
                    }
                });
        } else {
            // ==================== [Passwordless 구현] Passwordless 로그인 처리 ====================
            // 등록 여부 확인 후 6자리 인증번호 표시
            axios.get(`/api/auth/passwordless/status?account=${account}`)
                .then(({ data }) => {
                    if (data.registered) {
                        // 등록되어 있으면 Passwordless 로그인 시작
                        return axios.post('/api/auth/passwordless/login', { account });
                    } else {
                        // 미등록이면 설정 안내
                        const confirmSetup = window.confirm(
                            'Passwordless 서비스가 등록되어 있지 않습니다.\n\nPasswordless를 설정하시겠습니까?'
                        );
                        if (confirmSetup) {
                            setShowSetupScreen(true);
                        }
                        throw new Error('NOT_REGISTERED');
                    }
                })
                .then(({ data }) => {
                    if (data.success && data.authNumber) {
                        // 6자리 인증번호를 비밀번호 필드에 표시
                        const termSeconds = parseInt(data.term) || 60;
                        setAuthNumber(data.authNumber);
                        setSessionId(data.sessionId);
                        setMaxTime(termSeconds);
                        setTimeLeft(termSeconds);
                        // WebSocket 연결 및 승인 대기
                        startWebSocket(data.wsUrl, data.pushConnectorToken, data.sessionId, account);
                        startTimer();
                    }
                })
                .catch((error) => {
                    if (error.message !== 'NOT_REGISTERED') {
                        alert('Passwordless 로그인 중 오류가 발생했습니다.');
                    }
                });
        }
    };

    // ==================== [Passwordless 구현] WebSocket 관련 함수들 ====================

    // WebSocket 연결 및 승인 대기
    const startWebSocket = (wsUrl, pushConnectorToken, sid, account) => {
        connect(wsUrl, pushConnectorToken, (result) => {
            // 푸시 알림 받음 - 승인 확인
            console.log('Checking authentication result...');
            checkLoginResult(sid, account);
        });
    };

    // 타이머 시작
    const startTimer = () => {
        timerRef.current = setInterval(() => {
            setTimeLeft(prev => {
                if (prev <= 1) {
                    cancelPasswordlessLogin();
                    alert('인증 시간이 초과되었습니다.');
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
    };

    // 인증 결과 확인
    const checkLoginResult = (sid, account) => {
        axios.post('/api/auth/passwordless/result', {
            sessionId: sid,
            account: account
        })
        .then(({ data }) => {
            console.log('Auth result:', data.status);

            if (data.status === 'approved') {
                // 성공 - 로그인 처리
                disconnect();
                if (timerRef.current) clearInterval(timerRef.current);

                if (data.user) {
                    console.log('Login successful!');
                    login(data.user);
                }
            } else if (data.status === 'rejected') {
                // 거부됨
                cancelPasswordlessLogin();
                alert('인증이 거부되었습니다.');
            } else if (data.status === 'timeout' || data.status === 'error') {
                cancelPasswordlessLogin();
                alert('인증 시간이 초과되었습니다.');
            }
        })
        .catch((err) => {
            console.error('Failed to check login result:', err);
        });
    };

    // Passwordless 로그인 취소
    const cancelPasswordlessLogin = () => {
        disconnect();
        if (timerRef.current) {
            clearInterval(timerRef.current);
            timerRef.current = null;
        }
        if (sessionId) {
            axios.post('/api/auth/passwordless/cancel', { sessionId })
                .catch(() => {});
        }
        setAuthNumber('');
        setSessionId('');
        setTimeLeft(180);
    };

    // ==================== [Passwordless 구현] 모달 핸들러들 ====================

    // 컴포넌트 언마운트 시 정리
    useEffect(() => {
        return () => {
            if (timerRef.current) {
                clearInterval(timerRef.current);
            }
        };
    }, []);

    // Passwordless 등록 모달 닫기
    const handleRegistrationModalClose = () => {
        setShowPasswordlessRegistrationModal(false);
    };

    // Passwordless 등록 완료 후 처리
    const handleRegistrationSuccess = () => {
        setShowPasswordlessRegistrationModal(false);
        setShowSetupScreen(false);
        setLoginMethod('password');
    };

    // ==================== Render ====================
    return <>
        <div id="loginForm" className="bg-point">
            <div className="bg-white border-radius-20 m-auto" style={{ width: 500, transform: 'translateY(50px)' }}>
                <Logo style={{ zoom: 2, position: 'absolute', left: '50%', transform: 'translate(-50%, -90%)' }} />
                <form className="d-flex flex-column align-items-stretch" style={{ gap: 20, padding: '60px 60px 40px' }} onSubmit={submit}>
                    <label htmlFor="login_id" hidden>아이디</label>
                    <input
                        id="login_id"
                        className="form-control border-color-gray border-radius-20 ps-3"
                        ref={accountRef}
                        type="text"
                        name="username"
                        placeholder="아이디를 입력하세요"
                        style={{ height: 60 }}
                    />
                    <label htmlFor="login_password" hidden>비밀번호</label>
                    {/* ==================== [Passwordless 구현] 설정 화면 입력 필드 ==================== */}
                    {showSetupScreen ? (
                        <input
                            id="setup_password"
                            className="form-control border-color-gray border-radius-20 ps-3"
                            ref={setupPasswordRef}
                            type="password"
                            name="password"
                            placeholder="비밀번호를 입력하세요"
                            style={{ height: 60 }}
                        />
                    ) : (
                        /* ==================== [Passwordless 구현] 로그인 화면 입력 필드 (Password / Passwordless 인증번호) ==================== */
                        <input
                            id="login_password"
                            className={`form-control border-color-gray border-radius-20 ps-3 ${authNumber ? 'text-center' : ''}`}
                            ref={passwordRef}
                            type={authNumber ? 'text' : 'password'}
                            name="password"
                            value={authNumber || undefined}
                            placeholder={loginMethod === 'password' ? "비밀번호를 입력하세요" : ""}
                            disabled={loginMethod === 'passwordless' && !authNumber}
                            readOnly={authNumber !== ''}
                            onChange={(e) => {
                                if (loginMethod === 'password' && !authNumber) {
                                    passwordRef.current.value = e.target.value;
                                }
                            }}
                            style={{
                                height: 60,
                                fontSize: authNumber ? '24px' : '16px',
                                fontWeight: authNumber ? 'bold' : 'normal',
                                letterSpacing: authNumber ? '8px' : 'normal',
                                background: authNumber
                                    ? `linear-gradient(to right, #80bdff 0%, #80bdff ${(timeLeft / maxTime) * 100}%, transparent ${(timeLeft / maxTime) * 100}%, transparent 100%)`
                                    : '',
                                transition: 'background 0.5s linear'
                            }}
                        />
                    )}

                    {/* ==================== [Passwordless 구현] 로그인 방식 선택 라디오 버튼 ==================== */}
                    <div className="d-flex justify-content-center gap-4 mt-2">
                        <div className="form-check">
                            <input
                                className="form-check-input"
                                type="radio"
                                name="loginMethod"
                                id="loginPassword"
                                value="password"
                                checked={loginMethod === 'password'}
                                onChange={() => {
                                    setLoginMethod('password');
                                    setAuthNumber('');
                                    setShowSetupScreen(false);
                                }}
                            />
                            <label className="form-check-label" htmlFor="loginPassword">
                                Password 로그인
                            </label>
                        </div>
                        <div className="form-check">
                            <input
                                className="form-check-input"
                                type="radio"
                                name="loginMethod"
                                id="loginPasswordless"
                                value="passwordless"
                                checked={loginMethod === 'passwordless'}
                                onChange={() => {
                                    setLoginMethod('passwordless');
                                    setAuthNumber('');
                                }}
                            />
                            <label className="form-check-label" htmlFor="loginPasswordless">
                                Passwordless 로그인
                            </label>
                        </div>
                    </div>
                    <button
                        type="submit"
                        className="btn btn-primary border-radius-20"
                        style={{ height: 60 }}
                    >
                        {showSetupScreen ? 'Passwordless 설정' : '로그인'}
                    </button>
                </form>
                {/* ==================== [Passwordless 구현] 설정 링크 및 화면 전환 ==================== */}
                <p className="d-flex gap-8 justify-content-center">
                    {loginMethod === 'passwordless' ? (
                        !showSetupScreen ? (
                            <>
                                Passwordless를 설정하시겠습니까?
                                <a
                                    href="#"
                                    className="point"
                                    onClick={(e) => {
                                        e.preventDefault();
                                        setShowSetupScreen(true);
                                    }}
                                >
                                    설정하기
                                </a>
                            </>
                        ) : (
                            <a
                                href="#"
                                className="point"
                                onClick={(e) => {
                                    e.preventDefault();
                                    setShowSetupScreen(false);
                                }}
                            >
                                로그인 화면으로 돌아가기
                            </a>
                        )
                    ) : (
                        <>
                            아직 회원이 아니신가요?
                            <Link to={URL.REGISTER} className="point">회원가입</Link>
                        </>
                    )}
                </p>
                <div className="d-flex flex-column align-items-stretch" style={{ gap: 12, padding: 40 }} >
                    <SNSLogin />
                </div>
                {/* 비밀번호 찾기 링크 */}
                <p className="text-center" style={{ marginTop: -20, marginBottom: 20 }}>
                    <a
                        href="#"
                        className="text-muted"
                        style={{ fontSize: '0.85rem', textDecoration: 'none' }}
                        onClick={(e) => {
                            e.preventDefault();
                            setShowResetModal(true);
                        }}
                    >
                        비밀번호 찾기
                    </a>
                </p>
            </div>
        </div>

        {/* ==================== [Passwordless 구현] 등록 모달 ==================== */}
        {showPasswordlessRegistrationModal && (
            <PasswordlessRegistrationModal
                account={accountRef.current?.value}
                onClose={handleRegistrationModalClose}
                onSuccess={handleRegistrationSuccess}
            />
        )}

        {/* 비밀번호 찾기 모달 */}
        {showResetModal && (
            <PasswordResetModal
                account={accountRef.current?.value}
                onClose={() => setShowResetModal(false)}
            />
        )}
    </>;
}
