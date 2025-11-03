import axios from 'axios';

// axios 기본 설정
axios.defaults.withCredentials = true; // 모든 요청에 쿠키 포함

export default axios;
