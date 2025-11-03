from chatbot.chatbot_filter import first_filter, mid_filter
from chatbot.chatbot_rule_answer import rule_answer
from chatbot.chatbot_player_answer import player_answer
from chatbot.chatbot_team_answer import team_answer
from chatbot.chatbot_restaurant_answer import restaurant_answer
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.output_parsers import StrOutputParser

# 질문 유형 매핑
type_map = {
    "rules": "야구 규칙",
    "playerInfo": "선수 정보",
    "analysis": "팀 분석",
    "restaurant_rec": "구장 주변 맛집 추천",
}

# 답변 체인 매핑
answer_chains = {
    "야구 규칙": rule_answer,
    "선수 정보": player_answer,
    "팀 분석": team_answer,
    "구장 주변 맛집 추천": restaurant_answer,
}

# 필터 초기화
chat_first_filter = first_filter()
chat_mid_filter = mid_filter()

# ======================
# 자연스러운 재질문 (fallback)
# ======================
fallback_llm = ChatOpenAI(model="gpt-4.1-mini", temperature=0.7)

fallback_prompt = ChatPromptTemplate.from_template("""
너는 사용자의 질문 의도를 부드럽게 파악해서,
야구와 관련된 자연스러운 후속 질문을 던지는 AI야.

조건은 아래와 같아:
1. 문체는 자연스럽고 따뜻해야 해. (기계처럼 들리면 안 돼.)
2. 질문을 거절하거나 명령조로 쓰지 마.
3. 사용자가 뭘 알고 싶은지 추측해서 대화를 이어가는 느낌으로 써.
4. 문장은 1~2문장, 너무 길지 않게.
5. ‘~하신 건가요?’, ‘~궁금하신가요?’, ‘혹시 ~말씀하시는 걸까요?’ 같은 말투 사용.

예시:
- “혹시 특정 팀이나 선수를 말씀하신 걸까요?”
- “야구 관련해서 궁금하신 부분이 있을까요?”
- “조금만 더 구체적으로 알려주시면 바로 도와드릴게요!”
- “혹시 경기 규칙 쪽을 물어보신 걸까요?”

사용자 입력: {user_question}

이 입력에 어울리는 자연스러운 후속 질문을 1~2문장으로 만들어줘.
""")

fallback_chain = fallback_prompt | fallback_llm | StrOutputParser()


# ======================
# 메인 챗봇 프로세스
# ======================
async def chatbot_process(user_input: str) -> str:
    """
    실제 챗봇 처리 로직 (1차 필터 + 라우팅 + 자연스러운 fallback)
    """

    # 1️⃣ 1차 필터: 야구 관련 여부
    first_result = await chat_first_filter.ainvoke({"user_question": user_input})
    if not first_result.startswith("OK"):
        # NO일 경우 → 자연스럽게 fallback 유도
        gentle_reply = await fallback_chain.ainvoke({"user_question": user_input})
        return gentle_reply.strip()

    # 2️⃣ 라우팅 필터: 질문 유형 판별
    question_type = await chat_mid_filter.ainvoke({"user_question": user_input})

    # 3️⃣ 질문이 모호하거나 라우팅 불가 시
    if not question_type or question_type == "error":
        gentle_reply = await fallback_chain.ainvoke({"user_question": user_input})
        return gentle_reply.strip()

    # 4️⃣ 해당 유형에 맞는 답변 체인 실행
    question_category = type_map.get(question_type)
    if not question_category or question_category not in answer_chains:
        gentle_reply = await fallback_chain.ainvoke({"user_question": user_input})
        return gentle_reply.strip()

    # 5️⃣ 실제 답변 생성
    answer_chain = answer_chains[question_category]()
    final_answer = await answer_chain.ainvoke({"user_question": user_input})
    return final_answer.strip().replace("\n", "<br>")
