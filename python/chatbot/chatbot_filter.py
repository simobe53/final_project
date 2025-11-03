import os
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate

# 실행할 경우 가상환경에 설치해야 하는 파일
# pip install langchain langchain-openai langchain-chroma python-dotenv

# .env 환경변수 로드
load_dotenv()
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")

def first_filter():
    """
    1차 필터: 사용자의 질문이 야구 관련인지 판별
    - 반환: "OK" / "NO"
    """
    llm = ChatOpenAI(model="gpt-4.1-mini", temperature=0)

    filter_prompt_template = """
    너는 사용자의 질문이 야구와 관련 있는지를 판별하는 AI야.
    아래 기준에 따라 오직 "OK" 또는 "NO" 중 하나만 반환해.

    ### 판별 기준
    1️⃣ 명확히 야구 관련
       - KBO, 팀, 선수, 구장, 경기, 기록, 규칙, 용어 등
       - 예시:
         - "김광현 요즘 어때?" → OK
         - "콜드게임 조건이 뭐야?" → OK
         - "한화 요즘 잘해?" → OK
         - "잠실구장 맛집 알려줘" → OK

    2️⃣ 애매하거나 비야구
       - 일상 대화, 다른 종목, 단순 지역/행사 관련
       - 예시:
         - "서울 날씨 어때?" → NO
         - "오늘 뭐 먹지?" → NO
         - "문학구장 행사 일정 알려줘" → NO
         - "야구 말고 축구 얘기해줘" → NO

    3️⃣ 다의어 처리
       - "볼", "아웃", "세이프" 등은 야구 의미로 가능하면 OK.

    4️⃣ 구장 관련 예외 처리
       - '야구장', '구장' + 음식 관련 단어(맛집, 치킨, 맥주, 밥집 등) → OK
       - 구장 이름이 직접 들어가면 무조건 OK:
         잠실, 문학, 사직, 대전, 대구, 고척, 광주, 수원, 창원, 인천 등

    ### 출력 형식
    반드시 "OK" 또는 "NO"만 반환.
    """

    filter_prompt = ChatPromptTemplate.from_template(filter_prompt_template)
    filter_chain = filter_prompt | llm | StrOutputParser()
    return filter_chain


def mid_filter():
    """
    2차 필터: 야구 관련 질문의 세부 유형 분류
    - 출력: rules / playerInfo / analysis / restaurant_rec / error
    """
    llm = ChatOpenAI(model="gpt-4.1-mini", temperature=0)

    prompt_template = """
    너는 야구 관련 질문을 아래 네 가지 유형 중 하나로 분류하는 AI야.
    사용자의 질문({user_question})을 보고 가장 자연스럽게 맞는 유형 하나만 출력해.
    다만, 문장이 너무 짧거나 여러 의미가 섞이면 'error'로 반환해.

    ### 유형 정의
    1️⃣ rules → 경기 규칙 / 용어 관련
       - 포함 단어 예시: 규칙, 룰, 삼진, 볼넷, 스트라이크, 콜드게임, 플라이, 병살, 도루, 번트, 투수 교체 등

    2️⃣ playerInfo → 특정 선수 관련
       - 선수 이름(한글 또는 영어)이 포함된 질문
       - 성적, 근황, 활약, 타율, 방어율 등 선수 개인 관련 단어가 있으면 포함
       - 예: 김광현, 박성한, 류현진, 오타나더라도 KBO 선수와 유사하면 OK

    3️⃣ analysis → 팀 관련
       - 포함 단어 예시: 한화, LG, 두산, SSG, 롯데, 삼성, 키움, NC, KT, KIA
       - 팀 이름이 들어가거나 팀 성적, 순위, 경기력 등과 관련된 질문

    4️⃣ restaurant_rec → 구장 / 맛집 관련
       - 포함 단어 예시:
         잠실, 문학, 사직, 대전, 대구, 고척, 광주, 수원, 창원, 인천
         + 맛집, 밥집, 치킨, 맥주, 포차, 카페, 식당, 술집 등
       - 단어 조합만으로도 OK (예: "문학 치킨집", "사직 맥주")

    ### 분류 규칙
    - 선수 + 팀, 팀 + 구장 등 두 가지 유형 이상 섞이면 → 'error'
    - 선수나 팀이 2개 이상 동시에 등장하면 → 'error'
    - 질문이 한 단어 수준으로 짧거나 의미 불분명하면 → 'error'

    ### 출력 형식
    rules / playerInfo / analysis / restaurant_rec / error
    """

    prompt = ChatPromptTemplate.from_template(prompt_template)
    chain = prompt | llm | StrOutputParser()
    return chain


# def second_filter():
#     """
#     랭체인 기반으로 질문을 필터링하는 함수
#     """
    
#     # 2. 언어 모델(LLM) 정의
#     llm = ChatOpenAI(model="gpt-4.1-mini", temperature=0)

#     # 3. 쿼리 재구성 프롬프트
#     filter_prompt_template = """
#     너는 사용자의 질문이 주어진 질문 유형({question_type})과 일치하는지 확인하는 필터링 전문 AI야.
#     사용자가 보낸 질문({user_question})이 야구 관련임은 이미 확인되었다고 가정한다.
    
#     1. 질문 유형({question_type})과 질문 내용({user_question})이 일치하면:
#     - 여러 유형의 단어가 함께 들어 있어도, 질문 유형({question_type})을 기준으로 우선 처리해.
#     - 다른 유형의 단어가 섞여 있어도 무시하고, 질문 유형과 가장 관련 있는 방향으로 해석해.
#     - 다의어라서 다른 의미로도 쓰이는 단어라도, 
#       야구에서 사용되는 의미가 있다면 반드시 "OK"를 반환해야 해.
#       절대 애매하다고 판단하지 마.
#       예를 들어, '아웃', '볼', '세이프', '스트라이크', '플라이' 같은 단어들은
#       일상에서도 쓰이지만 야구 용어이기도 하므로 항상 "OK"야.
#     - 반환 형식: "OK"

#     2. 질문 유형({question_type})과 질문 내용({user_question})이 일치하지 않으면:
#     - 질문 내용에 맞춰 친절하고 자연스럽게 재질문을 유도해.
#     - 예시 안내 메시지:
#         - "질문 유형과 맞지 않는 내용이에요. {question_type}에 맞는 질문으로 다시 작성해 주세요."
#         - "이 질문은 선택된 유형({question_type})과 일치하지 않습니다. 다시 질문해 주세요."
    
#     3. 가능한 질문 유형 예시:
#     - 야구 규칙
#     - 선수 정보
#     - 팀 분석
#     - 구장 주변 맛집 추천

#     4. 팀 이름이 들어가는 질문이면 팀 분석 질문으로 간주해.
#     밑에 이게 현재 KBO 10개 프로팀이야.
#     - "엔씨": "NC",
#     - "NC 다이노스": "NC",
#     - "엘지": "LG",
#     - "LG 트윈스": "LG",
#     - "두산 베어스": "두산",
#     - "롯데 자이언츠": "롯데",
#     - "한화 이글스": "한화",
#     - "삼성 라이온즈": "삼성",
#     - "키움 히어로즈": "키움",
#     - "KT 위즈": "KT",
#     - "케이티": "KT",
#     - "기아": "KIA",
#     - "KIA 타이거즈": "KIA",
#     - "쓱": "SSG",
#     - "SSG 랜더스": "SSG"

#     5. 야구장과 관련 있는 단어가 있으면 구장 주변 맛집 추천으로 간주해.
#     - 팀 이름과 구장을 애매하게 물어보는 경우에도 구장 주변 맛집 추천으로 간주해.
#     - SSG구장이나 삼성구장 처럼 애매하게 질문하는 경우.
#     - 잠실구장
#     - 잠실야구장
#     - 문학구장
#     - 사직구장
#     - 사직야구장
#     - 대전한화생명볼파크
#     - 대구삼성라이온즈파크
#     - 고척스카이돔
#     - 광주기아챔피언스필드
#     - 수원KT위즈파크
#     - 창원NC파크
#     - 인천SSG랜더스필드

#     6. 반드시 위 규칙에 따라 OK 또는 안내 메시지 중 하나만 반환하고, 다른 응답은 섞지 마.
#     """

#     filter_prompt = ChatPromptTemplate.from_template(filter_prompt_template)
    
#     # LLM과 연결, 결과는 문자열로 받기
#     filter_chain = filter_prompt | llm | StrOutputParser()
    
#     return filter_chain