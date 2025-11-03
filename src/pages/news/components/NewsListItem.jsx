import TeamLogo from "/components/TeamLogo";
import { Link } from "react-router-dom";
import { URL } from "/config/constants";
import { useInit } from "/context/InitContext";

export default function NewsListItem({ id, imageUrl, title, link, content, teamName, teamId }) {
    const { activateChat, setPlaceRedirect } = useInit();

    const handleSummaryClick = (e) => {
        e.preventDefault();
        e.stopPropagation();

        // 뉴스 요약 시에는 placeRedirect 초기화
        setPlaceRedirect(null);

        // 챗봇 열기 + 자동 메시지 전송
        // 실제 메시지: "뉴스 {id} 요약해줘", 화면 표시: "뉴스 요약해줘"
        activateChat(true, `뉴스 ${id} 요약해줘`, '뉴스 요약해줘');
    };

    return <>
        <Link
            to={`${URL.NEWS}/${id}`}
            className="card border-radius-12 border-gray p-2 flex-row align-items-center gap-12"
            style={{ textDecoration: 'none', color: 'inherit' }}
        >
            {imageUrl && (
                <div
                    className="border-gray border-radius-12"
                    alt={title}
                    style={{ minWidth: 160, height: 120, background: `url('${imageUrl}') top center / cover no-repeat` }}
                    onError={(e) => e.target.style.display = 'none'}
                />
            )}
            <div className="flex-grow p-3 pt-0 pb-0 overflow-hidden">
                <div className="d-flex align-items-center gap-8 mb-1" style={{ zoom: 0.9 }}>
                    {teamId && <TeamLogo name={teamId} small />}
                    {teamName && <span className="small text-gray">{teamName}</span>}
                </div>
                <p className="pt-1 pb-2 text-truncate">{title}</p>
                <small className="small text-gray mt-auto text-truncate-2">{content}</small>
            </div>
        </Link>
    </>;
}


