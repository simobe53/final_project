import { useEffect, useState } from "react";
import { Link, Outlet, useNavigate } from "react-router-dom";
import { URL } from '/config/constants';
import { getDiary } from "/services/diary";
import { useAuth } from "/context/AuthContext"
import NeedLogin from "/components/NeedLogin";
import Calendar from "/components/Calendar";
import DiaryDetail from "./DiaryDetail";
import classes from './index.module.scss';

export default function Diary() {
    const { auth } = useAuth();
    const [date, setDate] = useState(null);
    const [diary, setDiary] = useState(null); // 일기 데이터
    const navigate = useNavigate();

    useEffect(() => {
        if (date) {
            getDiary(date)
            .then(data => {
                console.log("받은 일기 데이터:", data); // <- 전체 확인
                console.log("totalWins:", data.totalWins, "totalGames:", data.totalGames); // <- 원하는 값만 확인
                setDiary(data.id ? data : null);
            })
            .catch(e => {
                // 권한 오류 체크
                if (e.code === 301) {
                    alert('로그인이 필요한 페이지입니다!');
                    navigate(URL.LOGIN);
                }
            })
        }
    }, [date]);

    if (!auth?.id) return <NeedLogin />

    return <>
        <section className={classes.diaryContainer}>
            <Calendar onChange={setDate} />

            
            {/* 선택된 날짜의 일기 내용 영역 */}
            <div className={classes.contentArea}>
                {date ? (
                    <div>
                        {diary ? <DiaryDetail date={date} {...diary} onReset={() => setDiary(null)} />
                            : 
                            <div className={classes.contentMessage}>
                                <p>이 날짜에는 작성된 일기가 없습니다.</p>
                            </div>
                        }
                    </div>
                ) : (
                    <div className={classes.contentMessage}>
                        <p>날짜를 선택해주세요.</p>
                    </div>
                )}
                
                
            </div>
        </section>

        {/* 일기 작성 버튼 - contentArea 내부에 고정 */}
        {!!auth.id && <Link to={`${URL.DIARY}/create`} className="create_button" />}
        
        {/* 일기 작성 모달 */}
        <Outlet context={{ date }} />
    </>
}
