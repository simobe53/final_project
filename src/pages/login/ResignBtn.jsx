import { useState } from "react";
import { useAuth } from "/context/AuthContext";
import { deleteUser } from "/services/users";
import PwdConfirm from "./PwdConfirm";

// 회원 탈퇴 버튼
export default function ResignBtn() {
    const [confirmed, setConfirmed] = useState(false);
    const { auth: { account }, logout } = useAuth();

    const resign = () => {
        deleteUser(account).then((data) => {
            if (data?.id) {
                alert('성공적으로 탈퇴되었습니다. \n로그인 화면으로 돌아갑니다.');
                setConfirmed(false);
                logout({ goLogin: true });
            }
        });
    }

    const confirmResign = e => {
        e.preventDefault();
        e.stopPropagation();
        if (confirm("정말 탈퇴하시겠습니까?")) {
            setConfirmed(true);
        }
    }

    return <>
        <button onClick={confirmResign} className="btn btn-none text-nowrap ps-4 w-100 d-flex align-items-center">회원 탈퇴하기</button>
        {confirmed && <PwdConfirm onConfirm={resign} />}
    </>
}