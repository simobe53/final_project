import { NavLink } from "react-router-dom";
import { URL } from "/config/constants";
import Logo from "/components/Logo";
import { useAuth } from "/context/AuthContext";
import { useInit } from "/context/InitContext";
import { useNotifications } from "/context/NotificationContext";
import classes from "./Header.module.scss";
import LogoutBtn from "./login/LogoutBtn";

export default function Header() {
    const { auth: { account } } = useAuth(); /* 여기 들어오는 알림은 new 만 */
    const { activateChat, activateNotification } = useInit();
    const { unreadCount } = useNotifications();

    // if (!account) return null;

    return <>
        <nav className={classes.header} style={{ height: 55 }}>
            <div style={{ width: 100 }}>
                <button className="btn btn-none p-0" onClick={() => activateChat(true)}>
                    <img src="/assets/icons/chatbot.png" width="40px" />
                </button>
            </div>
            <div className="d-flex align-items-center">
                <Logo />
            </div>
            <ul className="navbar-nav d-flex flex-row align-items-center gap-3">
                {account && <>
                    <li className="nav-item">
                        <button 
                            className="btn btn-none p-0 position-relative" 
                            onClick={() => activateNotification(true)}
                        >
                            <i className="fas fa-bell text-white" style={{ fontSize: '24px' }} />
                            {unreadCount > 0 && (
                                <span 
                                    className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger"
                                    style={{ fontSize: '10px', padding: '3px 6px' }}
                                >
                                    {unreadCount > 99 ? '99+' : unreadCount}
                                </span>
                            )}
                        </button>
                    </li>
                </>}
                <li className="nav-item text-right">
                    {account ? 
                        <LogoutBtn className="btn p-0 btn-none">
                            <i className="fa-solid fa-right-from-bracket text-white" style={{ fontSize: '24px' }} />
                            </LogoutBtn> : 
                        <NavLink to={URL.LOGIN} className="position-relative">
                            <small>로그인</small>
                        </NavLink>
                    }
                </li>
            </ul>
        </nav>
    </>
}