import { useState } from "react"
import { useLocation } from "react-router-dom";
import { useAuth } from "/context/AuthContext";
import { useInit } from "/context/InitContext";
import { updateUser } from "/services/users";
import { URL } from "/config/constants";
import TeamSelect from "/components/TeamSelect";

export default function SignupTeam({ onConfirm = () => {}, onComplete = () => {} }) {
    const { auth, login } = useAuth() || {};
    const { teams } = useInit();
    const { pathname } = useLocation();
    const [team, setTeam] = useState({});

    const confirmTeam = e => {
        e.preventDefault();
        onConfirm(team);
    }

    /* 소셜 로그인 같이 유저의 팀이 없는 간편 가입 후 추가정보 기입을 위한 메소드 */
    const confirmRegister = () => {
        updateUser({ ...auth, team })    // 이미 가입된 유저의 정보를 수정하여 최종 가입시킨다.
        .then((data) => {
            if (data?.team) {
                alert("가입되었습니다!");   // 가입 안내 alert
                login(data);    // 자동 로그인 시켜줌 (카카오 로그인)
                onComplete();   // 화면 전환해줌
            }
        });
    }

    return <>
        <div className="d-flex flex-column" style={{ minHeight: '100%' }}>
            <div className="flex-grow overflow-y-auto">
                <div className="mt-3 mb-0 p-3 h4 text-center">
                    <span className="ps-2 pe-2 pb-1" style={{ borderBottom: '4px solid var(--point-color)' }}>나의 팀을 선택하세요!</span>
                </div>
                <div className="d-flex p-4 gap-20 flex-wrap">
                    {teams.map((t) => <>
                        <TeamSelect key={t.id} team={t} selected={team} setTeam={setTeam} />
                    </>)}
                </div>
            </div>
            <button className="btn btn-primary p-3 border-radius-0" disabled={!team} onClick={pathname != URL.REGISTER ? confirmRegister : confirmTeam}>내 팀으로 시작하기</button>
        </div>
    </>
}