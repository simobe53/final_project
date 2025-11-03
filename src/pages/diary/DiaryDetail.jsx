import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "/context/AuthContext"
import { deleteDiary } from "/services/diary";
import { getScheduleById } from "/services/news";
import { URL } from "/config/constants";
import Schedule from "/pages/schedules/Schedule";
import classes from './index.module.scss';

export default function DiaryDetail({ date, onReset, ...diary }) {
    const { auth } = useAuth();
    const navigate = useNavigate();
    const { schedule_id: scheduleId } = diary;
    const [schedule, setSchedule] = useState();

    function calculateWinRate(totalWins, totalGames) {
        if (!totalGames || totalGames === 0) return "0.00";
        return ((totalWins / totalGames) * 100).toFixed(2);
    }

    async function handleDelete() {
        if (!diary?.id) return alert("삭제할 일기가 없습니다.");
        if (!confirm("정말 이 일기를 삭제하시겠습니까?")) return;

        try {
            await deleteDiary(diary.id);
            alert("일기가 삭제되었습니다.");
            // 선택된 날짜의 데이터 초기화
            navigate(URL.DIARY);
            onReset();
        } catch (error) {
            console.error("❌ 삭제 실패:", error);
            alert("일기 삭제에 실패했습니다.");
        }
    }

    useEffect(() => {
        if (!scheduleId) setSchedule(null);
        getScheduleById(scheduleId)
        .then(data => setSchedule(data))
    }, [scheduleId])

    if (!diary) return null;
    const [y,m,d] = date.split("-");
    return <>
        <div>
            <div className="d-flex">
                {/* 티켓 이미지 */}
                <div className={classes.mainImageContainer}>
                    <div className={classes.winRate}>
                        <h6 className="small">나의 응원팀 직관 승률</h6>
                        <small>{diary.totalWins}승 / 총 {diary.totalGames} 경기 직관</small> <br/>
                        <b className="h5 point">{calculateWinRate(diary.totalWins, diary.totalGames)}%</b>
                    </div>
                    {diary.ticket_url && <div>
                        <h6 className="small">직관 티켓</h6>
                        <img src={diary.ticket_url} alt="직관 티켓 이미지" className={classes.mainImage} />
                    </div>}
                    {/* 하단 추가 이미지들 */}
                    {diary.photo_urls && diary.photo_urls.length > 0 && (
                        <div>
                            <h6 className="small">그날의 사진</h6>
                            <div className={classes.bottomImagesGrid}>
                                {diary.photo_urls.map((url, index) => (
                                    <div key={index} className={classes.bottomImageItem}>
                                        <img 
                                            src={url} 
                                            alt={`추가 이미지 ${index + 1}`}
                                            className={classes.bottomImage}
                                        />
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>
                {/* 일기 텍스트 내용 */}
                {diary.content && <>
                    <div className={classes.diaryText}>
                        <div className="d-flex align-items-center justify-content-between">
                            <h6 className="small m-0">{`${y}년 ${m}월 ${d}일의 직관 일기`}</h6>
                            <button className="btn btn-sm btn-link small p-0 text-gray" onClick={handleDelete}>삭제</button>
                        </div>
                        
                        {schedule && <div className="p-4 mb-4 d-flex justify-content-center">
                            <div className="position-relative">
                                <Schedule {...schedule} />
                                {schedule?.highlightUrl && <>
                                    <Link
                                        className="btn btn-sm p-1 ps-2 pe-2 border-radius-0 btn-primary position-absolute" 
                                        title="하이라이트 보기" 
                                        style={{ top: 0, right: 0, borderBottomLeftRadius: 12 }} 
                                        to={`${URL.NEWS}${URL.HIGHLIGHT}/${schedule.id}`}
                                    >
                                        <i className="fas fa-play" style={{ fontSize: 14 }} />
                                    </Link>
                                </>}
                            </div>
                        </div>}
                        <p>{diary.content}</p>
                    </div>
                </>}
            </div>
        </div>
    </>
}
