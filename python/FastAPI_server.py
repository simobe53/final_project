# -*- coding: utf-8 -*-
from typing import Dict, List
from fastapi import BackgroundTasks, FastAPI, HTTPException, Query, Request, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import uvicorn
from dotenv import load_dotenv
import os
from openai import BaseModel, OpenAI
import sys
import oracledb
import shutil
from pathlib import Path
from model.diary_generator import verify_ticket_url
from chatbot.chatbot_bias_commentary import generate_bias_commentary
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from chatbot.chatbot_main import chatbot_process
from model.article_generator import save_articles
from model.review_summarizer import review_summarizer
from model.highlight_summarizer import highlight_summarizer
from model.news_summarizer import news_summarizer
from model.simulation_chat import simulation_AI
from model.fraud_detection_model import FraudDetectionModel
import time
import random
import asyncio

from model.baseball_game_simulator import BaseballGameSimulator
from image_service import ImageService, ImageGenerationRequest
import requests
from model.at_bat_simulator import AtBatSimulator
from model.suno import SunoAPI
from model.youtube_music_extractor import YouTubeMusicExtractor
from model.youtube_search import get_youtube_searcher

app = FastAPI()
load_dotenv()
client = OpenAI()

# Suno 콜백을 위한 전역 이벤트 저장소
pending_suno_tasks: Dict[str, asyncio.Event] = {}
# Suno 에러 정보 저장소
suno_task_errors: Dict[str, dict] = {}
# Suno task_id와 user_id 매핑
suno_task_users: Dict[str, str] = {}

# 이미지 서비스 초기화
image_service = ImageService(client)

def save_to_database(result, request):
    """생성된 이미지를 데이터베이스에 저장"""
    try:
        # 이미지 URL에서 이미지 데이터 다운로드
        image_response = requests.get(result["image_url"])
        if image_response.status_code == 200:
            # Base64로 인코딩
            import base64
            image_base64 = base64.b64encode(image_response.content).decode('utf-8')
            
            # request에서 user_id 가져오기 (없으면 1로 기본값)
            user_id = getattr(request, 'user_id', 1)
            
            # Spring Boot 서버로 저장 요청
            save_data = {
                "userId": user_id,  # 프론트엔드에서 전달받은 사용자 ID
                "teamId": None,  # 임시로 null (실제로는 선택한 팀 ID)
                "koreanPrompt": request.korean_prompt,
                "englishPrompt": result["english_prompt"],
                "imageBase64": image_base64,
                "imageUrl": result["image_url"],
                "filename": result.get("filename", f"ai_uniform_{int(time.time())}.png"),
                "fileSize": len(image_response.content),
                "imageSize": request.size
            }
            
            # Spring Boot 서버에 저장 요청
            springboot_url = "http://localhost:8080/api/ai-uniform"
            try:
                response = requests.post(springboot_url, json=save_data)
                
                if response.status_code == 200:
                    print("✅ 데이터베이스 저장 성공")
                else:
                    print(f"❌ 데이터베이스 저장 실패: {response.status_code}")
                    print(f"응답 내용: {response.text}")
            except Exception as e:
                print(f"❌ Spring Boot 서버 연결 실패: {str(e)}")
                raise e
                
    except Exception as e:
        print(f"❌ 데이터베이스 저장 중 오류: {str(e)}")
        raise e

# CORS 설정

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# =============================================
# 실시간 타석별 시뮬레이션
# =============================================
simulation_logs = {}
@app.post("/simulate-at-bat")
def simulate_complete_at_bat(request: dict,background_tasks: BackgroundTasks):
    try:
        simulator = BaseballGameSimulator()
        sim_id = request.get("simulation_id")
        if not sim_id:
            return {"error": "simulation_id 필요"}
        if sim_id not in simulation_logs:
            simulation_logs[sim_id] = {
                "home": {"name": "홈팀", "score": 0},
                "away": {"name": "원정팀", "score": 0},
                "innings": []
            }
        game_log = simulation_logs[sim_id]
        TEAM_MAP = {
            1: "롯데 자이언츠",
            2: "LG 트윈스",
            3: "한화 이글스",
            4: "삼성 라이온즈",
            5: "키움 히어로즈",
            6: "두산 베어스",
            7: "KIA 타이거즈",
            8: "KT Wiz",
            9: "NC 다이노스",
            10: "SSG 랜더스",
        }
        home_team_id = request.get("home_team")
        away_team_id = request.get("away_team")
        home_team_name = TEAM_MAP.get(home_team_id, "홈팀")
        away_team_name = TEAM_MAP.get(away_team_id, "원정팀")
        game_log["home"]["name"] = home_team_name
        game_log["away"]["name"] = away_team_name

        # 게임 상황 추출
        inning = request.get("inning", 1)
        half = request.get("half", "초")
        outs = request.get("outs", 0)
        runners = []
        if request.get("base1"):
            runners.append("1루")
        if request.get("base2"):
            runners.append("2루")
        if request.get("base3"):
            runners.append("3루")

        # 선수 정보
        batter_info = request.get("batter")
        pitcher_info = request.get("pitcher")
        batter_name = batter_info.get('player_name') if batter_info else 'Unknown'
        pitcher_name = pitcher_info.get('player_name') if pitcher_info else 'Unknown'

        # AI 모델로 타석 결과 예측
        result, probabilities = simulator.predict_at_bat_result(
            inning, outs, runners, batter_info, pitcher_info,
            'p_home' if half == "초" else 'p_away'
        )

        if result is None:
            return {"error": "예측 실패"}

        # 완전한 시뮬레이션 수행 (비즈니스 로직은 AtBatSimulator에 위임)
        simulation_result = AtBatSimulator.perform_complete_simulation(
            request, result, batter_info, pitcher_info
        )

        if simulation_result.get("error"):
            return simulation_result
        # 예측 확률 정보 추가
        simulation_result["probabilities"] = probabilities

        # 로그 출력
        print(f"\n{'='*70}")
        print(f"[{inning}회{half} {outs}아웃] {batter_name} vs {pitcher_name}")
        print(f"주자 상황: {', '.join(runners) if runners else '주자 없음'}")
        print(f"결과: {result} (확률: {probabilities.get(result, 0):.1%})")
        print(f"베이스 변화: ", end="")

        new_state = simulation_result.get("new_game_state", {})
        new_bases = []
        if new_state.get("base1"):
            new_bases.append("1루")
        if new_state.get("base2"):
            new_bases.append("2루")
        if new_state.get("base3"):
            new_bases.append("3루")
        print(f"{', '.join(new_bases) if new_bases else '주자 없음'}")

        print(f"득점: {simulation_result.get('rbi', 0)}점")
        print(f"스코어: {new_state.get('awayScore', 0)} - {new_state.get('homeScore', 0)}")
        print(f"{'='*70}\n")

        game_log["home"]["score"] = new_state.get("homeScore", game_log["home"]["score"])
        game_log["away"]["score"] = new_state.get("awayScore", game_log["away"]["score"])

        inning_idx = inning - 1
        if len(game_log["innings"]) <= inning_idx:
            game_log["innings"].append({"inning": f"{inning}회", "plays": []})

        game_log["innings"][inning_idx]["plays"].append({
            "outs": outs,
            "bases": runners,
            "score": f"{new_state.get('awayScore',0)}-{new_state.get('homeScore',0)}",
            "batter": batter_name,
            "pitcher": pitcher_name,
            "result": result
        })

        # 주자 상황 텍스트
        runner_log = "주자 없음" if not runners else "만루" if len(runners) == 3 else ", ".join(runners)
        hit_result  = '안타' if result == '1루타' else result
        hit_result  = '볼넷' if result == '4구' else result

        # 현재 타석 결과 텍스트
        game_description = {
        "이닝": f"{inning}회{half}",
        "아웃": f"{outs}아웃",
        "타자": batter_name,
        "투수": pitcher_name,
        "주자 상황": runner_log,
        "타석 결과": hit_result,
        "득점": f"{simulation_result.get('rbi', 0)}점",
        "스코어": f"{away_team_name} {new_state.get('awayScore', 0)} - {home_team_name} {new_state.get('homeScore', 0)}",
        "공격팀": away_team_name if half == '초' else home_team_name,
        "수비팀": home_team_name if half == '초' else away_team_name,
        }

        # 타석 결과와 편파팀을 llm에 보낸 후에 편파 해설 텍스트 확인
        print("=" * 100)
        time.sleep(1 + (random.random() - 0.5))
        home_comment = generate_bias_commentary(game_description, home_team_name, sim_id, 1)
        print("홈팀 편파 : " + home_comment)
        print("=" * 100)

        time.sleep(1 + (random.random() - 0.5))
        away_comment = generate_bias_commentary(game_description, away_team_name, sim_id, 0)
        print("원정팀 편파 : " + away_comment)
        print("=" * 100)

        sim_id = request.get("simulation_id")
        if not sim_id:
            return {"error": "simulation_id 필요"}
        
        # send_message(home_comment, sim_id, 1)
        # send_message(away_comment, sim_id, 0)

        if simulation_result.get("game_ended"):
            background_tasks.add_task(
                save_articles,
                simulation_id=sim_id,
                game_log=game_log
            )

        return simulation_result

    except Exception as e:
        print(f"ERROR 시뮬레이션 오류: {str(e)}")
        return {"error": f"시뮬레이션 오류: {str(e)}"}

@app.post("/api/ai/suno/callback")
async def suno_callback(request: Request):
    """Suno API로부터의 콜백을 받아서 대기 중인 작업에 알림"""
    try:
        data = await request.json()
        print(f"📥 Suno 콜백 수신: {data}")

        callback_data = data.get('data', {})
        task_id = callback_data.get('task_id')
        callback_type = callback_data.get('callbackType')

        # 에러 콜백 처리 (callbackType이 'error'이거나 code가 400번대인 경우)
        error_code = data.get('code')
        is_error = callback_type == 'error' or (error_code and error_code >= 400)

        if is_error:
            print(f"❌ Task {task_id} 에러 발생 (code: {error_code})")
            error_msg = data.get('msg', 'Unknown error')
            suno_task_errors[task_id] = {
                'code': error_code,
                'msg': error_msg,
                'data': callback_data
            }

            # 사용자에게 알림 전송
            user_id = suno_task_users.get(task_id)
            if user_id:
                try:
                    # SpringBoot 알림 API 호출
                    springboot_url = os.getenv('SPRING_SERVER_URL', 'http://localhost:8080')
                    notification_data = {
                        'userId': int(user_id),
                        'notificationType': 'SUNO_ERROR',
                        'title': '❌ 응원곡 생성 실패',
                        'message': f'응원곡 생성에 실패했습니다: {error_msg}',
                        'link': '/my/cheer-song',
                        'isUrgent': True
                    }

                    notification_response = requests.post(
                        f'{springboot_url}/api/notifications/send',
                        json=notification_data,
                        timeout=5
                    )

                    if notification_response.status_code == 200:
                        print(f"✅ 사용자 {user_id}에게 에러 알림 전송 완료")
                    else:
                        print(f"⚠️ 알림 전송 실패: {notification_response.status_code}")

                except Exception as notif_error:
                    print(f"⚠️ 알림 전송 중 오류: {str(notif_error)}")

        if task_id and task_id in pending_suno_tasks:
            # 대기 중인 작업에 알림 (폴링 즉시 재개)
            pending_suno_tasks[task_id].set()
            print(f"✅ Task {task_id} 알림 완료 - 폴링 즉시 재개")
        else:
            print(f"⚠️ Task {task_id} not found in pending tasks")

        return {"status": "received"}
    except Exception as e:
        print(f"❌ 콜백 처리 오류: {str(e)}")
        return {"error": str(e)}

@app.post("/suno/generate")
async def suno_generate(request: dict):
    try:
        # 사용자 ID 추출
        user_id = request.get("user_id")

        # 환경 변수로 로컬/배포 환경 분기
        # FRONT_URL이 https://my-ball.site이면 배포 환경, 아니면 로컬 환경
        front_url = os.getenv('FRONT_URL', 'http://localhost:5173')
        is_production = front_url == 'https://my-ball.site'

        # Callback URL 설정
        # 배포 환경: 실제 서비스 URL
        # 로컬 환경: 테스트용 webhook URL
        callback_url = 'https://my-ball.site/api/ai/suno/callback' if is_production else 'https://webhook.site/e0aade0a-6721-478d-ad61-706a075655b8'

        suno = SunoAPI(os.getenv('SUNOAI_API_KEY'), pending_tasks=pending_suno_tasks, task_errors=suno_task_errors)

        # OpenAI로 가사 생성
        print('Generating lyrics with OpenAI...')
        player_name = request.get("player_name", "선수")
        mood = request.get("mood", "신나는")

        lyrics_prompt = f"""
당신은 야구 응원가 작사가입니다. 다음 조건에 맞는 응원가 가사를 작성해주세요:

- 선수 이름: {player_name}
- 곡 분위기: {mood}
- 곡 길이: 40~50초 분량
- 구조: [Verse], [Chorus], [Ends] 순서로 구성
- 장르: 응원가
- 특징: 재밌있고 반복적이며 쉽게 따라 부를 수 있는 가사

가사만 작성하고, 각 섹션을 명확히 구분해주세요.
"""

        response = client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": "당신은 야구 응원가 전문 작사가입니다."},
                {"role": "user", "content": lyrics_prompt}
            ],
            temperature=0.8
        )

        lyrics = response.choices[0].message.content.strip()
        print(f'Generated lyrics:\n{lyrics}')

        if is_production:
            print(f'🌐 배포 환경 감지 - Callback URL: {callback_url}')
        else:
            print(f'💻 로컬 환경 감지 - 테스트용 Webhook URL: {callback_url}')

        # Generate music with custom parameters
        print('Generating music...')
        music_task_id = suno.generate_music(
            prompt=f'{lyrics}',
            customMode=True,
            style=mood,
            title=f'{player_name} 응원가',
            vocalGender="m",
            instrumental=False,
            model='V4_5',
            callBackUrl=callback_url
        )

        # task_id와 user_id 매핑 저장
        if user_id:
            suno_task_users[music_task_id] = user_id
            print(f"✅ Task {music_task_id} - User {user_id} 매핑 저장")

        # Wait for completion (하이브리드 방식: 폴링 + 콜백 알림)
        music_result = await suno.wait_for_completion(task_id_music=music_task_id, task_id_lyrics=None)
        print('Music generated successfully!')
        music = music_result['sunoData']
        print(music[0])

        # 작업 완료 후 매핑 정리
        if music_task_id in suno_task_users:
            del suno_task_users[music_task_id]
            print(f"🗑️ Task {music_task_id} user 매핑 정리 완료")

        # prompt에서 "duration:" 이후 텍스트 제거
        original_prompt = music[0]["prompt"]
        if "duration:" in original_prompt.lower():
            # "duration:" 이전 부분만 추출
            lyrics_only = original_prompt.split("duration:")[0].strip()
        else:
            lyrics_only = original_prompt

        return {
            "Lyrics": lyrics_only,
            "Title": music[0]["title"],
            "Duration": music[0]["duration"],
            "Audio URL": music[0]["audioUrl"],
            "Image URL": music[0]["imageUrl"],
            "streamAudioUrl": music[0]["streamAudioUrl"],
            "sourceStreamAudioUrl": music[0]["sourceStreamAudioUrl"],
            "sourceImageUrl": music[0]["sourceImageUrl"]
        }

    except Exception as error:
        print(f'Error: {error}')
        # 에러 발생 시에도 매핑 정리
        if 'music_task_id' in locals() and music_task_id in suno_task_users:
            del suno_task_users[music_task_id]
            print(f"🗑️ Task {music_task_id} user 매핑 정리 완료 (에러)")
        raise HTTPException(status_code=500, detail=str(error))

@app.post("/ai/chat")
async def ai_chat(request: Request):
    """OpenAI LangChain 기반 채팅 API"""
    try:
        data = await request.json()                 # JSON body를 dict로 변환
        message = data.get("message")               # 질문 내용

        if not message:
            raise HTTPException(status_code=400, detail="메시지가 비어 있습니다.")

        response = await chatbot_process(message)
        return {"message": response}

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"AI 서버 오류: {str(e)}")
    
@app.post("/api/reviews/summarize")
def summarize_reviews(request: dict):
    """리뷰 목록을 받아서 요약을 생성합니다."""
    try:
        reviews = request.get('reviews', [])

        if not reviews:
            return {"error": "리뷰 목록이 비어있습니다."}

        # 리뷰 텍스트만 추출 (comments 필드가 있다고 가정)
        review_texts = []
        for review in reviews:
            if isinstance(review, dict) and 'comments' in review:
                review_texts.append(review['comments'])
            elif isinstance(review, str):
                review_texts.append(review)

        if not review_texts:
            return {"error": "유효한 리뷰 텍스트를 찾을 수 없습니다."}

        # 요약 생성
        summary = review_summarizer.summarize_reviews(review_texts)

        return {
            "summary": summary,
            "review_count": len(review_texts),
            "success": True
        }

    except Exception as e:
        print(f"리뷰 요약 오류: {str(e)}")
        return {"error": f"리뷰 요약 중 오류가 발생했습니다: {str(e)}"}

@app.post("/api/news/summarize")
def summarize_news(request: dict):
    """뉴스 데이터를 받아서 요약을 생성합니다."""
    try:
        news_data = request.get('news', {})
        
        if not news_data:
            return {
                "success": False,
                "error": "뉴스 데이터가 비어있습니다."
            }
        
        # 뉴스 요약 생성
        summary = news_summarizer.summarize_news(news_data)
        
        return {
            "summary": summary,
            "success": True
        }
        
    except Exception as e:
        print(f"뉴스 요약 오류: {str(e)}")
        import traceback
        traceback.print_exc()
        return {
            "success": False,
            "error": f"뉴스 요약 중 오류가 발생했습니다: {str(e)}"
        }



@app.post("/api/news/{news_id}/summarize")
def summarize_news_by_id(news_id: int):
    """특정 뉴스 ID로 뉴스를 조회하고 요약을 생성합니다."""
    try:
        # 여기서는 실제 DB 조회 로직이 필요합니다
        # 현재는 예시 데이터를 사용합니다
        news_data = {
            "title": "예시 뉴스 제목",
            "content": "예시 뉴스 내용입니다.",
            "team_name": "LG 트윈스"
        }
        
        # 뉴스 요약 생성
        summary = news_summarizer.summarize_news(news_data)
        
        return {
            "news_id": news_id,
            "summary": summary,
            "success": True
        }
        
    except Exception as e:
        print(f"뉴스 ID 요약 오류: {str(e)}")
        return {"error": f"뉴스 요약 중 오류가 발생했습니다: {str(e)}"}

class ChatRequest(BaseModel):
    messages: List[str]

@app.post("/api/chat/isToxic")
def check_toxic(body: ChatRequest):
    """채팅 분위기 감지 AI"""
    messages = body.messages
    print(",".join(messages))

    isToxic = simulation_AI.check_toxic(messages) == "TRUE"
    return { "toxic": isToxic }

# =========================
# 이미지 생성 엔드포인트들
# =========================

# 1. 단일 이미지 생성
@app.post("/generate-image")
def generate_single_image(request: ImageGenerationRequest):
    """한국어 프롬프트로 단일 이미지를 생성합니다. (DB 저장은 프론트엔드에서 처리)"""
    result = image_service.generate_single_image(request)
    
    # 이미지 생성 성공 시 user_id 포함하여 반환 (프론트엔드에서 DB 저장에 사용)
    if result.get("success"):
        result["user_id"] = getattr(request, 'user_id', 1)
    
    return result


# 2. 프롬프트 번역만 수행
@app.post("/translate-prompt")
def translate_prompt(request: dict):
    """한국어 프롬프트를 영어로 번역만 수행합니다."""
    korean_prompt = request.get("korean_prompt", "")
    if not korean_prompt:
        raise HTTPException(status_code=400, detail="korean_prompt가 필요합니다")
    
    return image_service.translate_prompt_only(korean_prompt)

# 3. 저장된 이미지 목록 조회 (제거됨 - DB에서만 조회)
# 4. 이미지 다운로드 (제거됨 - DB에서만 다운로드)

class TicketRequest(BaseModel):
    image_url: str
@app.post("/verify_ticket_url")
def verify_ticket(req: TicketRequest):
    result = verify_ticket_url(req.image_url)
    return result

class PhotoRequest(BaseModel):
    image_url: str
@app.post("/analyze_photo")
def analyze_photo(req: PhotoRequest):
    from model.diary_generator import analyze_photo_url
    return analyze_photo_url(req.image_url)

class DiaryRequest(BaseModel):
    ticket_data: Dict
    photo_analysis: List[Dict]
    game_info: Dict
@app.post("/generate_diary")
def generate_diary(req: DiaryRequest):
    from model.diary_generator import generate_diary
    diary_text = generate_diary(req.ticket_data, req.photo_analysis, req.game_info)
    return {"diary": diary_text}

# =========================
# 하이라이트 요약 엔드포인트
# =========================

@app.post("/highlights/summarize")
def summarize_highlight(request: dict):
    """하이라이트 영상 AI 요약 생성"""
    video_url_or_id = request.get('video_url') or request.get('video_id')
    return highlight_summarizer.summarize_highlight(video_url_or_id)


model = FraudDetectionModel()

class RawData(BaseModel):
    data: str

@app.post("/recordDetection")
def record_detection(raw: RawData):
    try:
        return model.record_detection(raw.data)
    except Exception as e:
        return {"error": str(e)}

@app.post("/deployContract")
def deploy_contract():
    bytecode = os.getenv("CONTRACT_BYTECODE")
    if not bytecode:
        return {"error": "CONTRACT_BYTECODE 환경변수 필요"}
    try:
        address = model.deploy_contract(bytecode)
        return {"contract_address": address}
    except Exception as e:
        return {"error": str(e)}

# =============================================
# YouTube 검색
# =============================================

@app.post("/api/youtube/search")
async def youtube_search_music(request: dict):
    """YouTube에서 음악 검색"""
    try:
        query = request.get("query")
        max_results = request.get("max_results", 5)

        if not query:
            raise HTTPException(status_code=400, detail="query가 필요합니다")

        searcher = get_youtube_searcher()
        results = searcher.search_music(
            query=query,
            max_results=max_results
        )

        return {
            "success": True,
            "query": query,
            "results": results,
            "count": len(results)
        }

    except Exception as error:
        print(f'❌ YouTube 검색 오류: {error}')
        raise HTTPException(status_code=500, detail=str(error))


@app.post("/api/youtube/search-first")
async def youtube_search_first_music(request: dict):
    """YouTube에서 첫 번째 검색 결과 반환 (자동 선택용)"""
    try:
        query = request.get("query")

        if not query:
            raise HTTPException(status_code=400, detail="query가 필요합니다")

        searcher = get_youtube_searcher()
        result = searcher.search_first_music(query=query)

        if not result:
            raise HTTPException(status_code=404, detail="검색 결과가 없습니다")

        return {
            "success": True,
            "query": query,
            "result": result
        }

    except HTTPException:
        raise
    except Exception as error:
        print(f'❌ YouTube 검색 오류: {error}')
        raise HTTPException(status_code=500, detail=str(error))


# =============================================
# YouTube → Suno Cover
# =============================================

@app.post("/api/ai/suno/youtube-cover")
async def suno_youtube_cover(request: dict):
    """YouTube 음악을 기반으로 새로운 응원가 생성"""
    audio_path = None
    extractor = None

    try:
        # 사용자 ID 추출
        user_id = request.get("user_id")

        # 환경 변수 설정
        front_url = os.getenv('FRONT_URL', 'http://localhost:5173')
        fastapi_base_url = os.getenv('FASTAPI_BASE_URL', 'https://09c8cf9865f1.ngrok-free.app')
        is_production = front_url == 'https://my-ball.site'

        callback_url = 'https://my-ball.site/api/ai/suno/callback' if is_production else 'https://webhook.site/e0aade0a-6721-478d-ad61-706a075655b8'

        # 요청 파라미터
        youtube_url = request.get("youtube_url")
        player_name = request.get("player_name", "선수")
        mood = request.get("mood", "신나는")
        custom_mode = request.get("custom_mode", True)
        instrumental = request.get("instrumental", False)

        if not youtube_url:
            raise HTTPException(status_code=400, detail="youtube_url이 필요합니다")

        # YouTube 음악 추출기 초기화
        extractor = YouTubeMusicExtractor(client)

        # 1. YouTube 오디오 다운로드
        audio_path = extractor.download_audio(youtube_url)

        # 2. 음악 분석 (BPM, 비트, 리듬)
        music_analysis = extractor.analyze_music(audio_path)
        print(f'🎼 음악 분석 완료: BPM={music_analysis["bpm"]}, 템포={music_analysis["tempo_category"]}')

        # 3. 가사 추출 (Whisper API 직접 사용)
        original_lyrics = None
        lyrics_data = None
        print(f'🎤 Whisper API로 가사 추출 시작...')
        try:
            lyrics_data = extractor.extract_lyrics_whisper(audio_path)
            if lyrics_data:
                original_lyrics = lyrics_data["full_text"]
                print(f'✅ Whisper로 가사 추출 성공 ({lyrics_data["method"]})')
            else:
                print(f'⚠️ 가사 추출 실패 - 음악 분석만으로 진행')
        except Exception as e:
            print(f'❌ Whisper 가사 추출 실패: {e}')
            import traceback
            traceback.print_exc()
            print(f'⚠️ 가사 없이 음악 분석만으로 진행')

        # 4. 가사-비트 배분 패턴 분석
        beat_pattern = None
        if lyrics_data and lyrics_data.get('has_timing'):
            beat_pattern = extractor.map_lyrics_to_beats(lyrics_data, music_analysis)

        # 5. 음악 기반 응원가 가사 생성 (비트 배분 패턴 전달)
        new_lyrics = extractor.generate_cheer_lyrics(music_analysis, player_name, mood, original_lyrics, beat_pattern)
        print(f'🎤 생성된 응원가 가사:\n{new_lyrics}\n')

        # 6. Suno Upload URL 생성 (audio_path는 이미 static_audio에 있음)
        upload_filename = Path(audio_path).name
        upload_url = f"{fastapi_base_url}/static_audio/{upload_filename}"

        print(f'📤 Upload URL: {upload_url}')

        # 7. Suno Upload & Cover
        suno = SunoAPI(os.getenv('SUNOAI_API_KEY'), pending_tasks=pending_suno_tasks, task_errors=suno_task_errors)

        cover_task_id = suno.upload_and_cover(
            uploadUrl=upload_url,
            prompt=new_lyrics,
            style=mood,
            title=f"{player_name} 응원가",
            customMode=True,              # 가사 엄격 사용
            instrumental=False,           # 가사 포함
            model="V5",                   # 최신 모델

            # 발음 정확도 개선
            vocalGender="m",              # 남성 보컬 (필요시 동적 설정 가능)
            negativeTags="mumbling, unclear vocals, distortion, rapid speech",

            # 원곡 유사도 개선
            audioWeight=0.85,             # 원곡 멜로디 강하게 유지
            styleWeight=0.35,             # 새 스타일 약하게 적용
            weirdnessConstraint=0.35,     # 창의성 억제, 원곡 충실

            callBackUrl=callback_url
        )

        print(f'⏳ Suno Task ID: {cover_task_id} - 처리 대기 중...')

        # task_id와 user_id 매핑 저장
        if user_id:
            suno_task_users[cover_task_id] = user_id
            print(f"✅ Task {cover_task_id} - User {user_id} 매핑 저장")

        # 8. 완료 대기
        cover_result = await suno.wait_for_completion(task_id_cover=cover_task_id)
        print('✅ 응원가 생성 완료!')

        # 작업 완료 후 매핑 정리
        if cover_task_id in suno_task_users:
            del suno_task_users[cover_task_id]
            print(f"🗑️ Task {cover_task_id} user 매핑 정리 완료")

        music = cover_result['sunoData']

        # prompt에서 가사만 추출
        original_prompt = music[0].get("prompt", "")
        if "duration:" in original_prompt.lower():
            lyrics_only = original_prompt.split("duration:")[0].strip()
        else:
            lyrics_only = original_prompt

        return {
            "success": True,
            "source_type": "YOUTUBE_COVER",  # YouTube 커버 구분
            "is_saveable": False,             # 저장 불가
            "is_shareable": False,            # 공유 불가
            "Title": music[0]["title"],
            "Duration": music[0]["duration"],
            "Audio URL": music[0]["audioUrl"],
            "Image URL": music[0]["imageUrl"],
            "Tags": music[0].get("tags", ""),
            "Original Lyrics": original_lyrics or "(가사 없음 - 음악 분석 기반)",
            "New Lyrics": lyrics_only,
            "Music Analysis": {
                "BPM": music_analysis["bpm"],
                "Tempo": music_analysis["tempo_category"],
                "Beat Count": music_analysis["beat_count"],
                "Energy": music_analysis["avg_energy"]
            },
            "Has Original Lyrics": original_lyrics is not None,
            "streamAudioUrl": music[0]["streamAudioUrl"],
            "sourceStreamAudioUrl": music[0].get("sourceStreamAudioUrl", ""),
            "sourceImageUrl": music[0].get("sourceImageUrl", "")
        }

    except Exception as error:
        print(f'❌ YouTube Cover 오류: {error}')
        import traceback
        traceback.print_exc()
        # 에러 발생 시에도 매핑 정리
        if 'cover_task_id' in locals() and cover_task_id in suno_task_users:
            del suno_task_users[cover_task_id]
            print(f"🗑️ Task {cover_task_id} user 매핑 정리 완료 (에러)")
        raise HTTPException(status_code=500, detail=str(error))
    finally:
        # 항상 임시 파일 정리 (오류 발생 시에도)
        if audio_path and extractor:
            extractor.cleanup(audio_path)


# Static files 설정 (sample.m4a 접근용)
from fastapi.staticfiles import StaticFiles

# static_audio 디렉토리가 없으면 생성
static_audio_dir = "static_audio"
if not os.path.exists(static_audio_dir):
    os.makedirs(static_audio_dir)
    print(f"'{static_audio_dir}' 디렉토리가 생성되었습니다.")

app.mount("/static_audio", StaticFiles(directory=static_audio_dir), name="static_audio")

# 서버 실행
if __name__ == '__main__':
    uvicorn.run(app, host='0.0.0.0', port=8020)