import { useState, useEffect, useMemo } from "react";
import { useNavigate } from "react-router-dom";
import StatusBar from "/components/StatusBar";
import Loading from "/components/Loading";
import TeamSelect from "/components/TeamSelect";
import { useInit } from "/context/InitContext";
import { URL } from "/config/constants";
import axios from "/config/axios";

export default function MyCheerSong() {
    const { teams } = useInit();
    const navigate = useNavigate();
    const [selectedTeam, setSelectedTeam] = useState({});
    const [playerName, setPlayerName] = useState("");
    const [mood, setMood] = useState("");
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState(null);
    const [error, setError] = useState(null);
    const [teamPlayers, setTeamPlayers] = useState([]);
    const [showSuggestions, setShowSuggestions] = useState(false);
    const [isFocused, setIsFocused] = useState(false);
    const [activeTab, setActiveTab] = useState('create');
    const [savedSongs, setSavedSongs] = useState([]);
    const [musicQuery, setMusicQuery] = useState("");  // 음악 제목 검색
    const [searchResults, setSearchResults] = useState([]);
    const [selectedVideo, setSelectedVideo] = useState(null);
    const [youtubeLoading, setYoutubeLoading] = useState(false);
    const [youtubeResult, setYoutubeResult] = useState(null);

    // 팀 선택 시 선수 목록 로드
    useEffect(() => {
        if (selectedTeam.idKey) {
            axios.get(`/api/players/teams/${selectedTeam.idKey}/players`)
                .then(response => {
                    const players = Object.values(response.data).flat();
                    setTeamPlayers(players);
                })
                .catch(err => console.error("선수 목록 로딩 실패:", err));
        } else {
            setTeamPlayers([]);
        }
        setPlayerName("");
    }, [selectedTeam.idKey]);

    // 필터링된 선수 목록 (useMemo로 최적화)
    const filteredPlayers = useMemo(() => {
        if (!playerName || teamPlayers.length === 0 || !isFocused) {
            return [];
        }
        return teamPlayers.filter(player =>
            player.playerName?.includes(playerName)
        );
    }, [playerName, teamPlayers, isFocused]);

    // 자동완성 드롭다운 표시 여부
    useEffect(() => {
        setShowSuggestions(isFocused && filteredPlayers.length > 0);
    }, [isFocused, filteredPlayers]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!selectedTeam.id) {
            setError("팀을 선택해주세요");
            return;
        }

        setLoading(true);
        setError(null);
        setResult(null);

        try {
            const response = await axios.post("/api/ai/suno/generate", {
                team: selectedTeam.name,
                player_name: playerName.trim() || selectedTeam.name,
                mood: mood,
                team_id: selectedTeam.idKey,
            });

            setResult(response.data);

            // SpringBoot에서 이미 저장되었으므로 갤러리만 새로고침
            if (response.data.saved) {
                loadMySongs();
            } else if (response.data.saveError) {
                // 저장 실패 메시지 표시
                setError(`응원곡은 생성되었지만 저장에 실패했습니다: ${response.data.saveError}`);
            }
        } catch (err) {
            setError(err.response?.data?.error || err.response?.data?.message || err.message || "응원곡 생성에 실패했습니다");
        } finally {
            setLoading(false);
        }
    };

    const handlePlayerSelect = (player) => {
        setShowSuggestions(false);
        setIsFocused(false);
        setPlayerName(player.playerName);
    };

    // YouTube 음악 검색
    const handleMusicSearch = async () => {
        if (!musicQuery) {
            setError("음악 제목을 입력해주세요");
            return;
        }

        setError(null);
        setSearchResults([]);

        try {
            const response = await axios.post("/api/cheer-songs/youtube/search", {
                query: musicQuery,
                max_results: 5,
                copyright_free_only: false
            });

            setSearchResults(response.data.results);
            setSelectedVideo(null);
        } catch (err) {
            setError(err.response?.data?.detail || "음악 검색에 실패했습니다");
        }
    };

    // 검색 결과에서 영상 선택
    const handleVideoSelect = (video) => {
        setSelectedVideo(video);
    };

    const handleYoutubeCover = async () => {
        if (!selectedVideo) {
            setError("음악을 먼저 검색하고 선택해주세요");
            return;
        }

        if (!selectedTeam.id) {
            setError("팀을 선택해주세요");
            return;
        }

        setYoutubeLoading(true);
        setError(null);
        setYoutubeResult(null);

        try {
            const response = await axios.post("/api/ai/suno/youtube-cover", {
                youtube_url: selectedVideo.watch_url,
                player_name: playerName.trim() || selectedTeam.name,
                mood: mood || "신나는",
                custom_mode: true,
                instrumental: false,
                team_id: selectedTeam.idKey,
            });

            setYoutubeResult(response.data);

            // SpringBoot에서 이미 저장되었으므로 갤러리만 새로고침
            if (response.data.saved) {
                loadMySongs();
            } else if (response.data.saveError) {
                // 저장 실패 메시지 표시
                setError(`YouTube 응원곡은 생성되었지만 저장에 실패했습니다: ${response.data.saveError}`);
            }
        } catch (err) {
            setError(err.response?.data?.detail || err.message || "YouTube 커버 생성에 실패했습니다");
        } finally {
            setYoutubeLoading(false);
        }
    };


    // 내 응원곡 목록 로드
    const loadMySongs = async () => {
        try {
            const response = await axios.get("/api/cheer-songs/my");
            setSavedSongs(response.data || []);
        } catch (err) {
            console.error("내 응원곡 목록 로드 실패:", err);
            setSavedSongs([]);
        }
    };

    // 공유된 응원곡 목록 로드 (모든 사용자의 공유 응원곡)
    const [sharedSongs, setSharedSongs] = useState([]);
    const loadSharedSongs = async () => {
        try {
            const response = await axios.get("/api/cheer-songs/shared");
            setSharedSongs(response.data || []);
        } catch (err) {
            console.error("공유된 응원곡 목록 로드 실패:", err);
            setSharedSongs([]);
        }
    };

    // 공유 토글
    const handleToggleShare = async (songId) => {
        try {
            await axios.post(`/api/cheer-songs/${songId}/share`);
            loadMySongs();
            loadSharedSongs();
        } catch (err) {
            setError(err.response?.data?.message || "공유 설정 변경에 실패했습니다");
        }
    };

    // 삭제
    const handleDelete = async (songId) => {
        if (!confirm("정말 삭제하시겠습니까?")) return;

        try {
            await axios.delete(`/api/cheer-songs/${songId}`);
            loadMySongs();
            loadSharedSongs();
        } catch (err) {
            setError(err.response?.data?.message || "삭제에 실패했습니다");
        }
    };

    // 내 응원곡 (공유/비공개 모두 포함)
    const mySongs = savedSongs;

    // 갤러리 탭 활성화 시 응원곡 목록 로드
    useEffect(() => {
        if (activeTab === 'gallery') {
            loadMySongs();
            loadSharedSongs();
        }
    }, [activeTab]);

    const tabStyle = (isActive) => ({
        color: isActive ? 'var(--point-color)' : 'var(--text-dim-color)',
        borderBottom: isActive ? '3px solid var(--point-color)' : 'none',
        fontWeight: isActive ? '600' : '400',
        backgroundColor: isActive ? '#fff' : 'transparent'
    });

    const inputStyle = {
        borderColor: 'var(--gray-border-color)',
        borderRadius: '8px',
        padding: '12px',
        fontSize: '14px',
        color: 'var(--text-color)',
        backgroundColor: '#fff'
    };

    const labelStyle = { color: 'var(--text-color)', fontWeight: '600' };

    const buttonStyle = (isDisabled) => ({
        backgroundColor: isDisabled ? 'var(--gray-border-color)' : 'var(--point-color)',
        color: '#fff',
        border: 'none',
        borderRadius: '8px',
        padding: '12px 24px',
        fontSize: '16px',
        fontWeight: '600',
        width: '100%',
        cursor: isDisabled ? 'not-allowed' : 'pointer'
    });

    return (
        <>
            <StatusBar title="팀 응원곡 제작" />
            <section className="d-flex flex-column overflow-y-auto" style={{ marginTop: 0, minHeight: "100%", height: '100%', background: '#fff' }}>
                <div className="container p-4">
                    <label className="h3 mb-4">⚾ AI 응원곡 생성기</label>

                    {/* 탭 메뉴 */}
                    <ul className="nav nav-tabs mb-4 gap-8" style={{ borderBottom: '2px solid var(--gray-border-color)' }}>
                        {[
                            { id: 'create', label: '응원곡 생성' },
                            { id: 'gallery', label: '갤러리' }
                        ].map(tab => (
                            <li key={tab.id} className="nav-item">
                                <button
                                    className={`nav-link ${activeTab === tab.id ? 'active' : ''}`}
                                    onClick={() => setActiveTab(tab.id)}
                                    style={tabStyle(activeTab === tab.id)}
                                >
                                    {tab.label}
                                </button>
                            </li>
                        ))}
                    </ul>

                {/* 응원곡 생성 탭 */}
                {activeTab === 'create' && (
                    <div className="tab-content">
                        <label className="form-label mb-0" style={labelStyle}>팀 선택</label>
                        <form onSubmit={handleSubmit}>
                            <div className="d-flex p-0 flex-wrap" style={{ zoom: 0.85 }}>
                                {teams.map((team) => (
                                    <div key={team.id}>
                                        <div style={{ transform: 'scale(0.84)', width: '100%', height: '160px' }}>
                                            <TeamSelect team={team} selected={selectedTeam} setTeam={setSelectedTeam} />
                                        </div>
                                    </div>
                                ))}
                            </div>

                            <div className="mb-3 mt-3 position-relative">
                                <label className="form-label" style={labelStyle}>선수 이름 (선택)</label>
                                <input
                                    type="text"
                                    className="form-control form-control-lg"
                                    value={playerName}
                                    onChange={(e) => setPlayerName(e.target.value)}
                                    onFocus={() => {
                                        setIsFocused(true);
                                        if (playerName && filteredPlayers.length > 0) setShowSuggestions(true);
                                    }}
                                    onBlur={() => setTimeout(() => {
                                        setIsFocused(false);
                                        setShowSuggestions(false);
                                    }, 200)}
                                    placeholder={!isFocused && !playerName ? (selectedTeam.name || "팀을 먼저 선택해주세요") : ""}
                                    disabled={loading || !selectedTeam.id}
                                    style={inputStyle}
                                />
                                {showSuggestions && filteredPlayers.length > 0 && (
                                    <div className="position-absolute w-100 bg-white border rounded shadow-sm mt-1"
                                         style={{ maxHeight: '200px', overflowY: 'auto', zIndex: 1000 }}>
                                        {filteredPlayers.map((player, index) => (
                                            <div
                                                key={player.pNo || `player-${index}`}
                                                className="p-2"
                                                style={{ cursor: 'pointer' }}
                                                onMouseDown={(e) => {
                                                    e.preventDefault();
                                                    handlePlayerSelect(player);
                                                }}
                                                onMouseEnter={(e) => e.currentTarget.style.backgroundColor = '#f8f9fa'}
                                                onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'white'}
                                            >
                                                {player.playerName} ({player.position})
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>

                            <div className="mb-4">
                                <label className="form-label" style={labelStyle}>곡 분위기 (선택)</label>
                                <input
                                    type="text"
                                    className="form-control form-control-lg"
                                    value={mood}
                                    onChange={(e) => setMood(e.target.value)}
                                    placeholder="예) 힙합, 신나는, 감동적인"
                                    disabled={loading}
                                    style={inputStyle}
                                />
                            </div>

                            <button type="submit" className="btn btn-primary" disabled={loading} style={buttonStyle(loading)}>
                                {loading ? (
                                    <><span className="spinner-border spinner-border-sm me-2" />생성 중...</>
                                ) : (
                                    <><i className="fas fa-music me-2" />응원곡 만들기</>
                                )}
                            </button>
                        </form>

                        <div className="mt-4 p-4" style={{
                            border: '2px dashed var(--point-color)',
                            borderRadius: '12px',
                            backgroundColor: '#f8f9fa'
                        }}>
                            <label className="form-label mb-3" style={{...labelStyle, color: 'var(--point-color)'}}>
                                <i className="fab fa-youtube me-2" />YouTube 음악으로 응원가 만들기
                            </label>

                            {/* 음악 검색 */}
                            <div className="mb-3">
                                <input
                                    type="text"
                                    className="form-control form-control-lg mb-2"
                                    value={musicQuery}
                                    onChange={(e) => setMusicQuery(e.target.value)}
                                    onKeyPress={(e) => e.key === 'Enter' && handleMusicSearch()}
                                    placeholder="음악 제목을 입력하세요 (예: 아이유 좋은날)"
                                    disabled={youtubeLoading}
                                    style={inputStyle}
                                />
                                <button
                                    onClick={handleMusicSearch}
                                    className="btn btn-secondary w-100"
                                    disabled={!musicQuery || youtubeLoading}
                                    style={{
                                        backgroundColor: (!musicQuery || youtubeLoading) ? 'var(--gray-border-color)' : '#6c757d',
                                        color: '#fff',
                                        border: 'none',
                                        borderRadius: '8px',
                                        padding: '10px',
                                        fontWeight: '600'
                                    }}
                                >
                                    <i className="fas fa-search me-2" />음악 검색
                                </button>
                            </div>

                            {/* 검색 결과 */}
                            {searchResults.length > 0 && (
                                <div className="mb-3" style={{
                                    maxHeight: '300px',
                                    overflowY: 'auto',
                                    border: '1px solid var(--gray-border-color)',
                                    borderRadius: '8px',
                                    padding: '10px'
                                }}>
                                    <h6 className="mb-2" style={{ fontWeight: '600' }}>검색 결과:</h6>
                                    {searchResults.map((video, index) => (
                                        <div
                                            key={index}
                                            onClick={() => handleVideoSelect(video)}
                                            style={{
                                                display: 'flex',
                                                alignItems: 'center',
                                                padding: '10px',
                                                marginBottom: '8px',
                                                border: selectedVideo?.video_id === video.video_id ? '2px solid #FF0000' : '1px solid var(--gray-border-color)',
                                                borderRadius: '8px',
                                                cursor: 'pointer',
                                                backgroundColor: selectedVideo?.video_id === video.video_id ? '#fff5f5' : '#fff'
                                            }}
                                        >
                                            <img src={video.thumbnail} alt={video.title} style={{
                                                width: '80px',
                                                height: '60px',
                                                borderRadius: '4px',
                                                marginRight: '10px'
                                            }} />
                                            <div style={{ flex: 1 }}>
                                                <div style={{ fontWeight: '600', fontSize: '14px', marginBottom: '4px' }}>
                                                    {video.title}
                                                </div>
                                                <div style={{ fontSize: '12px', color: 'var(--text-dim-color)' }}>
                                                    {video.channel}
                                                </div>
                                            </div>
                                            {selectedVideo?.video_id === video.video_id && (
                                                <i className="fas fa-check-circle" style={{ color: '#FF0000', fontSize: '20px' }} />
                                            )}
                                        </div>
                                    ))}
                                </div>
                            )}

                            {/* 응원가 생성 버튼 */}
                            <button
                                onClick={handleYoutubeCover}
                                className="btn w-100"
                                disabled={youtubeLoading || !selectedVideo}
                                style={{
                                    ...buttonStyle(youtubeLoading || !selectedVideo),
                                    backgroundColor: (youtubeLoading || !selectedVideo) ? 'var(--gray-border-color)' : '#FF0000'
                                }}
                            >
                                {youtubeLoading ? (
                                    <><span className="spinner-border spinner-border-sm me-2" />응원가 생성 중...</>
                                ) : (
                                    <><i className="fas fa-magic me-2" />선택한 음악으로 응원가 만들기</>
                                )}
                            </button>
                        </div>

                        {loading && (
                            <div className="text-center mt-5">
                                <Loading />
                                <p className="mt-3" style={{ color: 'var(--text-dim-color)' }}>
                                    응원곡을 생성하고 있습니다. 약 1-2분 정도 소요됩니다...
                                </p>
                            </div>
                        )}

                        {youtubeLoading && (
                            <div className="text-center mt-5">
                                <Loading />
                                <p className="mt-3" style={{ color: 'var(--text-dim-color)' }}>
                                    <i className="fab fa-youtube me-2" />YouTube 음악을 분석하고 응원가를 생성하고 있습니다...<br />
                                    <span style={{ fontSize: '12px' }}>음악 다운로드 → 가사 추출 → 리듬 분석 → 응원가 생성 (약 2-3분)</span>
                                </p>
                            </div>
                        )}

                        {error && (
                            <div className="alert alert-danger mt-4" style={{
                                backgroundColor: '#f8d7da',
                                color: '#721c24',
                                border: '1px solid #f5c6cb',
                                borderRadius: '8px',
                                padding: '12px 16px'
                            }}>
                                {error}
                            </div>
                        )}

                        {youtubeResult && (
                            <div className="mt-4">
                                <div className="card shadow-lg" style={{
                                    border: '2px solid #FF0000',
                                    borderRadius: '12px',
                                    overflow: 'hidden'
                                }}>
                                    <div className="card-body p-4">
                                        <div className="d-flex align-items-center mb-3">
                                            {selectedTeam.id && (
                                                <img src={`/assets/icons/${selectedTeam.idKey}.png`}
                                                     alt={selectedTeam.name}
                                                     style={{ width: "40px", height: "40px" }}
                                                     className="me-3" />
                                            )}
                                            <div>
                                                <h5 className="card-title mb-1" style={{ fontWeight: '600', color: 'var(--text-color)' }}>
                                                    <i className="fab fa-youtube me-2" style={{ color: '#FF0000' }} />{youtubeResult.Title || "YouTube 응원가"}
                                                </h5>
                                                <p className="small mb-0" style={{ color: 'var(--text-dim-color)' }}>
                                                    <i className="far fa-clock me-1" />재생 시간: {youtubeResult.Duration}초
                                                </p>
                                            </div>
                                        </div>

                                        {youtubeResult["Original Lyrics"] && (
                                            <div className="mb-3">
                                                <h6 className="mb-2" style={{ fontWeight: '600', color: '#FF0000' }}>
                                                    <i className="fas fa-music me-2" />원본 가사 (추출됨)
                                                </h6>
                                                <pre className="p-3 rounded border" style={{
                                                    whiteSpace: "pre-wrap",
                                                    backgroundColor: '#fff3f3',
                                                    color: 'var(--text-color)',
                                                    borderColor: '#ffcccc',
                                                    fontSize: '12px',
                                                    maxHeight: '150px',
                                                    overflowY: 'auto'
                                                }}>
                                                    {youtubeResult["Original Lyrics"]}
                                                </pre>
                                            </div>
                                        )}

                                        {youtubeResult["New Lyrics"] && (
                                            <div className="mb-3">
                                                <h6 className="mb-2" style={{ fontWeight: '600', color: 'var(--point-color)' }}>
                                                    <i className="fas fa-star me-2" />새로운 응원가 가사
                                                </h6>
                                                <pre className="p-3 rounded border" style={{
                                                    whiteSpace: "pre-wrap",
                                                    backgroundColor: '#f8f9fa',
                                                    color: 'var(--text-color)',
                                                    borderColor: 'var(--gray-border-color)',
                                                    fontSize: '14px'
                                                }}>
                                                    {youtubeResult["New Lyrics"]}
                                                </pre>
                                            </div>
                                        )}

                                        {youtubeResult["Audio URL"] && (
                                            <div className="mb-3">
                                                <h6 className="mb-2" style={{ fontWeight: '600', color: 'var(--text-color)' }}>
                                                    <i className="fas fa-headphones me-2" />생성된 응원가
                                                </h6>
                                                <audio
                                                    controls
                                                    className="w-100"
                                                    style={{ borderRadius: "8px" }}
                                                    controlsList="nodownload"
                                                    onContextMenu={(e) => e.preventDefault()}
                                                >
                                                    <source src={youtubeResult["Audio URL"]} type="audio/mpeg" />
                                                    브라우저가 오디오 재생을 지원하지 않습니다.
                                                </audio>
                                            </div>
                                        )}

                                        <div className="d-flex gap-2">
                                            <button
                                                onClick={() => setActiveTab('gallery')}
                                                className="btn w-100"
                                                style={{
                                                    backgroundColor: 'var(--point-color)',
                                                    color: '#fff',
                                                    border: 'none',
                                                    borderRadius: '8px',
                                                    padding: '10px 20px',
                                                    fontSize: '14px',
                                                    fontWeight: '600'
                                                }}>
                                                <i className="fas fa-images me-2" />갤러리에서 보기
                                            </button>
                                        </div>
                                        <div className="mt-2 text-center">
                                            <p className="small mb-0" style={{ color: 'var(--text-dim-color)' }}>
                                                <i className="fas fa-check-circle me-1" style={{ color: '#28a745' }} />
                                                YouTube 기반 응원곡이 자동으로 저장되었습니다!
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}

                        {result && (
                            <div className="mt-4">
                                <div className="card shadow-lg" style={{
                                    border: '1px solid var(--gray-border-color)',
                                    borderRadius: '12px',
                                    overflow: 'hidden'
                                }}>
                                    <div className="card-body p-4">
                                        <div className="d-flex align-items-center mb-3">
                                            {selectedTeam.id && (
                                                <img src={`/assets/icons/${selectedTeam.idKey}.png`}
                                                     alt={selectedTeam.name}
                                                     style={{ width: "40px", height: "40px" }}
                                                     className="me-3" />
                                            )}
                                            <div>
                                                <h5 className="card-title mb-1" style={{ fontWeight: '600', color: 'var(--text-color)' }}>
                                                    {result.Title || "응원곡"}
                                                </h5>
                                                <p className="small mb-0" style={{ color: 'var(--text-dim-color)' }}>
                                                    <i className="far fa-clock me-1" />재생 시간: {result.Duration}초
                                                </p>
                                            </div>
                                        </div>

                                        {result.Lyrics && (
                                            <div className="mb-3">
                                                <h6 className="mb-2" style={{ fontWeight: '600', color: 'var(--text-color)' }}>
                                                    <i className="fas fa-align-left me-2" />가사
                                                </h6>
                                                <pre className="p-3 rounded border" style={{
                                                    whiteSpace: "pre-wrap",
                                                    backgroundColor: '#f8f9fa',
                                                    color: 'var(--text-color)',
                                                    borderColor: 'var(--gray-border-color)',
                                                    fontSize: '14px'
                                                }}>
                                                    {result.Lyrics}
                                                </pre>
                                            </div>
                                        )}

                                        {result["Audio URL"] && (
                                            <div className="mb-3">
                                                <h6 className="mb-2" style={{ fontWeight: '600', color: 'var(--text-color)' }}>
                                                    <i className="fas fa-headphones me-2" />오디오
                                                </h6>
                                                <audio controls className="w-100" style={{ borderRadius: "8px" }}>
                                                    <source src={result["Audio URL"]} type="audio/mpeg" />
                                                    브라우저가 오디오 재생을 지원하지 않습니다.
                                                </audio>
                                            </div>
                                        )}

                                        <div className="d-flex gap-2">
                                            {result["Audio URL"] && (
                                                <a href={result["Audio URL"]}
                                                   download={`${selectedTeam?.name}_${playerName}_응원곡.mp3`}
                                                   className="btn w-100"
                                                   style={{
                                                       backgroundColor: '#28a745',
                                                       color: '#fff',
                                                       border: 'none',
                                                       borderRadius: '8px',
                                                       padding: '10px 20px',
                                                       fontSize: '14px',
                                                       fontWeight: '600',
                                                       textDecoration: 'none',
                                                       display: 'flex',
                                                       alignItems: 'center',
                                                       justifyContent: 'center'
                                                   }}>
                                                    <i className="fas fa-download me-2" />다운로드
                                                </a>
                                            )}
                                            <button
                                                onClick={() => setActiveTab('gallery')}
                                                className="btn w-100"
                                                style={{
                                                    backgroundColor: 'var(--point-color)',
                                                    color: '#fff',
                                                    border: 'none',
                                                    borderRadius: '8px',
                                                    padding: '10px 20px',
                                                    fontSize: '14px',
                                                    fontWeight: '600'
                                                }}>
                                                <i className="fas fa-images me-2" />갤러리에서 보기
                                            </button>
                                        </div>
                                        <div className="mt-2 text-center">
                                            <p className="small mb-0" style={{ color: 'var(--text-dim-color)' }}>
                                                <i className="fas fa-check-circle me-1" style={{ color: '#28a745' }} />
                                                응원곡이 자동으로 저장되었습니다!
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* 갤러리 탭 */}
                {activeTab === 'gallery' && (
                    <div className="tab-content">
                        {/* 내 응원곡 섹션 */}
                        <div className="mb-5">
                            <label className="mb-3" style={labelStyle}>내 응원곡</label>
                            <div className="row">
                                {mySongs.map((song, index) => (
                                    <div key={index} className="col-md-6 col-lg-6 mb-3">
                                        <div className="card" style={{
                                            border: '1px solid var(--gray-border-color)',
                                            borderRadius: '12px',
                                            overflow: 'hidden',
                                        }}>
                                            <div className="card-body" style={{ padding: '16px' }}>
                                                <h6 className="card-title mb-2" style={{
                                                    color: 'var(--text-color)',
                                                    fontSize: '16px',
                                                    fontWeight: '600'
                                                }}>
                                                    {song.title || "응원곡"}
                                                </h6>
                                                <p className="card-text small mb-3" style={{
                                                    color: 'var(--text-dim-color)',
                                                    fontSize: '12px'
                                                }}>
                                                    선수: {song.playerName}<br/>
                                                    분위기: {song.mood || 'N/A'}<br/>
                                                    재생시간: {song.duration}초
                                                </p>
                                                {song.audioUrl && (
                                                    <audio
                                                        controls
                                                        className="w-100 mb-3"
                                                        style={{ height: '40px' }}
                                                        controlsList={song.sourceType === "YOUTUBE_COVER" ? "nodownload" : ""}
                                                        onContextMenu={song.sourceType === "YOUTUBE_COVER" ? (e) => e.preventDefault() : undefined}
                                                    >
                                                        <source src={song.audioUrl} type="audio/mpeg" />
                                                    </audio>
                                                )}
                                                {song.lyrics && (
                                                    <details className="mb-2">
                                                        <summary style={{
                                                            cursor: 'pointer',
                                                            color: 'var(--point-color)',
                                                            fontSize: '14px',
                                                            fontWeight: '500'
                                                        }}>
                                                            가사 보기
                                                        </summary>
                                                        <pre className="mt-2 p-2 rounded" style={{
                                                            whiteSpace: 'pre-wrap',
                                                            fontSize: '12px',
                                                            backgroundColor: '#f8f9fa',
                                                            border: '1px solid var(--gray-border-color)',
                                                            color: 'var(--text-color)',
                                                            maxHeight: '200px',
                                                            overflowY: 'auto'
                                                        }}>
                                                            {song.lyrics}
                                                        </pre>
                                                    </details>
                                                )}
                                                <div className="d-flex gap-2 mt-2">
                                                    {song.sourceType !== "YOUTUBE_COVER" && (
                                                        <button
                                                            onClick={() => handleToggleShare(song.songId)}
                                                            className="btn btn-sm flex-grow-1"
                                                            style={{
                                                                backgroundColor: song.isShared ? '#6c757d' : 'var(--point-color)',
                                                                color: '#fff',
                                                                border: 'none',
                                                                borderRadius: '6px',
                                                                fontSize: '12px',
                                                                fontWeight: '500'
                                                            }}
                                                        >
                                                            <i className={`fas ${song.isShared ? 'fa-lock' : 'fa-share-alt'} me-1`} />
                                                            {song.isShared ? '공유 취소' : '공유'}
                                                        </button>
                                                    )}
                                                    <button
                                                        onClick={() => handleDelete(song.songId)}
                                                        className="btn btn-sm flex-grow-1"
                                                        style={{
                                                            backgroundColor: '#dc3545',
                                                            color: '#fff',
                                                            border: 'none',
                                                            borderRadius: '6px',
                                                            fontSize: '12px',
                                                            fontWeight: '500'
                                                        }}
                                                    >
                                                        <i className="fas fa-trash me-1" />삭제
                                                    </button>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                ))}
                                {mySongs.length === 0 && (
                                    <div className="col-12 text-center py-5">
                                        <p style={{
                                            color: 'var(--text-dim-color)',
                                            fontSize: '16px',
                                            fontStyle: 'italic'
                                        }}>
                                            저장된 응원곡이 없습니다.
                                        </p>
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* 공유된 응원곡 섹션 */}
                        <div>
                            <label className="mb-3" style={labelStyle}>공유된 응원곡</label>
                            <div className="row">
                                {sharedSongs.map((song, index) => (
                                    <div key={index} className="col-md-6 col-lg-6 mb-3">
                                        <div className="card" style={{
                                            border: '1px solid var(--gray-border-color)',
                                            borderRadius: '12px',
                                            overflow: 'hidden',
                                            boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                                        }}>
                                            <div className="card-body" style={{ padding: '16px' }}>
                                                <h6 className="card-title mb-2" style={{
                                                    color: 'var(--text-color)',
                                                    fontSize: '16px',
                                                    fontWeight: '600'
                                                }}>
                                                    {song.title || "응원곡"}
                                                </h6>
                                                <p className="card-text small mb-3" style={{
                                                    color: 'var(--text-dim-color)',
                                                    fontSize: '12px'
                                                }}>
                                                    작성자: {song.userName || 'Unknown'}<br/>
                                                    선수: {song.playerName}<br/>
                                                    분위기: {song.mood || 'N/A'}<br/>
                                                    재생시간: {song.duration}초
                                                </p>
                                                {song.audioUrl && (
                                                    <audio
                                                        controls
                                                        className="w-100 mb-3"
                                                        style={{ height: '40px' }}
                                                        controlsList={song.sourceType === "YOUTUBE_COVER" ? "nodownload" : ""}
                                                        onContextMenu={song.sourceType === "YOUTUBE_COVER" ? (e) => e.preventDefault() : undefined}
                                                    >
                                                        <source src={song.audioUrl} type="audio/mpeg" />
                                                    </audio>
                                                )}
                                                {song.lyrics && (
                                                    <details className="mb-2">
                                                        <summary style={{
                                                            cursor: 'pointer',
                                                            color: 'var(--point-color)',
                                                            fontSize: '14px',
                                                            fontWeight: '500'
                                                        }}>
                                                            가사 보기
                                                        </summary>
                                                        <pre className="mt-2 p-2 rounded" style={{
                                                            whiteSpace: 'pre-wrap',
                                                            fontSize: '12px',
                                                            backgroundColor: '#f8f9fa',
                                                            border: '1px solid var(--gray-border-color)',
                                                            color: 'var(--text-color)',
                                                            maxHeight: '200px',
                                                            overflowY: 'auto'
                                                        }}>
                                                            {song.lyrics}
                                                        </pre>
                                                    </details>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                ))}
                                {sharedSongs.length === 0 && (
                                    <div className="col-12 text-center py-5">
                                        <p style={{
                                            color: 'var(--text-dim-color)',
                                            fontSize: '16px',
                                            fontStyle: 'italic'
                                        }}>
                                            공유된 응원곡이 없습니다.
                                        </p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                )}
                </div>
            </section>
        </>
    );
}
