import json
import os
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough, RunnableLambda

from chatbot.chatbot_tools import get_place_data

load_dotenv()
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")

def debug_log(data):
    print("\n--- 🕵️‍♂️ 중간 데이터 확인 ---")
    print(f"사용자 원본 질문: {data.get('user_question')}")
    print(f"LLM이 재구성한 질문: {data.get('rephrased_question')}")
    print("------------------------")
    return data

def restaurant_answer():
    llm = ChatOpenAI(model="gpt-4.1-mini", temperature=0)

    # 1. 질문 재구성 프롬프트
    rephrase_prompt = ChatPromptTemplate.from_template(
    """
    너는 KBO 구장 기반의 검색어를 만드는 AI야.  
    사용자의 입력에서 '팀 이름' 또는 '구장 이름'을 찾아,  
    정확한 [공식 구장 명칭]으로 바꾼 뒤, 사용자의 원래 검색어 맥락을 그대로 유지해줘.  

    ⚙️ 규칙:
    1. 출력은 한 줄짜리 검색어 문장으로 만들어.  
    예: “잠실 야구장 치킨집”, “대전 한화생명 이글스파크 카페”  
    2. 괄호, 설명, 불필요한 단어는 절대 붙이지 마.  
    3. '야구장'이나 '구장'이 다른 단어와 붙어 있으면 자동으로 띄어쓰기해.  
    예: '잠실야구장' → '잠실 야구장', '고척구장' → '고척 구장'  
    4. '인천SSG랜더스필드'처럼 이미 완전한 고유명사는 그대로 사용.  
    5. 팀 이름만 있을 경우, 그 팀의 홈구장으로 자동 변환.  
    (예: ‘한화 카페’ → ‘대전 한화생명 이글스파크 카페’)  
    6. 구장명 또는 팀명이 전혀 없을 경우, 입력을 그대로 사용.  
    7. 절대 문장형 설명을 하지 말고, 결과는 검색어 하나만 출력해.  
    8. 팀명과 구장명이 없더라도 지역명이 있으면 
    그 지역의 대표 야구장으로 자동 변환해.
    예: "대구" → "대구삼성라이온즈파크"

    출력 형식:
    👉 [구장명 또는 지역명 + 사용자 입력 키워드]

    9.아래 KBO 팀 이름과 구장 이름을 참고해서 검색어를 만들어줘.
    - "엔씨": "NC",
    - "NC 다이노스": "NC",
    - "엘지": "LG",
    - "LG 트윈스": "LG",
    - "두산 베어스": "두산",
    - "롯데 자이언츠": "롯데",
    - "한화 이글스": "한화",
    - "삼성 라이온즈": "삼성",
    - "키움 히어로즈": "키움",
    - "KT 위즈": "KT",
    - "케이티": "KT",
    - "기아": "KIA",
    - "KIA 타이거즈": "KIA",
    - "쓱": "SSG",
    - "SSG 랜더스": "SSG"
    - "문학" : "인천SSG랜더스필드"
    - "잠실구장" : "잠실야구장"
    - 사직야구장
    - 잠실야구장
    - 대전한화생명볼파크
    - 대구삼성라이온즈파크
    - 고척스카이돔
    - 광주기아챔피언스필드
    - 수원KT위즈파크
    - 창원NC파크
    - 인천SSG랜더스필드

    질문: {user_question}
    재구성된 질문:""")
    rephrase_chain = rephrase_prompt | llm | StrOutputParser()

    # 3. 최종 답변 생성 프롬프트
    place_data_prompt = ChatPromptTemplate.from_template(
    """
    너는 ⚾ KBO 구장 근처 맛집을 추천하는 AI야.
    아래 [검색 결과]는 카카오맵 API에서 가져온 데이터야.
    [검색 결과]를 자연스럽게 추천 형식으로 요약해줘.
    [검색된 구장 이름]을 그대로 사용해.

    [규칙]
    1️⃣ {rephrased_question}을 한줄 소개글로 바꿔줘.
    2️⃣ 각 항목 앞에는 업종에 맞는 이모지를 붙여줘.
    3️⃣ 마지막에 따뜻한 코멘트로 마무리해.
    4️⃣ 각 항목은 줄바꿈으로 구분해.

    [검색 결과]
    {place_data}

    [출력 예시]
    잠실야구장 근처에는 이런 맛집이 있어요 😋  
    1️⃣ 🍔 크라이치즈버거 삼성역점  
    → 간단하게 식사하기 좋아요.  
    2️⃣ 🍲 중앙해장  
    → 얼큰한 해장국으로 인기예요.  
    3️⃣ 🐟 바다풍경  
    → 회와 식사를 조용히 즐길 수 있어요.  
    ⚾ 모두 경기 전후 들르기 좋은 곳이에요!
    """)

    # 체인 연결
    final_chain = (
        RunnablePassthrough.assign(rephrased_question=rephrase_chain)
        # 이 부분을 추가하여 LLM이 재구성한 질문이 무엇인지 확인하는 용도
        | RunnableLambda(debug_log)
        | RunnablePassthrough.assign(place_data=lambda x: json.dumps(get_place_data(x["rephrased_question"]), ensure_ascii=False, indent=2))
        | place_data_prompt | llm | StrOutputParser())
    
    return final_chain