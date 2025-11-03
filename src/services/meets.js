import axios from "/config/axios";
//글
export async function createMeet(post){
    const { data } = await axios.post(`/api/meets`,post)
    return data;
}

export async function getMeetAllWithPage(page){
    const { data } = await axios.get(`/api/meets?page=${page}`)
    return data;
}
export async function getMeetAll(){
    const { data } = await axios.get(`/api/meets/all`)
    return data;
}

export async function getMeet(id) {
    const { data } = await axios.get(`/api/meets/${id}`)
    return data;
}

export async function getMeetsForMain(limit = 5){
    const { data } = await axios.get(`/api/meets/top?limit=${limit}`)
    return data;
}

export async function deleteMeet(id){
    const { data } = await axios.delete(`/api/meets/${id}`)
    return data;
}

export async function updateMeet(post) {
    const { data } = await axios.put(`/api/meets/${post.id}`,post)
    return data;
}
//댓글
export async function getMeetComment(id){
    const { data } = await axios.get(`/api/meets/${id}/comments`)
    return data;
}

export async function createMeetComment(id,comment){
    const { data } = await axios.post(`/api/meets/${id}/comments`,comment)
    return data;
}

export async function deleteMeetComment(commentId){
    const { data } = await axios.delete(`/api/meets/comments/${commentId}`)
    return data;
}
//지원
export async function createApply(id,apply) {
    const { data } = await axios.post(`/api/meets/${id}/apply`,apply)
    return data;
}
export async function getMeetApplies(id) {
    const { data } = await axios.get(`/api/meets/${id}/apply`)
    return data;
}
export async function acceptApply(id,userId) { 
    const { data } = await axios.patch(`/api/meets/${id}/apply/accept`,{ id:userId })
    return data;
}
export async function rejectApply(id,userId) {
    const { data } = await axios.patch(`/api/meets/${id}/apply/reject`,{ id:userId })
    return data;
}
export async function cancleApplyMeet(id,userId){
    const { data } = await axios.delete(`/api/meets/${id}/apply/cancle?userId=${userId}`)
    return data;
}
export async function closeApplyMeet(id) {
    const { data } = await axios.patch(`/api/meets/${id}/apply/close`)
    return data;
}

