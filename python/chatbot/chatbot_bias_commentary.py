import os
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
import logging
import requests

logging.getLogger("httpx").setLevel(logging.WARNING)

load_dotenv()
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")

# ✅ LLM 설정 (톤 자연화)
llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.3)


# ✅ 경기 상황 서술
flavor_prompt = ChatPromptTemplate.from_template("""
다음 KBO 경기 데이터({game_description})를 기반으로
간결하게 플레이를 재구성한다.

출력은 한 문단, 간결/중계톤.
- 구종 1개
- 타구 종류/방향 (해당 시)
- 이닝상황: 1~6 → 초반, 7~9 → 후반
- 역사적 기록, 날짜, 주석, 예시 금지
""")

def added_game_data(game_description: str) -> str:
    return (flavor_prompt | llm | StrOutputParser()).invoke(
        {"game_description": game_description}
    )


# ✅ 최종 편파 해설 생성 (형식 엄격 고정)
commentary_prompt = ChatPromptTemplate.from_template("""
너는 KBO AI 편파 해설가다 ⚾

아래 재구성된 데이터({flavor_text})를 참고하되
내용을 재정리하거나 추가 금지.
오직 {main_actor} 중심 해설만 작성.

✅ 형식 (절대 변경 금지)
🎙️ {main_actor} 중심 플레이 1문장.
감정 반영 문장.
상황 정리 또는 기대 1문장.

✅ 감정 강도별 톤
- level5: 확실한 긍정/응원 강조
- level4: 긍정 + 절제
- level3: 중립 + 기대
- level2: 아쉬움 + 차분
- level1: 실망 + 과격 금지

✅ 시점: {perspective}
✅ 상대: {opponent}

✅ 금지:
- “설명:”, “기록 필드”, 목록, 표, 날짜/팀명 확장
- 분석가/데이터 나열
- "하지만, 그러나, 그리고" 전환사
- 3문장 초과
""")


# ✅ 반복 방지 캐시
last_commentary = {"home": None, "away": None}


# ✅ WebSocket 메시지 전달
def send_message(message, sid, isHome):
    try:
        spring_url = os.getenv("SPRING_SERVER_URL", "http://localhost:8080")
        res = requests.post(
            f"{spring_url}/api/chat/notice/{sid}",
            json={"type": "bias-comment", "message": message, "isHome": isHome, "simulationId": sid}
        )
        print(f"{'✅' if res.ok else '❌'} Spring 전달 / {res.status_code}")
    except Exception as e:
        print("[!] 전달 오류:", e)


# ✅ 메인 함수: 편파 해설 생성
def generate_bias_commentary(game_description, bias_team, sid, isHome):
    global last_commentary

    flavor_text = added_game_data(game_description)

    is_attacking = (game_description["공격팀"] == bias_team)
    perspective = "공격" if is_attacking else "수비"
    main_actor = game_description["타자"] if is_attacking else game_description["투수"]
    opponent = game_description["투수"] if is_attacking else game_description["타자"]

    # ✅ 감정 강도는 외부 로직 결과 그대로 사용
    emotion_level = game_description.get("감정 레벨", 3)

    chain = commentary_prompt | llm | StrOutputParser()
    result = chain.invoke({
        "flavor_text": flavor_text,
        "bias_team": bias_team,
        "perspective": perspective,
        "main_actor": main_actor,
        "opponent": opponent,
        "emotion_level": emotion_level
    })

    key = "home" if isHome else "away"

    # ✅ 반복 방지: 동일하면 표현만 약간 바꾸기
    if result == last_commentary[key]:
        result = chain.invoke({
            "flavor_text": flavor_text,
            "bias_team": bias_team,
            "perspective": perspective,
            "main_actor": main_actor,
            "opponent": opponent,
            "emotion_level": emotion_level,
            "variation": "동일 의미, 다른 표현"
        })

    last_commentary[key] = result

    if sid:
        send_message(result, sid, isHome)

    return result
