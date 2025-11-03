import base64, json
import os

import oracledb
from datetime import datetime
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()
client = OpenAI()

def parse_date_iso(date_str: str):
    for fmt in ["%Y년 %m월 %d일(%a) %H:%M", "%Y-%m-%d", "%Y/%m/%d"]:
        try:
            return datetime.strptime(date_str, fmt).isoformat()
        except:
            continue
    return date_str

TEAM_NAME_MAP = {
    "Dinos": "NC",
    "롯데자이언츠": "롯데",
    "삼성라이온즈": "삼성",
    "기아타이거즈": "KIA",
    "한화이글스": "한화",
    "LG TWINS": "LG",
    "LG트윈스": "LG",
    "두산베어스": "두산",
    "SSG랜더스": "SSG",
    "K": "키움",
    "히어로즈": "키움"
}
# -----------------------------
# JSON 마크다운 제거
# -----------------------------
def parse_ticket_json(ticket_result: str):
    cleaned = ticket_result.strip()
    if cleaned.startswith("```json"):
        cleaned = cleaned[7:]
    if cleaned.endswith("```"):
        cleaned = cleaned[:-3]
    cleaned = cleaned.strip()
    try:
        return json.loads(cleaned)
    except:
        return {"raw": ticket_result}
    
def get_oracle_connection():
    return oracledb.connect(
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        dsn=f"{os.getenv('DB_HOST')}:{os.getenv('DB_PORT')}/{os.getenv('DB_SERVICENAME')}"
    )

# -----------------------------
# 티켓 검증 함수
# -----------------------------
def verify_ticket_url(image_url: str):
    prompt = """
    이 이미지를 기반으로 야구 티켓인지 판단해주세요.
    조건:
    - 반드시 QR코드가 존재해야 티켓으로 인정
    - 결과는 JSON 형식
    JSON 스키마:
    {
        "is_ticket": true/false,
        "qr_present": true/false,
        "date": "...",
        "seat": "...",
        "home_team": "...",
        "away_team": "..."
    }
    """
    resp = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": "당신은 야구 티켓 검증 전문가입니다."},
            {"role": "user", "content": [
                {"type": "text", "text": prompt},
                {"type": "image_url", "image_url": {"url": image_url}}
            ]}
        ],
        temperature=0
    )

    ticket_result = resp.choices[0].message.content.strip()
    ticket_json = parse_ticket_json(ticket_result)

    # 날짜 포맷 정리
    if "date" in ticket_json:
        ticket_json["date"] = parse_date_iso(ticket_json["date"])
    
    # 매핑 적용 후 home_team, away_team에 덮어쓰기
    ticket_json["home_team"] = TEAM_NAME_MAP.get(ticket_json.get("home_team"), ticket_json.get("home_team"))
    ticket_json["away_team"] = TEAM_NAME_MAP.get(ticket_json.get("away_team"), ticket_json.get("away_team"))
    print("🔹 팀 이름 매핑 후 ticket_json:", ticket_json)
    # DB에서 경기 기록 조회
    game_info = fetch_game_record(ticket_json)
    ticket_json["game_info"] = game_info

    if game_info:
        date_time_str = f"{game_info['game_date'].split()[0]} {game_info['game_time']}"
        ticket_json["date"] = date_time_str
    print("🔹 fetch_game_record 결과 game_info:", game_info)
    return ticket_json

def fetch_game_record(ticket_info: dict):
    date_obj = datetime.fromisoformat(ticket_info.get("date"))
    home_team = ticket_info.get("home_team")
    away_team = ticket_info.get("away_team")
    game_date = date_obj.strftime("%Y-%m-%d")
    print("🔹 검색 날짜:", game_date)
    print("🔹 홈팀:", home_team)
    print("🔹 원정팀:", away_team)
    query = """
        SELECT * FROM KBO_SCHEDULE
        WHERE TRUNC(GAME_DATE) = TO_DATE(:game_date, 'YYYY-MM-DD')
            AND UPPER(TRIM(HOME_TEAM)) = UPPER(:home_team)
            AND UPPER(TRIM(AWAY_TEAM)) = UPPER(:away_team)
    """
    with get_oracle_connection() as conn:
        with conn.cursor() as cursor:
            cursor.execute(query, {
                "game_date": game_date,
                "home_team": home_team,
                "away_team": away_team
            })
            row = cursor.fetchone()
            print("🔹 조회 결과 행:", row)

            if row:
                return {
                    "away_team_score": row[0],
                    "game_date": str(row[1]),
                    "home_team_score": row[2],
                    "game_time": str(row[5]),
                    "id": row[6],
                    "away_team": row[7],
                    "home_team": row[11],
                    "stadium": row[14],
                    "victory_team": row[15]
                }
            else:
                print("조회실패!!!")
                return None


def analyze_photo_url(image_url: str):
    """
    야구장 사진을 분석하여 음식(food)과 분위기(mood)를 JSON으로 반환합니다.
    """

    system_prompt = """
    당신은 야구장 사진을 분석하는 전문가입니다.
    사용자가 제공한 이미지를 바탕으로 음식과 응원 분위기를 JSON 형태로만 응답하세요.
    """

    user_prompt = """
    아래 이미지를 분석해서 음식(food)과 야구장 분위기(mood)를 JSON 형식으로 작성해 주세요.

    - food: 사진 속에 보이는 음식 이름들을 리스트로 작성 (예: ["치킨", "맥주"])
    - mood: cheering_items, uniforms, cheerleaders, weather 정보를 포함해야 함
      {
        "cheering_items": ["응원봉", "플래카드"],
        "uniforms": ["팀 유니폼", "모자"],
        "cheerleaders": "치어리더들이 활기차게 응원",
        "weather": "맑고 화창한 날씨"
      }

    ⚠️ 실제 이미지에서 보이는 요소만을 근거로 분석하고, 추측은 금지합니다.
    """

    try:
        resp = client.chat.completions.create(
            model="gpt-4o-mini",  # ✅ 이미지 분석 가능 모델
            response_format={"type": "json_object"},  # ✅ JSON만 반환하게 강제
            messages=[
                {"role": "system", "content": system_prompt},
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": user_prompt},
                        {"type": "image_url", "image_url": {"url": image_url}},  # ✅ 핵심
                    ],
                },
            ],
            max_tokens=400,
        )

        # ✅ 응답을 그대로 JSON 파싱
        analysis_data = json.loads(resp.choices[0].message.content)
        print("✅ 이미지 분석 결과:", analysis_data)

        return {
            "photo_url": image_url,
            "analysis": analysis_data
        }

    except Exception as e:
        print("❌ 이미지 분석 실패:", e)
        return {
            "photo_url": image_url,
            "analysis": {
                "food": [],
                "mood": {
                    "cheering_items": [],
                    "uniforms": [],
                    "cheerleaders": "정보 없음",
                    "weather": "정보 없음"
                }
            }
        }

def generate_diary(ticket_data: dict, photo_analysis: list, game_info: dict):
    foods = []
    moods_summary = []

    for item in photo_analysis:
        analysis_obj = item.get("analysis", {})
        foods.extend(analysis_obj.get("food", []))

        mood = analysis_obj.get("mood", {})
        mood_desc = (
            f"응원 도구: {', '.join(mood.get('cheering_items', [])) or '정보 없음'}, "
            f"유니폼: {', '.join(mood.get('uniforms', [])) or '정보 없음'}, "
            f"치어리더: {mood.get('cheerleaders', '정보 없음')}, "
            f"날씨: {mood.get('weather', '정보 없음')}"
        )
        moods_summary.append(mood_desc)

    food_str = ", ".join(set(foods)) or "기억이 안 나는 음식"
    mood_str = " / ".join(moods_summary)

    prompt = f"""
    오늘 {game_info['home_team']} vs {game_info['away_team']} 경기 직관!
    경기장: {game_info['stadium']}, 좌석: {ticket_data['seat']}
    경기 결과: {game_info['home_team_score']} : {game_info['away_team_score']}

    내가 먹은 음식: {food_str}
    경기장 분위기: {mood_str}

    위 정보를 바탕으로, 생생한 직관 일기를 작성해주세요.
    감정 표현과 현장 묘사를 풍부하게, 그러나 자연스럽게.
    제목은 작성하지 마세요.
    음식은 꼭 포함 해주세요.
    """
    resp = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": prompt}],
        max_tokens=700
    )

    diary_text = resp.choices[0].message.content.strip()
    return diary_text