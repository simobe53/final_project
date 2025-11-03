import axios from "/config/axios";

export async function createPlace(place){
    const { data } = await axios.post(`/api/places`, place)
    return data;
}

export async function getPlaces(teamId){
    const { data } = await axios.get(teamId ? `/api/places?teamId=${teamId}` : `/api/places`)
    return data;
}

export async function getPlaceById(id){
    const { data } = await axios.get(`/api/places/${id}`)
    return data;
}

export async function getPlacesForMain(limit = 4){
    const { data } = await axios.get(`/api/places/top?limit=${limit}`)
    return data;
}

export async function deletePlaceById(id){
    const { data } = await axios.delete(`/api/places/${id}`)
    return data;
}

export async function toggleScrap(placeId, userId){
    const { data } = await axios.post(`/api/places/${placeId}/scrap?userId=${userId}`)
    return data;
}

export async function getRanks(placeId){
    const { data } = await axios.get(`/api/places/${placeId}/ranks`)
    return data;
}

export async function createRank(placeId, rank){
    const { data } = await axios.post(`/api/places/${placeId}/ranks`, rank)
    return data;
}

export async function deleteRank(rankId, userId){
    await axios.delete(`/api/places/comments/${rankId}?userId=${userId}`)
}

export async function getAverageRank(placeId) {
    const { data } = await axios.get(`/api/ranks/average?placeId=${placeId}`)
    return data;
}

export async function summarizeReviews(reviews) {
    try {
        const { data } = await axios.post(`/api/places/reviews/summarize`, {
            reviews: reviews
        });
        return data;
    } catch (error) {
        console.error('리뷰 요약 API 호출 실패:', error);
        throw error;
    }
}