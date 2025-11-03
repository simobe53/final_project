import os
import json
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough

from chatbot.chatbot_tools import get_player_data

load_dotenv()
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")

def player_answer():
    llm = ChatOpenAI(model="gpt-4.1-mini", temperature=0)

    # 1. 질문 재구성 프롬프트
    rephrase_prompt = ChatPromptTemplate.from_template(
    """너는 KBO 선수 정보 질문을 전문으로 처리하는 AI야.
    질문이 불명확하거나 짧으면 이해하기 쉽게 재작성해줘.
    예를 들어 "선수" 요즘 어때? 라고 하면 이번 시즌 성적을
    보고 어떤지 파악하는 질문으로 재구성해줘.
    질문: {user_question}
    재구성된 질문:""")
    rephrase_chain = rephrase_prompt | llm | StrOutputParser()

    # 2. 선수 이름 추출 프롬프트
    extract_name_prompt = ChatPromptTemplate.from_template("""다음 질문에서 KBO 선수 이름만 정확히 뽑아줘: "{rephrased_question}" """)
    extract_name_chain = extract_name_prompt | llm | StrOutputParser()

    # 3. 최종 답변 생성 프롬프트
    player_data_prompt = ChatPromptTemplate.from_template(
    """
    너는 KBO 프로야구 선수 데이터를 분석하고 설명하는 전문가야.
    [선수 데이터]를 바탕으로 사용자의 [질문]에 자연스럽고 알기 쉽게 대답해줘.

    [규칙]
    1. [선수 데이터]의 수치를 직접 나열하지 말고, 그 수치가 의미하는 선수의 성향을 설명해.
    2. 공격형, 수비형, 꾸준함, 장타력 등 구체적인 특징을 중심으로 말해.
    3. 2~3줄 이내로 친절하고 부드럽게 표현해.
    4. 불명확한 질문이라도, 선수의 전반적인 특징으로 유추해서 답해.

    ---
    [선수 데이터]:
    {player_data}

    [사용자 질문]:
    {user_question}

    [전문가 요약 답변]:
    """)

    # 체인 연결
    final_chain = (
        RunnablePassthrough.assign(rephrased_question=rephrase_chain)
        | RunnablePassthrough.assign(player_name=extract_name_chain)
        | RunnablePassthrough.assign(player_data=lambda x: json.dumps(get_player_data(x["player_name"]), ensure_ascii=False, indent=2))
        | {"rephrased_question": lambda x: x["rephrased_question"], "player_data": lambda x: x["player_data"], "user_question": lambda x: x["user_question"]}
        | player_data_prompt | llm | StrOutputParser())
    
    return final_chain