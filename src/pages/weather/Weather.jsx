import { useState, useEffect, useMemo } from 'react';
import { useAuth } from "/context/AuthContext";
import Loading from "/components/Loading";
import classes from './Weather.module.scss';
import { weathers, weathersClass } from './Weather.constants';

// KBO 구장들의 좌표 정보
const stadiums = [
    { name: '잠실야구장', team: 'LG', lat: 37.5122, lon: 127.0722, id: 'jamsil-lg', teamCode: 'LG', color: "rgba(222, 36, 36, 1)" },
    { name: '잠실야구장', team: '두산', lat: 37.5122, lon: 127.0722, id: 'jamsil-ob', teamCode: 'OB', color: "rgba(19, 31, 92, 1)" },
    { name: '고척스카이돔', team: '키움', lat: 37.4982, lon: 126.8670, id: 'gocheok', teamCode: 'WO', color: "rgba(141, 37, 37, 1)" },
    { name: '인천SSG랜더스필드', team: 'SSG', lat: 37.4370, lon: 126.6930, id: 'incheon', teamCode: 'SK', color: "rgba(58, 145, 73, 1)" },
    { name: '수원KT위즈파크', team: 'KT', lat: 37.2997, lon: 127.0099, id: 'suwon', teamCode: 'KT', color: "rgba(58, 58, 58, 1)" },
    { name: '대전한화생명이글스파크', team: '한화', lat: 36.3171, lon: 127.4290, id: 'daejeon', teamCode: 'HH', color: "rgba(255, 109, 36, 1)" },
    { name: '광주기아챔피언스필드', team: 'KIA', lat: 35.1681, lon: 126.8890, id: 'gwangju', teamCode: 'HT', color: "rgba(203, 45, 71, 1)" },
    { name: '대구삼성라이온즈파크', team: '삼성', lat: 35.8410, lon: 128.6816, id: 'daegu', teamCode: 'SS', color: "rgba(48, 113, 233, 1)" },
    { name: '사직야구장', team: '롯데', lat: 35.1940, lon: 129.0615, id: 'sajik', teamCode: 'LT', color: "rgba(55, 44, 172, 1)" },
    { name: '창원NC파크', team: 'NC', lat: 35.2226, lon: 128.5823, id: 'changwon', teamCode: 'NC', color: "rgba(30, 65, 162, 1)" }
];

export default function Weather() {
    const { auth: { team } = {  } } = useAuth();
    const [weatherData, setWeatherData] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [currentIndex, setCurrentIndex] = useState(team?.idKey || 'LG');

    // OpenWeatherMap API 키 (환경변수에서 가져오기)
    const API_KEY = import.meta.env.VITE_WEATHER_API_KEY || '57f0454286b3d28480b0d0b48fc5185a';
    const API_URL = 'https://api.openweathermap.org/data/2.5/weather';

    useEffect(() => {
        const fetchWeatherData = async () => {
            setLoading(true);
            setError(null);
            
            try {
                const promises = stadiums.map(async (stadium) => {
                    const response = await fetch(
                        `${API_URL}?lat=${stadium.lat}&lon=${stadium.lon}&appid=${API_KEY}&units=metric`
                    );
                    
                    if (!response.ok) {
                        throw new Error(`날씨 데이터를 가져올 수 없습니다: ${stadium.name}`);
                    }
                    
                    const data = await response.json();
                    return {
                        id: stadium.id,
                        name: stadium.name,
                        team: stadium.team,
                        teamCode: stadium.teamCode,
                        temperature: Math.round(data.main.temp),
                        feelsLike: Math.round(data.main.feels_like),
                        description: data.weather[0].description,
                        icon: data.weather[0].icon,
                        main: data.weather[0].main,
                        humidity: data.main.humidity,
                        windSpeed: data.wind.speed,
                        pressure: data.main.pressure,
                        clouds: data.clouds.all
                    };
                });

                const results = await Promise.all(promises);
                const weatherMap = {};
                results.forEach(result => {
                    weatherMap[result.id] = result;
                });
                
                setWeatherData(weatherMap);
            } catch (err) {
                setError(err.message);
                console.error('날씨 데이터 로딩 실패:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchWeatherData();
    }, []);

    const sortedStadiums = useMemo(() => stadiums.sort((a, b) => {
        if (a.teamCode === currentIndex) return -1;
        if (b.teamCode === currentIndex) return 1;
        else return a.name > b.name ? 1 : -1;
    }), [currentIndex]);

    const getTeamLogoPath = (teamCode) => {
        return `/assets/icons/${teamCode}.png`;
    };

    const [currentStadium] = stadiums.filter(({ teamCode }) => teamCode === currentIndex);
    
    // currentStadium이 undefined인 경우 기본값 설정
    const safeStadium = currentStadium || stadiums[0]; // 없으면 첫 번째 구장(잠실)
    const currentWeather = weatherData[safeStadium.id];

    return (
        <section className={`d-flex mt-4 gap-20 overflow-hidden ${classes.weatherContainer}`}>
            {/* 인디케이터 */}
            <div className="d-flex flex-column bg-gray border border-gray overflow-hidden border-radius-20">
                {sortedStadiums.map((stadium) => (
                    <button
                        key={stadium.id}
                        className={`btn d-flex btn-none align-items-center ${classes.button} ${stadium.teamCode === currentIndex ? classes.active : ""}`}
                        onClick={() => setCurrentIndex(stadium.teamCode)}
                        aria-label={`${stadium.name} 날씨 보기`}
                    >
                        <img 
                            src={getTeamLogoPath(stadium.teamCode)} 
                            alt={stadium.team}
                            className={classes.teamLogo}
                            onError={(e) => e.target.style.display = 'none'}
                        />
                        <div className={classes.stadiumInfo}>
                            <h3 className={classes.stadiumName}>{stadium.name}</h3>
                            <p className={classes.teamName}>{stadium.team}</p>
                        </div>
                    </button>
                ))}
            </div>
            <div className="d-flex flex-column flex-grow border-radius-20 overflow-hidden" style={{ background: safeStadium.color, width: 450 }}>
                {error && <span className="text-white text-center m-4">날씨 정보를 불러올 수 없습니다: <br/>{error}</span>}
                {loading && <Loading />}
                {!loading && currentWeather && (
                    <>
                        {/* 구장 헤더 */}
                        <div className="pt-2 p-3 d-flex flex-column align-items-center" style={{ background: 'rgba(0,0,0,.2)' }}>
                            <img 
                                src={getTeamLogoPath(safeStadium.teamCode)} 
                                alt={safeStadium.team}
                                width="80px"
                                onError={(e) => {
                                    e.target.style.display = 'none';
                                }}
                            />
                            <div className={classes.stadiumInfo}>
                                <h3 className={`${classes.stadiumName} text-white`}>{safeStadium.name} 현재 날씨</h3>
                            </div>
                        </div>
                        {/* 메인 날씨 정보 */}
                        <div className={classes.mainWeather}>
                            <i className={`fa-solid fa-${weathersClass[currentWeather.main]} text-white mt-4 mb-3`} style={{ fontSize: 60 }} />

                            <div className={classes.temperatureSection}>
                                <div className={classes.temperature}>
                                    {currentWeather.temperature}°C
                                </div>
                                <div className={classes.description}>
                                    {weathers[currentWeather.description]}
                                </div>
                                <div className={classes.feelsLike}>
                                    체감 <span style={{ fontSize: '1.5em' }}>{currentWeather.feelsLike}°C</span>
                                </div>
                            </div>
                        </div>

                        {/* 상세 정보 그리드 */}
                        <div className={classes.detailsGrid}>
                            <div className={classes.detailCard}>
                                <div className={classes.detailIcon}>💧</div>
                                <div className={classes.detailLabel}>습도</div>
                                <div className={classes.detailValue}>{currentWeather.humidity}%</div>
                            </div>
                            <div className={classes.detailCard}>
                                <div className={classes.detailIcon}>💨</div>
                                <div className={classes.detailLabel}>풍속</div>
                                <div className={classes.detailValue}>{currentWeather.windSpeed}m/s</div>
                            </div>
                            <div className={classes.detailCard}>
                                <div className={classes.detailIcon}>🌡️</div>
                                <div className={classes.detailLabel}>기압</div>
                                <div className={classes.detailValue}>{currentWeather.pressure}hPa</div>
                            </div>
                            <div className={classes.detailCard}>
                                <div className={classes.detailIcon}>☁️</div>
                                <div className={classes.detailLabel}>구름</div>
                                <div className={classes.detailValue}>{currentWeather.clouds}%</div>
                            </div>
                        </div>
                    </>
                )}
            </div>
        </section>
    );
}
