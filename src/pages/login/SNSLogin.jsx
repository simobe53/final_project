
import { useState, useEffect } from 'react';
import axios from "/config/axios";

export default function SNSLogin() {
    const [kakaoUrl, setKakaoUrl] = useState('');
    const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;
        const handleGoogleResponse = async (response) => {
        console.log("Google login response:", response)
        const idToken = response.credential;
        console.log("idToken:", idToken);
        if (!idToken) {
            return;
        }
        try {
            const res = await fetch('/api/auth/google', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ idToken }),
            });

            const data = await res.json();

            if (data.token) {
                localStorage.setItem("jwt", data.token);
                window.location.href = "/";
            } else {}
        } catch (err) {
            console.error(err);
        }
    };

    useEffect(() => {
        // 서버에서 카카오 로그인 URL 가져오기
        axios.get('/api/auth/kakao-url')
            .then(response => {
                setKakaoUrl(response.data.url);
            })
            .catch(error => {
            });
        const script = document.createElement("script");
        script.src = "https://accounts.google.com/gsi/client";
        script.async = true;
        script.defer = true;

        script.onload = () => {
            if (window.google && window.google.accounts) {
            window.google.accounts.id.initialize({
                client_id: GOOGLE_CLIENT_ID,
                callback: handleGoogleResponse,
            });

            window.google.accounts.id.renderButton(
                document.getElementById("googleBtn"),
                { theme: "outline", height: 55, width: 440 }
            );
            }
        };

        document.body.appendChild(script);
        return () => document.body.removeChild(script);
    }, []);
    return <>
        <a className="btn border-radius-12 p-1" style={{ background: '#fde500' }} href={kakaoUrl}>
            <img src="/assets/icons/kakao_login_btn.png" alt="카카오로 시작하기" />
        </a>
        <div id="googleBtn" style={{ width: '100%', height: 55 }}></div>
    </>
}