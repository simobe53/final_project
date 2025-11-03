import { useInit } from "/context/InitContext";
import TeamLogo from "/components/TeamLogo";

export default function Schedule({
    id, homeTeam, awayTeam, stadium, homeTeamScore, awayTeamScore,
    gameDate, gameTime = '', nowInning = ''
}) {
    const { teams } = useInit();
    const [homeT = { name: homeTeam }] = teams.filter(({ name }) => name.includes(homeTeam));
    const [awayT = { name: awayTeam }] = teams.filter(({ name }) => name.includes(awayTeam));

    const nowPlaying = nowInning?.includes("회");
    const finished = nowInning === "종료";

    const [hour, min] = gameTime.split(":");


    return <>
    <div className={`card border-${nowPlaying ? 'point' : 'gray'} p-3`} style={{ flex: '1 1 200px', minWidth: 240, maxWidth: 240 }}>
        {nowPlaying && <b className="bg-point p-2 text-white small fw-500 pb-1 pt-1 position-absolute" style={{ top: 0, right: 0, borderBottomLeftRadius: 8 }}>{nowInning}</b>}
        {finished && <span className="bg-gray p-2 small fw-500 pb-1 pt-1 position-absolute" style={{ top: 0, right: 0, borderBottomLeftRadius: 8 }}>{nowInning}</span>}
        <p className={`small ${finished ? "text-gray" : "point"} fw-500 pb-2`} style={{ fontSize: 13 }}>{stadium} | {gameDate} {hour}:{min}</p>
        <div className="d-flex flex-column align-items-start gap-8">
            <table>
                <colgroup>
                    <col width="180px"></col>
                    <col width="50px"></col>
                </colgroup>
                <tbody>
                    <tr>
                        <td className="d-flex align-items-center gap-8">
                            {homeT.idKey && <TeamLogo name={homeT.idKey} small />}
                            <span className="d-flex align-items-center gap-1">
                                {homeT.name}
                                <small className="bg-secondary text-white pt-0 pb-0 p-1" style={{ fontSize: '0.7rem', borderRadius: 4 }}>홈</small>
                            </span>
                        </td>
                        <td align="right"><b className={homeTeamScore > awayTeamScore ? "point" : ""}>{homeTeamScore}</b></td>
                    </tr>
                    <tr>
                        <td className="d-flex align-items-center gap-8">
                            {awayT.idKey && <TeamLogo name={awayT.idKey} small />}
                            <span>{awayT.name}</span>
                        </td>
                        <td align="right"><b className={homeTeamScore < awayTeamScore ? "point" : ""}>{awayTeamScore}</b></td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
</>
}