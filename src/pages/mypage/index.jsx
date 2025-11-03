import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import StatusBar from "/components/StatusBar";
import Logo from "/components/Logo";
import { URL } from "/config/constants";
import { useAuth } from "/context/AuthContext";
import PwdConfirm from "/pages/login/PwdConfirm";
import MyInfo from "./MyInfo";

export default function MyPage() {
    const [confirmed, setConfirmed] = useState(false);
    const navigate = useNavigate();
    const { auth } = useAuth();
    const isLogin = !!auth.id;
    return <>
        <StatusBar title="마이페이지" noBack />
        <section className="d-flex flex-column overflow-y-auto" style={{ marginTop: 0, minHeight: "100%", height: '100%', background: 'var(--gray-border-color)' }}>
            <MyInfo setConfirmed={setConfirmed} isLogin={isLogin} />
            <div className="bg-white mt-2 mb-2">
                <div className="border-bottom border-gray p-2 ps-4 d-flex justify-content-between align-items-center h6 m-0">서비스</div>
                <Link to={`${URL.MYPAGE}${URL.UNIFORM}`} className="border-bottom border-gray p-4 pointer d-flex justify-content-between align-items-center">
                    <span><img src="/assets/icons/food.png" width="24px" className="me-2"/> 팀 유니폼 제작</span>
                    <i className="fas fa-angle-right" />
                </Link>
                <Link to={`${URL.MYPAGE}${URL.CHEERSONG}`} className="border-bottom border-gray p-4 pointer d-flex justify-content-between align-items-center">
                    <span><img src="/assets/icons/food.png" width="24px" className="me-2"/> 팀 응원곡 제작</span>
                    <i className="fas fa-angle-right" />
                </Link>
            </div>
            <div className="bg-white mt-2">
                <div className="border-bottom border-gray p-2 ps-4 d-flex justify-content-between align-items-center h6 m-0">내 글 관리</div>
                <Link to={`${URL.MYPAGE}${URL.PLACE}`} className="border-bottom border-gray p-4 pointer d-flex justify-content-between align-items-center">
                    <span><img src="/assets/icons/food.png" width="24px" className="me-2"/> 내가 추천한 맛집</span>
                    <i className="fas fa-angle-right" />
                </Link>
                <Link to={`${URL.MYPAGE}${URL.PLACE}/scrap`} className="border-bottom border-gray p-4 pointer d-flex justify-content-between align-items-center">
                    <span><img src="/assets/icons/bookmark.png" width="24px" className="me-2"/> 내가 스크랩한 맛집</span>
                    <i className="fas fa-angle-right" />
                </Link>
                <Link to={`${URL.MYPAGE}${URL.PLACE}/review`} className="border-bottom border-gray p-4 pointer d-flex justify-content-between align-items-center">
                    <span><img src="/assets/icons/reviewIcon.png" width="24px" className="me-2"/> 내가 작성한 리뷰</span>
                    <i className="fas fa-angle-right" />
                </Link>
                <Link to={`${URL.MYPAGE}${URL.MEET}`} className="border-bottom border-gray p-4 pointer d-flex justify-content-between align-items-center">
                    <span><img src="/assets/icons/stadium.png" width="24px" className="me-2"/> 나의 직관 모집글</span>
                    <i className="fas fa-angle-right" />
                </Link>
            </div>
            <br/>
            <div className="p-3 pt-4">
                <br/>
                <small className="text-gray">
                    © 2024 서울특별시 서초구 서초대로77길 41, 4층 (서초동, 대동Ⅱ) <br/>
                    <a className="text-gray" target="_blank" href="https://ictedu.co.kr">한국 ICT 인재개발원</a> 
                    
                    <br/><br/>
                    icon by. <a className="text-gray" href="https://www.flaticon.com/kr/authors/arkinasi" target="_blank">arkinasi</a>
                </small>		
            </div>
        </section>
        {confirmed && <PwdConfirm onConfirm={() => navigate(URL.MYINFO)} />}
    </>;
}