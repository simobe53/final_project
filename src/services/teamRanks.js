import axiosInstance from "/config/axios";

// 특정 연도의 팀 순위 조회
export async function getTeamRanks(year = "2025") {
    try {
        const response = await axiosInstance.get(`/api/team-ranks/${year}`);
        return response.data;
    } catch (error) {
        console.error("팀 순위 조회 실패:", error);
        throw error;
    }
}

