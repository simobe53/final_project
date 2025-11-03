import { useState, useEffect } from 'react'
import { Outlet } from 'react-router-dom';
import axios from "/config/axios"
import { useAuth } from '/context/AuthContext';
import { NotificationProvider } from '/context/NotificationContext';
import Header from '/pages/Header';
import Menu from '/pages/Menu';
import SignupTeam from '/pages/login/SignupTeam';
import Chatbot from '/pages/Chatbot';
import NotificationPanel from '/pages/NotificationPanel';
import './App.css'

function App() {
  const [openChat, setOpenChat] = useState(false);
  const [needToSetTeam, setNeedToSetTeam] = useState();
  const { auth, fetchAuth, logout } = useAuth();
  const [notifyCount, setnotifyCount] = useState(0);

  // 로그인 상태 체크 (카카오 로그인 후 상태 동기화)
  useEffect(() => {
    // 이미 로그인된 상태라면 API 호출하지 않음
    if (auth.account) { //{}도 true로 평가되니까 auth.account 체크
      return;
    }
    const checkAuth = async () => {
      try {
        const response = await axios.get('/api/auth/login', {
          withCredentials: true
        });
        if (response.status === 200) {
          fetchAuth(response.data);
        }
      } 
      catch (e) {
        console.log(e);
        logout({});
       }
    };
    checkAuth();
  }, []);

  useEffect(() => {
    if (auth.id && !auth.team?.id && !needToSetTeam) setNeedToSetTeam(true);
  }, [auth, needToSetTeam])

  return (
    <NotificationProvider>
      <div id="main_section">
          <Chatbot />
          <NotificationPanel />
          {needToSetTeam ? 
            <SignupTeam onComplete={() => setNeedToSetTeam(false)} /> :
              <>
              <Header notifyCount={notifyCount} openChat={setOpenChat} />
              <main className="container">
                  <Outlet context={{setnotifyCount}}/>
              </main>
              <Menu />
            </>
          }
      </div>
    </NotificationProvider>
  )
}

export default App
