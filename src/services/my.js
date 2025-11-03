import axios from "/config/axios";

/* TODO:: 백엔드와 연결 필요 :: URL 편하신대로 변경해도 괜찮음. 페이지네이션으로 던져줘도 괜찮음 */

export async function getMyPlaces(userId){      // 유저가 작성한 플레이스 전체 조회
    const { data } = await axios.get(`/api/places?userId=${userId}`)
    return data;
}

export async function getMyScrapPlaces(){     // 유저가 스크랩한 플레이스 전체 조회
    const { data } = await axios.get(`/api/scrap/myPage`)
    return data;
}

export async function getMyPlaceReviews(){     // 유저가 작성한 플레이스 리뷰 전체 조회
    const { data } = await axios.get(`/api/ranks/myPage`)
    return data;
}

export async function getMyFeeds(userId){       // 유저가 작성한 피드 전체 조회
    const { data } = await axios.get(`/api/feeds?userId=${userId}`)
    return data.feeds;
}

// AI 유니폼 생성 관련 API (Spring Boot 프록시 사용)
export async function generateImage(imageData) {     // 단일 이미지 생성
    const { data } = await axios.post('/api/ai/generate-image', imageData);
    return data;
}

export async function saveAiUniformImage(uniformData) {     // AI 유니폼 이미지 DB 저장
    const { data } = await axios.post('/api/ai-uniform', uniformData);
    return data;
}

export async function translatePrompt(promptData) {     // 프롬프트 번역
    const { data } = await axios.post('/api/ai/translate-prompt', promptData);
    return data;
}

export async function getMyImages(userId = 1) {     // 저장된 이미지 목록 조회 (DB에서만)
    const { data } = await axios.get(`/api/ai-uniform/user/${userId}`);
    return data;
}