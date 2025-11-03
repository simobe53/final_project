import { useEffect, useState } from "react";
import { verifyUser } from "/services/users";
import Input from "/components/Input";
import Logo from "/components/Logo";
import { useAuth } from "/context/AuthContext";
import axios from "/config/axios";
import PasswordResetModal from "/components/PasswordResetModal";

export default function PwdConfirm({ onConfirm }) {
    const [password, setPassword] = useState("");
    const { auth: { account } } = useAuth();
    const [isPasswordlessUser, setIsPasswordlessUser] = useState(false);
    const isSocialLogin = !isNaN(account);
    const [showResetModal, setShowResetModal] = useState(false);

    const submit = (e) => {
        e.preventDefault();
        verifyUser(account, password)
        .then((data) => {
            if (data?.id) onConfirm();
        })
        .catch((error) => alert(error.response?.data?.message || '비밀번호가 틀립니다!'));
        e.stopPropagation();
    };

    const checkEnter = (e) => {
        if (e.key === "Enter") submit(e);
    }

    useEffect(() => {
        /* 카카오, 네이버 로그인 사용자는 비밀번호 재검증을 하지 않는다. 즉, 숫자면 바로 넘어가지도록 */
        if (isSocialLogin) onConfirm();
    }, [isSocialLogin])

    // Passwordless 등록 여부 확인
    useEffect(() => {
        if (!account || isSocialLogin) return;

        axios.get(`/api/auth/passwordless/status?account=${account}`)
            .then(({ data }) => {
                setIsPasswordlessUser(data.registered);
            })
            .catch(() => {
                setIsPasswordlessUser(false);
            });
    }, [account, isSocialLogin]);

    // Passwordless 해지 핸들러
    const handlePasswordlessUnregister = () => {
        if (!window.confirm('Passwordless 서비스를 해지하시겠습니까?')) {
            return;
        }

        // 해지 API 호출
        axios.post('/api/auth/passwordless/unregister', {
            account
        })
        .then(({ data }) => {
            if (data.success) {
                alert('Passwordless 서비스가 해지되었습니다.\n비밀번호를 재설정해주세요.');
                setIsPasswordlessUser(false);
                // 비밀번호 찾기 모달 자동 표시
                setShowResetModal(true);
            } else {
                alert('Passwordless 해지에 실패했습니다.');
            }
        })
        .catch((error) => {
            alert('Passwordless 해지 중 오류가 발생했습니다.');
        });
    };


    if (isSocialLogin) return null;

    return <>
        <div id="loginForm" className="bg-point">
            <div className="bg-white border-radius-20 m-auto" style={{ width: 500 }}>
                <Logo style={{ zoom: 2, position: 'absolute', left: '50%', transform: 'translate(-50%, -90%)' }} />
                <div className="d-flex flex-column align-items-stretch justify-content-center" style={{ gap: 20, padding: 40 }}>
                    <h6 className="mt-2 mb-1 text-center" style={{ lineHeight: 1.3 }}>
                        개인정보 보호를 위해 본인확인을 진행합니다.
                    </h6>

                    {/* Passwordless 회원인 경우에만 안내 메시지 표시 */}
                    {isPasswordlessUser && (
                        <div className="alert alert-warning">
                            <p className="mb-0 small">
                                <strong>Passwordless 서비스 관련 안내</strong><br/>
                                Passwordless 서비스를 이용하시는 회원은 먼저 [비밀번호 재설정]을 통해 비밀번호를 안내 받으시기 바랍니다.
                            </p>
                        </div>
                    )}

                    <input type="text" name="account" value={account} readOnly hidden />
                    <Input type="password" name="password" placeholder="비밀번호를 입력하세요" value={password} onChange={e => setPassword(e.target.value)} autoFocus onKeyDown={checkEnter} />

                    <button
                        type="submit"
                        className="btn btn-primary border-radius-20"
                        style={{ height: 60 }}
                        disabled={!password}
                        onClick={submit}
                    >
                        확인
                    </button>

                    {/* Passwordless 회원인 경우 추가 버튼 표시 */}
                    {isPasswordlessUser && (
                        <div className="d-flex gap-2">
                            <button
                                type="button"
                                className="btn btn-outline-secondary border-radius-20 flex-grow-1"
                                style={{ height: 50 }}
                                onClick={() => setShowResetModal(true)}
                            >
                                비밀번호 재설정
                            </button>
                            <button
                                type="button"
                                className="btn btn-outline-danger border-radius-20 flex-grow-1"
                                style={{ height: 50 }}
                                onClick={handlePasswordlessUnregister}
                            >
                                Passwordless 해지
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>

        {/* 비밀번호 재설정 모달 */}
        {showResetModal && (
            <PasswordResetModal
                account={account}
                onClose={() => setShowResetModal(false)}
                isLoggedIn={true}
            />
        )}
    </>;
}
