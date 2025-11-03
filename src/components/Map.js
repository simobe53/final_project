var geocoder = geocoder;
var markers = [];

if (!geocoder) {
    try {
        geocoder = new kakao.maps.services.Geocoder()
    } catch(e) {
        console.log(e);
    };
}


export function getKakaoMap(map, {lat, lng}) {
    return new kakao.maps.Map(map, { 
        center: new kakao.maps.LatLng(lat, lng)
    });
}

export function setMarker(map, { lat, lng, onclick }) {
    var marker = new kakao.maps.Marker({
        position: new kakao.maps.LatLng(lat, lng)
    });
    marker.setMap(map);
    if (onclick) {
        kakao.maps.event.addListener(marker, 'click', onclick);
    }
    markers.push(marker);
    return marker;
}

export function removeMarkers() {
    for ( var i = 0; i < markers.length; i++ ) {
        markers[i].setMap(null);
    }   
    markers = [];
}

export function getLatLngFromAddress(address, success) {
    geocoder.addressSearch(address, function(result, status) {
    // 정상적으로 검색이 완료됐으면 
        if (status === kakao.maps.services.Status.OK) {
            const lat = result[0].y;
            const lng = result[0].x;

            success({ lat, lng });
        } 
        return null;
    });    
}


// 주소로 좌표 중심 지도 그리기
export function getMapfromAddress(mapRef, address, success) {
    return geocoder.addressSearch(address, function(result, status) {

    // 정상적으로 검색이 완료됐으면 
        if (status === kakao.maps.services.Status.OK) {
            const lat = result[0].y;
            const lng = result[0].x;

            const map = getKakaoMap(mapRef, {lat, lng});
            success(map);
        } 
        return null;
    });    
}

// 주소로 지도에 마커 찍기
export function setMarkerfromAddress(map, address, onclick) {
    return geocoder.addressSearch(address, function(result, status) {

    // 정상적으로 검색이 완료됐으면 
        if (status === kakao.maps.services.Status.OK) {
            const lat = result[0].y;
            const lng = result[0].x;

            setMarker(map, { lat, lng, onclick })
        } 
        return null;
    });    
}


export function getMyAddressNow(success) {
    if (!navigator.geolocation) return;
    navigator.geolocation.getCurrentPosition(({ coords: { latitude: lat, longitude: lng} }) => {
        // 좌표로 행정동 주소 정보를 가져옴
        geocoder.coord2RegionCode(lng, lat, (res) => {
            const { address_name: address } = res[0];
            success({ address, lat, lng });
        });
    }, err => console.log(err))
}


export function getLocationFromAddress(address, success) {
    if (!navigator.geolocation) return;
    getLatLngFromAddress(address, ({ lat, lng }) => {
        // 좌표로 행정동 주소 정보를 가져옴
        geocoder.coord2RegionCode(lng, lat, (res) => {
            const { address_name: location } = res[0];
            success(location);
        });
    })
}
