import { useNavigate } from "react-router-dom";
import ProfileImg from "/components/ProfileImg";
import TeamLogo from "/components/TeamLogo";
import { useAuth } from "/context/AuthContext";
import { URL } from '/config/constants';
import MyPoint from './MyPoint'

export default function MyInfo({
    setConfirmed
}) {
    const { auth: { id, name: username, profileImage, team } } = useAuth();
    const isLogin = !!id;
    const navigate = useNavigate();
    return <>
    <div className="mb-2 d-flex gap-20 border-bottom border-gray align-items-center p-3 pt-4 pb-4 bg-white">
        <ProfileImg src={profileImage} zoom={1.5} />
        {isLogin ? <>
        </> : <>
        </>}
        <div className="flex-grow">
            <p className={isLogin ? 'h5' : 'h5 mb-0'}>{isLogin ? `${username} 님` : '로그인 해주세요'}</p>
            {isLogin && <p className="text-gray d-flex align-items-center gap-2">
                <TeamLogo name={team.idKey} zoom={0.6} />
                <small>{team.name}</small>
            </p>}
        </div>
        {isLogin ? 
            <button onClick={() => setConfirmed(true)} className="btn btn-none border-radius-20" style={{ width: 40, height: 40 }}>
                <i className="fas fa-wrench" style={{ fontSize: 20 }} />
            </button>:
            <button onClick={() => navigate(URL.LOGIN)} className="btn btn-outline-secondary btn-sm border-radius-20">
                로그인
            </button>
        }
    </div>
    {
        isLogin && <MyPoint />
    }
</>
}
