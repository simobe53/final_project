import { useState } from "react";
import axios from "/config/axios";
import BaseModal from "./BaseModal";

export default function PasswordResetModal({ account, onClose, isLoggedIn = false }) {
    const [inputAccount, setInputAccount] = useState(account || "");
    const [email, setEmail] = useState("");

    const handleResetSubmit = async (e) => {
        e.preventDefault();

        if (!inputAccount) {
            alert('아이디를 입력해주세요.');
            return;
        }

        if (!email) {
            alert('이메일을 입력해주세요.');
            return;
        }

        try {
            const { data } = await axios.post('/api/auth/reset-password', {
                account: inputAccount,
                email
            });

            if (data.success) {
                alert(data.message);
                onClose();
                setInputAccount("");
                setEmail("");
            } else {
                alert(data.message);
            }
        } catch (error) {
            alert(error.response?.data?.message || '비밀번호 찾기 중 오류가 발생했습니다.');
        }
    };

    return (
        <BaseModal
            title={isLoggedIn ? '비밀번호 재설정' : '비밀번호 찾기'}
            onClose={onClose}
        >
            <form onSubmit={handleResetSubmit}>
                <div className="modal-body">
                    <div className="mb-3">
                        <label htmlFor="reset_account" className="form-label">아이디</label>
                        <input
                            type="text"
                            className="form-control"
                            id="reset_account"
                            value={inputAccount}
                            onChange={(e) => setInputAccount(e.target.value)}
                            placeholder="아이디를 입력하세요"
                            readOnly={!!account}
                            required
                        />
                    </div>
                    <div className="mb-3">
                        <label htmlFor="reset_email" className="form-label">이메일</label>
                        <input
                            type="email"
                            className="form-control"
                            id="reset_email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="이메일을 입력하세요"
                            required
                        />
                    </div>
                    <div className="alert alert-info">
                        <small>
                            {isLoggedIn
                                ? '가입 시 등록하신 이메일로 8자리 숫자 형태의 임시 비밀번호가 전송됩니다.'
                                : '입력하신 이메일로 8자리 숫자 형태의 임시 비밀번호가 전송됩니다.'}
                        </small>
                    </div>
                </div>
                <div className="modal-footer border-0">
                    <button
                        type="button"
                        className="btn btn-secondary"
                        onClick={onClose}
                    >
                        취소
                    </button>
                    <button
                        type="submit"
                        className="btn btn-primary"
                    >
                        임시 비밀번호 발급
                    </button>
                </div>
            </form>
        </BaseModal>
    );
}
