import os
from dotenv import load_dotenv
from langchain_chroma import Chroma
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough

load_dotenv()

# 불변(immutable) 규칙 DB는 모듈 로드시 1회 로드
embeddings = OpenAIEmbeddings(model="text-embedding-3-small")

current_dir = os.path.dirname(os.path.abspath(__file__))
db_path = os.path.join(current_dir, "rules_db_v2025")  # 최신 규칙 기반

vector_store = Chroma(
    persist_directory=db_path,
    embedding_function=embeddings
)

retriever = vector_store.as_retriever(search_kwargs={"k": 5})

# LLM도 1회 로드
llm = ChatOpenAI(model="gpt-4.1-mini", temperature=0)


def rule_answer():
    """
    KBO 최신 규칙(RAG) 기반 답변 엔진
    - 규칙 근거 기반
    - 추측 금지
    - 팬 친화 표현 (조항번호 노출 X)
    """

    # 🔹 규칙 질문을 이해하기 쉽게 다듬는 단계
    rephrase_prompt = ChatPromptTemplate.from_template(
    """
    너는 KBO 야구 규칙 전문가야.
    사용자의 질문을 규칙에서 사용하는 정확한 용어로,
    의미를 바꾸지 않고 자연스럽게 재작성해줘.
    ▪ 주체(투수/타자/주자)가 모호하면 명확히
    ▪ 한 문장으로 간결하게

    질문: {user_question}
    재구성된 질문:
    """
    )
    
    rephrase_chain = rephrase_prompt | llm | StrOutputParser()

    # 규칙 기반 정확 응답 (조항번호, 전문 표현 드러내지 않기)
    response_prompt = ChatPromptTemplate.from_template(
    """
    너는 KBO 야구 규칙 전문 AI야.

    반드시 아래 기준을 따라:
    1️⃣ 검색된 규칙 내용만 활용해 답변해.
    2️⃣ 규칙에 없는 건 절대 추측하지 마.
        - 해당 규칙을 찾지 못했습니다. 라고 말해.
    3️⃣ 초보자도 이해하게, 3줄 이내로.
    4️⃣ 조항번호 등 전문적인 문구는 드러내지 마.
    5️⃣ 필요하면 간단한 예시 한 줄만 추가해.

    질문: {rephrased_question}
    규칙 근거: {retrieved_context}

    답변:
    """
    )

    final_chain = (
        RunnablePassthrough.assign(rephrased_question=rephrase_chain)
        | RunnablePassthrough.assign(
            retrieved_context=lambda x: retriever.invoke(x["rephrased_question"])
        )
        | response_prompt
        | llm
        | StrOutputParser()
    )

    return final_chain
