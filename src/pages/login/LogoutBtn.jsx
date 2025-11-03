import axios from "/config/axios";
import { useAuth } from "/context/AuthContext";

export default function LogoutBtn({
    className = '',
    children
}) {
    const { auth } = useAuth();
    const { logout } = useAuth();
    
    const onLogout = () => {
        axios.get("/api/auth/logout", {
            withCredentials: true // 쿠키 포함하여 요청
        })
        .then(response => {
            logout({ goLogin: true });
            // 백엔드에서 받은 카카오 로그아웃 URL로 리다이렉트
            if (response.data.logoutUrl) {
                window.location.href = response.data.logoutUrl;
            }
        })
        .catch(error => {
            console.log(error);
            logout({ goLogin: true }); 
        });
    }

    return <button onClick={onLogout} className={className || 'btn border border-gray border-radius-12 btn-sm text-white'}>
        {children || "로그아웃"}
    </button>
}