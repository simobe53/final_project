import axios from "/config/axios";

export async function getDiary(date) {
    const { data } = await axios.get(`/api/diary?date=${date}`);
    return data;
}

export async function createDiary(diary) {
    const { data } = await axios.post(`/api/diary/create`, diary);
    return data;
}

export async function deleteDiary(id) {
    const { data } = await axios.delete(`/api/diary/${id}`);
    return data;
}

export async function verifyTicket(payload) {
    const { data } = await axios.post('/api/diary/verify-ticket', payload);
    return data;
}

export default {};