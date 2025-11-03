import { useEffect, useRef, useState } from 'react';
import { useAuth } from '/context/AuthContext';

export default function LocationMap({ meetId, participants }) {
    const { auth } = useAuth();
    // 동적 WebSocket 설정 (Meet 위치공유 전용)
    const hostname = location.hostname;
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const port = hostname === 'localhost' ? ':8080' : '';
    const mapRef = useRef(null);
    const [map, setMap] = useState(null);
    const [markers, setMarkers] = useState({});
    const [myLocation, setMyLocation] = useState(null);
    const [isSharing, setIsSharing] = useState(false);
    const [activeCount, setActiveCount] = useState(0);
    const intervalRef = useRef(null);
    const wsRef = useRef(null);
    const markersRef = useRef({});
    const infoWindowsRef = useRef({});
    // 지도 초기화 및 사용자 위치 가져오기
    useEffect(() => {
        const initMap = () => {
            if (!mapRef.current) return;

            // 기본 위치로 지도 초기화 (서울 시청)
            const kakaoMap = new window.kakao.maps.Map(mapRef.current, {
                center: new window.kakao.maps.LatLng(37.5665, 126.9780),
                level: 3
            });

            setMap(kakaoMap);
            
            // 사용자의 현재 위치 가져오기
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(
                    (position) => {
                        const { latitude, longitude } = position.coords;
                        setMyLocation({ latitude, longitude });
                        
                        // 지도 중심을 사용자 위치로 이동
                        const userPosition = new window.kakao.maps.LatLng(latitude, longitude);
                        kakaoMap.setCenter(userPosition);
                    },
                    (error) => {
                        console.log('위치 가져오기 실패, 기본 위치 사용:', error);
                    },
                    {
                        enableHighAccuracy: true,
                        timeout: 5000,
                        maximumAge: 0
                    }
                );
            }
        };

        // Kakao Maps API가 로드될 때까지 대기
        if (window.kakao && window.kakao.maps) {
            initMap();
        } else {
            // 스크립트가 로드되지 않았다면 로드 대기
            const checkKakao = setInterval(() => {
                if (window.kakao && window.kakao.maps) {
                    clearInterval(checkKakao);
                    initMap();
                }
            }, 300);

            return () => clearInterval(checkKakao);
        }
    }, []);

    // 지도 relayout (지도가 제대로 표시되도록)
    useEffect(() => {
        if (map) {
            // 여러 타이밍에 relayout 호출하여 확실하게 표시
            const timers = [];
            
            // 사용자 위치 또는 기본 위치 결정
            const centerPosition = myLocation 
                ? new window.kakao.maps.LatLng(myLocation.latitude, myLocation.longitude)
                : new window.kakao.maps.LatLng(37.5665, 126.9780);
            
            // 즉시 실행
            map.relayout();
            map.setCenter(centerPosition);
            
            // 다양한 시간 간격으로 relayout 호출
            [50, 100, 200, 300, 500].forEach(delay => {
                timers.push(setTimeout(() => {
                    map.relayout();
                    map.setCenter(centerPosition);
                }, delay));
            });
            
            return () => {
                timers.forEach(timer => clearTimeout(timer));
            };
        }
    }, [map, isSharing, myLocation]); // myLocation도 의존성에 추가
    
    // 컨테이너 크기 변화 감지 및 relayout
    useEffect(() => {
        if (!map || !mapRef.current) return;
        
        const resizeObserver = new ResizeObserver(() => {
            map.relayout();
        });
        
        resizeObserver.observe(mapRef.current);
        
        return () => {
            resizeObserver.disconnect();
        };
    }, [map]);

    // WebSocket 연결 (Meet 위치공유 전용: /api/location)
    const connectWebSocket = () => {
        const wsUrl = `${protocol}//${hostname}${port}/api/location?meetId=${meetId}`;
        console.log('WebSocket 연결 시도 (위치공유):', wsUrl);
        const ws = new WebSocket(wsUrl);
        
        ws.onopen = () => {
            console.log('WebSocket 연결됨 - Meet 위치공유', meetId);
            console.log('ws.readyState:', ws.readyState);
        };
        
        ws.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                // 위치 메시지만 처리
                if (data.type === 'location') {
                    updateMarker(data);
                }
            } catch (err) {
                console.error('메시지 파싱 실패:', err);
            }
        };
        
        ws.onerror = (error) => {
            console.error('WebSocket 오류:', error);
        };
        
        ws.onclose = () => {
            console.log('WebSocket 연결 종료');
        };
        
        wsRef.current = ws;
    };

    // 프로필 이미지를 원형 마커로 변환 (Canvas 사용)
    const createCircularMarker = (profileImage, isMyLocation = false) => {
        return new Promise((resolve) => {
            const canvas = document.createElement('canvas');
            const size = isMyLocation ? 60 : 50; // 내 위치는 더 크게
            canvas.width = size;
            canvas.height = size;
            const ctx = canvas.getContext('2d');

            const img = new Image();
            img.crossOrigin = 'anonymous';
            
            img.onload = () => {
                // 원형 클리핑
                ctx.beginPath();
                ctx.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2);
                ctx.closePath();
                ctx.clip();
                
                // 프로필 이미지 그리기
                ctx.drawImage(img, 0, 0, size, size);
                
                // 내 위치면 테두리 추가
                if (isMyLocation) {
                    ctx.restore();
                    ctx.beginPath();
                    ctx.arc(size / 2, size / 2, size / 2 - 2, 0, Math.PI * 2);
                    ctx.strokeStyle = '#007bff';
                    ctx.lineWidth = 4;
                    ctx.stroke();
                }
                
                resolve(canvas.toDataURL());
            };
            
            img.onerror = () => {
                // 이미지 로드 실패 시 기본 원형 아바타
                ctx.fillStyle = '#e0e0e0';
                ctx.beginPath();
                ctx.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2);
                ctx.fill();
                
                // 사람 아이콘 그리기
                ctx.fillStyle = '#999';
                ctx.beginPath();
                ctx.arc(size / 2, size / 3, size / 6, 0, Math.PI * 2);
                ctx.fill();
                
                ctx.beginPath();
                ctx.arc(size / 2, size * 0.75, size / 3, 0, Math.PI * 2);
                ctx.fill();
                
                resolve(canvas.toDataURL());
            };
            
            // 이미지 소스 설정
            if (profileImage) {
                img.src = profileImage.startsWith('data:') 
                    ? profileImage 
                    : `data:image/png;base64,${profileImage}`;
            } else {
                img.onerror();
            }
        });
    };

    // 마커 업데이트
    const updateMarker = async (data) => {
        if (!map) return;

        const { userId, userName, latitude, longitude, profileImage } = data;
        const position = new window.kakao.maps.LatLng(latitude, longitude);

        // 기존 마커 제거
        if (markersRef.current[userId]) markersRef.current[userId].setMap(null);
        if (infoWindowsRef.current[userId]) infoWindowsRef.current[userId].close();
        
        // 프로필 이미지 결정
        const isMyLocation = userId === auth.id;
        const displayProfileImage = isMyLocation ? auth.profileImage : profileImage;
        const markerSize = isMyLocation ? 60 : 50;
        
        // 원형 마커 이미지 생성
        const circularImageUrl = await createCircularMarker(displayProfileImage, isMyLocation);

        const markerImage = new window.kakao.maps.MarkerImage(
            circularImageUrl,
            new window.kakao.maps.Size(markerSize, markerSize),
            { offset: new window.kakao.maps.Point(markerSize / 2, markerSize / 2) }
        );

        const marker = new window.kakao.maps.Marker({
            position,
            map,
            title: userName,
            image: markerImage
        });

        // 정보창
        const infowindow = new window.kakao.maps.InfoWindow({
            content: `<div style="padding:5px 10px; font-size:12px;">${userName}${userId === auth.id ? ' (나)' : ''}</div>`
        });

        window.kakao.maps.event.addListener(marker, 'click', () => infowindow.open(map, marker));

        if (userId === auth.id) {
            infowindow.open(map, marker);
            map.setCenter(position);
        }
        markersRef.current[userId] = marker;
        infoWindowsRef.current[userId] = infowindow;

        setMarkers(prev => {
            const updated = { ...prev, [userId]: marker };
            setActiveCount(Object.keys(updated).length);
            return updated;
        });
    };

    // 내 위치 가져오기 및 전송
    const getMyLocation = () => {
        if (!navigator.geolocation) {
            alert('위치 정보를 사용할 수 없습니다.');
            return;
        }

        navigator.geolocation.getCurrentPosition(
            (position) => {
                const { latitude, longitude } = position.coords;
                setMyLocation({ latitude, longitude });
                updateMarker({
                    userId: auth.id,
                    userName: auth.name || '나',
                    latitude,
                    longitude,
                    profileImage: auth.profileImage
                });
                // WebSocket으로 위치 전송
                if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
                    const locationData = {
                        type: 'location',
                        userId: auth.id,
                        userName: auth.name,
                        latitude,
                        longitude,
                        profileImage: auth.profileImage
                    };
                    wsRef.current.send(JSON.stringify(locationData));
                }
            },
            (error) => {
                console.error('위치 가져오기 실패:', error);
            },
            {
                enableHighAccuracy: true,
                timeout: 5000,
                maximumAge: 0
            }
        );
    };

    // 활성 참가자 수 계산
    useEffect(() => {
        setActiveCount(Object.keys(markers).length);
    }, [markers]);

    // 위치 공유 시작
    const startSharing = () => {
        setIsSharing(true);
        
        // WebSocket 연결
        connectWebSocket();
        
        // 초기 위치 전송
        setTimeout(() => getMyLocation(), 1000);

        // 5초마다 위치 업데이트
        intervalRef.current = setInterval(() => {
            getMyLocation();
        }, 5000);
    };

    // 위치 공유 중단
    const stopSharing = () => {
        setIsSharing(false);
        
        if (intervalRef.current) clearInterval(intervalRef.current);

        // WebSocket 연결 종료
        if (wsRef.current) {
            wsRef.current.close();
            wsRef.current = null;
        }

        // 마커 제거
        Object.values(markersRef.current).forEach(marker => marker.setMap(null));
        Object.values(infoWindowsRef.current).forEach(info => info.close());
        markersRef.current = {};
        infoWindowsRef.current = {};
        setActiveCount(0);
    };

    // 컴포넌트 언마운트 시에만 위치 공유 중단
    useEffect(() => {
        return () => {
            // 컴포넌트가 완전히 언마운트될 때만 실행
            if (wsRef.current) {
                wsRef.current.close();
            }
            if (intervalRef.current) {
                clearInterval(intervalRef.current);
            }
        };
    }, []); // 빈 의존성 배열로 마운트/언마운트 시에만 실행

    return (
        <div className="location-map-container">
            <div className="d-flex justify-content-between align-items-center p-3 border-bottom">
                <div>
                    <h6 className="mb-1">실시간 위치 공유</h6>
                    {isSharing && (
                        <small className="text-muted">
                            <i className="fas fa-users me-1"></i>
                            현재 {activeCount}명이 위치를 공유 중입니다
                        </small>
                    )}
                </div>
                <button
                    className={`btn btn-sm ${isSharing ? 'btn-danger' : 'btn-primary'}`}
                    onClick={isSharing ? stopSharing : startSharing}
                >
                    <i className={`fas ${isSharing ? 'fa-stop' : 'fa-play'} me-1`}></i>
                    {isSharing ? '공유 중단' : '위치 공유 시작'}
                </button>
            </div>
            <div
                ref={mapRef}
                style={{ width: '100%', height: '400px' }}
            />
            {isSharing && (
                <div className="p-2 bg-light text-center border-top">
                    <small className="text-muted">
                        <i className="fas fa-circle text-success me-2" style={{ fontSize: '8px' }}></i>
                        실시간 위치 공유 중 · 5초마다 자동 업데이트
                    </small>
                </div>
            )}
            {!isSharing && (
                <div className="p-3 bg-light text-center">
                    <p className="mb-2 text-muted small">
                        <i className="fas fa-info-circle me-1"></i>
                        모임 참가자들과 실시간으로 위치를 공유할 수 있습니다
                    </p>
                    <p className="mb-0 text-muted small">
                        위치 공유를 시작하려면 위 버튼을 눌러주세요
                    </p>
                </div>
            )}
        </div>
    );
}

