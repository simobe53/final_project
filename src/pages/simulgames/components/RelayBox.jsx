import RelayChart from './RelayChart';
import './RelayBox.scss';


export default function RelayBox({ data, activePath }){
const { player, bso, outStatus, probabilities, gameInfo } = data;

    return (
        <div id="RelayBox" >
            <div className='player_info'>
                <div className="img border-gray border border-radius-20 overflow-hidden">
                    <img src={player.image} alt={player.name} />
                </div>
                <div>
                    <div className='player'>
                        <span >{player.name}</span>
                        <span >{player.position}</span>
                        <span >타율: {player.average}</span>
                    </div>
                    <div className='info d-inline-flex gap-2'>
                        <span>타석 {bso.bat}</span> |
                        <span>타수 {bso.hitter}</span> |
                        <span>안타 {bso.safty}</span> |
                        <span>득점 {bso.score}</span> |
                        <span>홈런 {bso.HR}</span>
                    </div>
                </div>
            </div>

            <div className="play_box pt-1 pb-1">
                <div className="player_out ps-0">
                    <div className='d-flex align-items-center gap-2 mb-2'>
                        <span className="h6 m-0">{outStatus.name}</span>
                        <span> : </span>
                        <span className="h6 point m-0">{outStatus.Status}</span>
                        <b className="small">[ AI 분석 확률 ]</b>
                    </div>
                    <RelayChart data={probabilities} result={outStatus.Status} />
                </div>
                
                <div className='game_info ps-0 pb-1'>
                    {gameInfo.map((info, index) => info && (
                        <div key={index} className="justify-between items-center last:border-b-0">
                            <div>
                                <span className={`rounded ${
                                    info.SBH === '스트라이크' ? 'text-red' :
                                    info.SBH === '볼' ? 'text-green' :
                                    ''
                                }`}>
                                    {info.SBH}
                                </span>
                                <span className="p-2 small">{info.CF}</span>
                                {info.score && (
                                    <span className="small text-gray">{info.score}</span>
                                )}
                            </div>
                            <div>
                                {info.baseInfo?.map((d, i) => <p key={i} className="small p-1">{d}</p>)}
                            </div>
                        </div>
                    ))}
                </div>
                
            </div>
        </div>
    )
        
}