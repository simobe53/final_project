import axios from "/config/axios";

export async function getArticlesBySimulation(simulationId) {
  try {
    const res = await axios.get(`/api/articles?simulation_id=${simulationId}`);
    return res.data.map((a) => {
      const [titleLine, ...rest] = a.content.split("\n");
      const contentBody = rest.join("\n");
      const summary = contentBody.split("--- 경기 요약 ---")[1]?.trim() || "";

      return {
        teamName: a.teamName,
        title: titleLine || `기사 ${a.id}`,
        content: contentBody.replace(/--- 경기 요약 ---.*/, "").trim(),
        summary: summary,
      };
    });
  } catch (error) {
    console.error("getArticlesBySimulation 오류:", error);
    return [];
  }
}

export default {};
