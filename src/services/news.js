import axios from '/config/axios';
import { formatDate } from "/components";

/**
 * 내 응원 팀의 뉴스 조회
 * @param {number} limit - 조회할 뉴스 개수
 * @param {number} offset - 건너뛸 뉴스 개수
 * @returns {Promise<Array>} 뉴스 리스트
 */
export const getMyTeamNews = async (limit = 20, offset = 0) => {
    const response = await axios.get(`/api/news/my-team?limit=${limit}&offset=${offset}`);
    return response.data;
};

/**
 * 내 응원 팀의 하이라이트 조회
 * @param {number} limit - 조회할 하이라이트 개수
 * @returns {Promise<Array>} 일정순으로 최신 하이라이트 리스트
 */
export const getMyTeamHighlights = async (limit = 5) => {
    const response = await axios.get(`/api/schedules/my-team?limit=${limit}&highlightOnly=true`);
    return response.data;
};

/**
 * 특정 팀의 뉴스 조회
 * @param {string} teamId - 팀 ID (예: LG, HH, SK)
 * @param {number} limit - 조회할 뉴스 개수
 * @returns {Promise<Array>} 뉴스 리스트
 */
export const getNewsByTeam = async (teamId, limit = 5) => {
    const response = await axios.get(`/api/news/team/${teamId}?limit=${limit}`);
    return response.data;
};

/**
 * 전체 최신 뉴스 조회
 * @param {number} limit - 조회할 뉴스 개수
 * @param {number} offset - 건너뛸 뉴스 개수
 * @returns {Promise<Array>} 뉴스 리스트
 */
export const getLatestNews = async (limit = 20, offset = 0) => {
    const response = await axios.get(`/api/news/latest?limit=${limit}&offset=${offset}`);
    return response.data;
};

/**
 * 뉴스 상세 조회
 * @param {number} id - 조회할 뉴스 아이디
 * @returns {Promise<Object>} 뉴스 상세
 */
export const getNewsById = async (id) => {
    const response = await axios.get(`/api/news/${id}`);
    return response.data;
};

/**
 * 날짜별 경기 일정(하이라이트) 조회
 * @param {string} date - 조회할 날짜 (format: yyyy-MM-dd)
 * @returns {Promise<Array>} 경기 일정(하이라이트 포함) 리스트
 */
export const getSchedules = async (date = '') => {
    const isToday = date === formatDate(new Date());
    const response = await axios.get(isToday ? '/api/schedules/today' : `/api/schedules?date=${date}`);
    return response.data;
};


/**
 * 날짜별 경기 일정(하이라이트) 조회
 * @param {string} date - 조회할 날짜 (format: yyyy-MM-dd)
 * @returns {Promise<Array>} 경기 일정(하이라이트 포함) 리스트
 */
export const getHighlights = async (date) => {
    const { data } = await axios.get(`/api/schedules?date=${date}`);
    
    // 하이라이트 있는 일정만 출력하도록
    return data.filter(({ highlightUrl }) => !!highlightUrl);
};


/**
 * 경기 일정(하이라이트) 상세 조회
 * @param {number} id - 경기 일정 아이디
 * @returns {Promise<Object>} 경기 일정(하이라이트 포함) 리스트
 */
export const getScheduleById = async (id, { isHighlight = false } = {}) => {
    const { data } = await axios.get(`/api/schedules/${id}`);
    // 하이라이트 있는 일정만 출력하도록
    if (isHighlight) return data.highlightUrl ? data : {};
    return data;
};

/**
 * 하이라이트 영상 AI 요약 생성
 * @param {number} id - 경기 일정 아이디
 * @returns {Promise<Object>} { success: boolean, summary: string, error: string }
 */
export const generateHighlightSummary = async (id) => {
    try {
        const { data } = await axios.post(`/api/schedules/${id}/generate-summary`);
        return data;
    } catch (error) {
        // 에러 응답 처리
        if (error.response && error.response.data) {
            return error.response.data;
        }
        return {
            success: false,
            error: '서버와의 통신 중 오류가 발생했습니다.'
        };
    }
};

/**
 * 뉴스 AI 요약 생성
 * @param {Object} newsData - 뉴스 데이터 { title, content, team_name }
 * @returns {Promise<Object>} { success: boolean, summary: string, error: string }
 */
export const generateNewsSummary = async (newsData) => {
    try {
        const { data } = await axios.post('/api/news/summarize', { news: newsData });
        return data;
    } catch (error) {
        // 에러 응답 처리
        if (error.response && error.response.data) {
            return error.response.data;
        }
        return {
            success: false,
            error: '서버와의 통신 중 오류가 발생했습니다.'
        };
    }
};

/**
 * 여러 뉴스 AI 종합 요약 생성
 * @param {Array} newsList - 뉴스 리스트 [{ title, content, team_name }, ...]
 * @returns {Promise<Object>} { success: boolean, summary: string, error: string }
 */
export const generateMultipleNewsSummary = async (newsList) => {
    try {
        const { data } = await axios.post('/api/news/summarize-multiple', { news_list: newsList });
        return data;
    } catch (error) {
        // 에러 응답 처리
        if (error.response && error.response.data) {
            return error.response.data;
        }
        return {
            success: false,
            error: '서버와의 통신 중 오류가 발생했습니다.'
        };
    }
};

/**
 * 뉴스 ID로 AI 요약 생성
 * @param {number} newsId - 뉴스 아이디
 * @returns {Promise<Object>} { success: boolean, summary: string, error: string }
 */
export const generateNewsSummaryById = async (newsId) => {
    try {
        const { data } = await axios.post(`/api/news/${newsId}/summarize`);
        return data;
    } catch (error) {
        // 에러 응답 처리
        if (error.response && error.response.data) {
            return error.response.data;
        }
        return {
            success: false,
            error: '서버와의 통신 중 오류가 발생했습니다.'
        };
    }
};


