import { Link } from "react-router-dom";
import TeamLogo from "/components/TeamLogo";
import { URL } from "/config/constants";
import { useInit } from "/context/InitContext";

export default function HighlightsListItem({ 
    id, highlightThumb,
    homeTeam, awayTeam, stadium, homeTeamScore, awayTeamScore,
    gameDate, gameTime
 }) {
    const { teams } = useInit();
    const [homeT = { name: homeTeam }] = teams.filter(({ name }) => name.includes(homeTeam));
    const [awayT = { name: awayTeam }] = teams.filter(({ name }) => name.includes(awayTeam));

    const where = homeT.stadium?.includes(stadium) ? homeT.stadium : awayT.stadium?.includes(stadium) ? awayT.stadium : stadium;

    return <>
        <div className="card border-gray overflow-hidden border-radius-12 flex-row align-items-center gap-8" style={{ height: 200, minHeight: 200 }}>
            <div 
                style={{ minWidth: 320, height: 200, background: `url('${highlightThumb}') center / cover no-repeat` }} 
            />
            <div className="p-3" style={{ flexGrow: 1 }}>
                <Link to={`${URL.NEWS}${URL.HIGHLIGHT}/${id}`} className="h5">{`${gameDate} ${homeTeam} VS ${awayTeam}`}</Link>
                <div className="mt-3 mb-3 border-radius-12 p-2 bg-gray" style={{ width: 200 }}>
                    <table>
                        <colgroup>
                            <col width="150px"></col>
                            <col width="50px"></col>
                        </colgroup>
                        <tbody>
                            <tr>
                                <td className="d-flex align-items-center gap-8">
                                    {homeT.idKey && <TeamLogo name={homeT.idKey} small />}
                                    <span>{homeT.name}</span>
                                </td>
                                <td><b className={homeTeamScore > awayTeamScore ? "point" : ""}>{homeTeamScore}</b></td>
                            </tr>
                            <tr>
                                <td className="d-flex align-items-center gap-8">
                                    {awayT.idKey && <TeamLogo name={awayT.idKey} small />}
                                    <span>{awayT.name}</span>
                                </td>
                                <td><b className={homeTeamScore < awayTeamScore ? "point" : ""}>{awayTeamScore}</b></td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <p className="small text-gray d-flex gap-8">{where} | {gameTime}</p>
            </div>
        </div>
    </>;
}


