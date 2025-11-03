import axios from "/config/axios";

export async function getTeams() {
    const { data } = await axios.get('/api/teams')
    return data;
}

export async function getPlayers() {
    const { data } = await axios.get('/api/players')
    return data;
}