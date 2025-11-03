/* eslint-disable react-refresh/only-export-components */

import { createContext, useContext, useState } from "react";
import { useNavigate } from "react-router-dom";
import { URL } from "/config/constants";


const AuthContext = createContext(null);
const initState = {
    team: { location: '서울특별시 송파구 올림픽로 25' }
};

export function AuthProvider({ children, resetUI }) {
    const [auth, setAuth] = useState(initState);  // 전역 state;
    const navigate = useNavigate();

    const login = (user) => {
        setAuth(user);
        navigate(URL.HOME, { replace: true });
    };

    const logout = ({ goLogin = false }) => {
        setAuth(initState);
        if (resetUI) resetUI(); // 챗봇, 알림창 닫기
        if (goLogin) navigate(URL.LOGIN, { replace: true });
    }

    const fetchAuth = user => {
        setAuth(user);
    }

    return <>
        <AuthContext.Provider value={{ auth, login, logout, fetchAuth }}>
            {children}
        </AuthContext.Provider>
    </>
}

export function useAuth() {
    return useContext(AuthContext);
}
