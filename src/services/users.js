import axios from "/config/axios";

export async function getUserByAccount(account) {
    const { data } = await axios.get(`/api/users/${account}`);
    return data;
}

export async function createUser(user) {
    const { data } = await axios.post(`/api/users`, user);
    return data;
}

export async function updateUser(user) {
    const { data } = await axios.put(`/api/users/${user.account}`, user);
    return data;
}

export async function deleteUser(account) {
    const { data } = await axios.delete(`/api/users/${account}`);
    return data;
}

export async function verifyUser(account, password) {
    const { data } = await axios.post('/api/auth/verify-password', { account, password });
    return data;
}

export async function changePassword({ id, password, orgPassword }) {
    const { data } = await axios.patch(`/api/users/${id}/password`, { password, orgPassword });
    return data;
}

export async function sendEmail(email) {
    const { data } = await axios.post('/api/users/mailSend', new URLSearchParams({ email }));
    return data;
}

export async function mailCheck(code) {
    const { data: isMatch } = await axios.get(`/api/users/mailCheck?checkNumber=${encodeURIComponent(code)}`);
    return isMatch;
}

export default {};