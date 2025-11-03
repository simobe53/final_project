import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { getDate } from "/components";
import { useAuth } from "/context/AuthContext";
import { useInit } from "/context/InitContext";
import Modal from "/components/Modal";
import TeamLogo from "/components/TeamLogo";
import TeamSelect from "/components/TeamSelect";
import { URL } from "/config/constants";

export default function Game({ setTeam: setSelectedTeam, id, showAt, hometeam: ht, awayteam: at, homeLineup: { pitcher: homePitcher, ...homeLineup } = {}, awayLineup: { pitcher: awayPitcher, ...awayLineup } = {}}) {
    const { auth: { account, team: myteam } } = useAuth();
    const { teams, players } = useInit();
    const [open, setOpen] = useState(false);
    const homeTeam = teams.filter(({ id }) => id === ht)[0] || {};
    const awayTeam = teams.filter(({ id }) => id === at)[0] || {};
    const [team, setTeam] = useState(account ? homeTeam : myteam);
    const navigate = useNavigate();

    const handleEnter = () => {
        navigate(`${URL.SIMULATION}/${id}`);
        setSelectedTeam(team.id);
        setOpen(false);
    }

    const getPlayerName = (id) => players[id]?.playerName || '미정';

    return <>
        <div className="d-flex flex-column border border-gray pt-2 pb-3 ps-3 pe-3 pointer" style={{ marginTop: -1 }} key={id} onClick={() => setOpen(true)}>
            <small className="mb-2">{getDate(showAt)}</small>
            <div className="d-flex justify-content-start align-items-center gap-2 mb-2 mt-1">
                <TeamLogo name={homeTeam.idKey} zoom={0.7} />
                <b className="text-nowrap">{homeTeam.name}</b>
                <small style={{ fontSize: 11, lineHeight: 1, letterSpacing: '-0.2px' }} className="border border-point border-radius-12 point p-1">Home</small>
                <small className="text-nowrap ms-auto me-1">선발 : {getPlayerName(homePitcher)}</small>
                <div className="border border-radius-12 pt-1 pb-1 p-2" style={{ background: 'rgba(0,0,0,0.05)', fontSize: 14 }}>
                    {Object.keys(homeLineup).map(id => <small key={id}>{`${id.replace('batting', '')} ${getPlayerName(homeLineup[id])} `}</small>)}
                </div>
            </div>
            <div className="d-flex justify-content-start align-items-center gap-2">
                <TeamLogo name={awayTeam.idKey} zoom={0.7} />
                <b className="text-nowrap">{awayTeam.name}</b>
                <small className="text-nowrap ms-auto me-1">선발 : {getPlayerName(awayPitcher)}</small>
                <div className="border border-radius-12 pt-1 pb-1 p-2" style={{ background: 'rgba(0,0,0,0.05)', fontSize: 14 }}>
                    {Object.keys(awayLineup).map(id => <small key={id}>{`${id.replace('batting', '')} ${getPlayerName(awayLineup[id])} `}</small>)}
                </div>
            </div>
        </div>
        {open && <>
            <Modal title="팀 선택" onClose={() => setOpen(false)}>
                <div className="d-flex gap-20">
                    <TeamSelect team={homeTeam} setTeam={setTeam} selected={team} />
                    <TeamSelect team={awayTeam} setTeam={setTeam} selected={team} />
                </div>
                <button className="btn p-4 full-width point-bg" onClick={handleEnter}>{team.name} 팬으로 입장하기</button>
            </Modal>
        </>}
    </>
}
