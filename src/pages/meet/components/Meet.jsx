import { Link } from 'react-router-dom';
import { makeCalendar, getDate } from '/components';
import { URL } from '/config/constants';

export default function Meet({ id, meetAt, title, goal, totalApply = 0, ref, active, apply, writer }) {
    // 모임 상태에 따른 버튼 텍스트와 스타일 결정
    const getButtonInfo = () => {
        if (!active) {
            return { text: '모집마감', className: 'btn btn-secondary btn-sm mt-1' };
        }
        // if (applies >= goal) {
        //     return { text: '모집 임박', className: 'btn btn-warning btn-sm mt-1' };
        // }
        if (apply) {
            return { text: '지원완료', className: 'btn btn-success btn-sm mt-1' };
        }
        return { text: '모집중', className: 'btn btn-outline-primary btn-sm mt-1' };
    };

    const buttonInfo = getButtonInfo();

    return <>
        <Link to={`${URL.MEET}/${id}`} className="text-decoration-none">
            <li className="border border-gray border-radius-12 d-flex align-items-center p-3 gap-20" ref={ref}>
                {makeCalendar(meetAt)}
                <div className="d-flex flex-column flex-grow">
                    <div className="d-flex align-items-center gap-2 mb-1">
                        <span className="text-truncate d-inline-block" style={{ maxWidth: 190 }}>{title}</span>
                        {writer && <span className="badge bg-primary" style={{ fontSize: 10 }}>내글</span>}
                    </div>
                    <small className="text-gray"><i className="fas fa-calendar-check me-1" /> {getDate(meetAt)}</small>
                </div>
                <div className="d-flex flex-column">
                    <small className="text-gray text-right text-nowrap" style={{ fontSize: 12 }}>
                        신청 {totalApply}명 / 목표 {goal}명
                    </small>
                    <span className={buttonInfo.className}>{buttonInfo.text}</span>
                </div>
            </li>
        </Link>
    </>
}