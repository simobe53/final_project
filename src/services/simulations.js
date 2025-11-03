import axios from "/config/axios";

export async function startSimulation(simulationData) {
    const { data } = await axios.post('/api/simulations/start', simulationData);
    return data;
}

export async function getSimulations(date) {
    const { data } = await axios.get(`/api/simulations?showAt=${date}`);
    return data;
}

export async function getSimulation(id) {
    const { data } = await axios.get(`/api/simulations/${id}`);
    return data;
}

export async function createSimulation(simulationData) {
    const { data } = await axios.post('/api/simulations', simulationData);
    return data;
}

export async function updateSimulation(id, simulationData) {
    const { data } = await axios.put(`/api/simulations/${id}`, simulationData);
    return data;
}

export async function deleteSimulation(id) {
    const { data } = await axios.delete(`/api/simulations/${id}`);
    return data;
}

// 특정 시뮬레이션의 타석당 결과 조회
export async function getSimulationAtBats(simulationId) {
    try {
        const response = await axios.get(`/api/simulations/${simulationId}/at-bats`);
        return response.data;
    } catch (error) {
        console.error('타석 데이터 조회 실패:', error);
        throw error;
    }
}

// 🎮 실시간 시뮬레이션 게임 시작
export async function startRealtimeGame(simulationId) {
    try {
        const response = await axios.post(`/api/simulations/${simulationId}/start-game`);
        return response.data;
    } catch (error) {
        console.error('실시간 게임 시작 실패:', error);
        throw error;
    }
}

// 🏃 다음 타석 결과 요청 (실시간) - 백그라운드 스케줄러가 자동 진행하므로 불필요
// export async function getNextAtBat(simulationId) {
//     try {
//         const response = await axios.post(`/api/simulations/${simulationId}/next-at-bat`);
//         return response.data;
//     } catch (error) {
//         console.error('다음 타석 요청 실패:', error);
//         throw error;
//     }
// }

// 📊 현재 게임 상태 조회
export async function getGameState(simulationId) {
    try {
        const response = await axios.get(`/api/simulations/${simulationId}/game-state`);
        return response.data;
    } catch (error) {
        console.error('게임 상태 조회 실패:', error);
        throw error;
    }
}
// 사용자 시뮬레이션 요청 생성 (일반 사용자)
export async function createSimulationRequest(requestData) {
    const { data } = await axios.post('/api/user-simul-requests', requestData);
    return data;
}

// 사용자 시뮬레이션 요청 목록 조회
export async function getUserSimulationRequests(params = {}) {
    const { data } = await axios.get('/api/user-simul-requests', { params });
    return data;
}

// 사용자 시뮬레이션 요청 상세 조회
export async function getUserSimulationRequest(id) {
    const { data } = await axios.get(`/api/user-simul-requests/${id}`);
    return data;
}

// 사용자 시뮬레이션 요청 승인 (관리자)
export async function approveUserSimulationRequest(id, adminComment = '', scheduledAt = null) {
    const { data } = await axios.put(`/api/user-simul-requests/${id}/approve`, {
        adminComment,
        scheduledAt
    });
    return data;
}

// 사용자 시뮬레이션 요청 거절 (관리자)
export async function rejectUserSimulationRequest(id, adminComment = '') {
    const { data } = await axios.put(`/api/user-simul-requests/${id}/reject`, { adminComment });
    return data;
}

// 대기 중인 사용자 요청 개수 조회 (관리자 알림용)
export async function getPendingUserRequestCount() {
    const { data } = await axios.get('/api/user-simul-requests/pending-count');
    return data;
}