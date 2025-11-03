import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "/context/AuthContext";
import { changePassword } from "/services/users";
import { REGEX, URL } from '/config/constants';
import Input from "/components/Input";

// 회원 비밀번호 변경 버튼
export default function PwdChangeBtn() {
    const [opened, setOpened] = useState(false);
    const [orgPassword, setOrgPassword] = useState("");
    const [password, setPassword] = useState("");
    const [confirmed, setConfirmed] = useState("");
    const { auth: { id } } = useAuth();
    const validate = orgPassword && password && confirmed && password === confirmed && RegExp(REGEX.PASSWORD).test(password);
    const navigate = useNavigate();

    const changePwd = (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (!validate) return;
        changePassword({ id, password, orgPassword })
        .then((data) => {
            if (data?.id) {
                alert('성공적으로 변경되었습니다.');
                setOpened(false);
                navigate(URL.MAIN);
            } else {
                alert("비밀번호가 틀립니다!");
            }
        });
    }

    const openPopup = e => {
        e.preventDefault();
        e.stopPropagation();
        setOpened(true)
    }

    return <>
        <button onClick={openPopup} className="btn btn-none text-nowrap ps-4 w-100 d-flex align-items-center">비밀번호 변경하기</button>
        {opened && <>
            <div className="position-fixed bg-white shadow-lg overflow-hidden border-radius-12" style={{ width: 400, zIndex: 2, top: '50%', left: '50%', transform: 'translate(-50%, -50%)' }}>
                <div className="d-flex ps-2 pe-2 border-bottom border-gray justify-content-between align-items-center">
                    <h6 className="p-3 m-0">비밀번호 변경</h6>
                    <button className="btn btn-none p-2" onClick={() => setOpened(false)}>
                        <i className="fas fa-xmark" style={{ fontSize: 24 }} />
                    </button>
                </div>
                <form className="d-flex flex-column">
                    <div className="d-flex flex-column gap-20 p-4">
                        <Input
                            id="register_orgpassword"
                            type="password"
                            name="orgPassword"
                            label="기존 비밀번호"
                            required
                            value={orgPassword}
                            onChange={e => setOrgPassword(e.target.value)}
                            autoFocus
                        />

                        <Input
                            id="register_password"
                            type="password"
                            name="password"
                            label="변경할 비밀번호"
                            required
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                        />

                        <Input
                            label="비밀번호 확인"
                            placeholder="비밀번호를 다시 한번 입력하세요"
                            id="register_password_confirm"
                            type="password"
                            required
                            errorMessage={password && confirmed && password != confirmed ? '입력하신 비밀번호와 일치하지 않습니다' : ''}
                            value={confirmed}
                            onChange={e => setConfirmed(e.target.value)}
                        />
                    </div>
                    <button type="submit" onClick={changePwd} className="btn btn-primary mt-auto border-radius-0" style={{ height: 60 }} disabled={!validate}>{validate ? '비밀번호 변경' : '필수 사항을 전부 입력하세요'}</button>
                </form>
            </div>
        </>}
    </>
}