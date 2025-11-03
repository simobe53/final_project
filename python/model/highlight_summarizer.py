# -*- coding: utf-8 -*-
"""
하이라이트 영상 요약 생성 모듈
yt-dlp 자막 추출 + Whisper STT + GPT 요약
"""
import os
import subprocess
import tempfile
import time
import random
import logging
import re
from openai import OpenAI
from dotenv import load_dotenv
from urllib.parse import urlparse, parse_qs
import webvtt

# 환경 변수 로드
load_dotenv()

# 로깅 설정
logging.basicConfig(level=logging.INFO, format='%(levelname)s:%(name)s:%(message)s')
logger = logging.getLogger(__name__)

# 임시 파일 이름 템플릿
AUDIO_FILENAME = "audio_temp.mp3"
VTT_FILENAME_TEMPLATE = f"{{}}.%(ext)s" # yt-dlp 기본 템플릿


class HighlightSummarizer:
    def __init__(self):
        """하이라이트 요약 모델 초기화"""
        try:
            api_key = os.getenv("OPENAI_API_KEY")
            if not api_key or api_key == "your_openai_api_key_here":
                logger.error("OpenAI API 키가 설정되지 않았습니다.")
                self.client = None
                return
                
            self.client = OpenAI(api_key=api_key)
            logger.info("하이라이트 요약 모델 초기화 완료")
        except Exception as e:
            logger.error(f"하이라이트 요약 모델 초기화 실패: {str(e)}")
            self.client = None
    
    def get_video_id_from_url(self, url):
        """YouTube URL에서 동영상 ID를 추출합니다."""
        try:
            parsed_url = urlparse(url)
            
            if parsed_url.hostname in ('www.youtube.com', 'youtube.com'):
                # embed URL: https://www.youtube.com/embed/VIDEO_ID
                if '/embed/' in parsed_url.path:
                    return parsed_url.path.split('/')[-1].split('?')[0]
                # watch URL: https://www.youtube.com/watch?v=VIDEO_ID
                video_id = parse_qs(parsed_url.query).get('v')
                if video_id:
                    return video_id[0]
            # 단축 URL: https://youtu.be/VIDEO_ID
            elif parsed_url.hostname == 'youtu.be':
                return parsed_url.path[1:]
            
            # video_id만 전달된 경우
            if '/' not in url and '?' not in url:
                return url
            
            return None
        except Exception:
            return None
    
    def _get_random_proxy(self):
        """Webshare 프록시 풀에서 랜덤하게 프록시를 선택합니다."""
        proxy_base = os.getenv("YT_PROXY_URL")
        if not proxy_base:
            return None

        # 프록시 로테이션: -1 ~ -40 접미사 추가
        proxy_suffix = random.randint(1, 40)
        # http://username:password@host:port 형식에서 username 뒤에 접미사 추가
        proxy_with_rotation = proxy_base.replace("qdsvesvs:", f"qdsvesvs-{proxy_suffix}:")
        return proxy_with_rotation

    def _extract_and_transcribe_audio(self, video_url, temp_dir):
        """yt-dlp로 오디오를 추출하고 Whisper API로 STT 변환합니다."""

        audio_path = os.path.join(temp_dir, AUDIO_FILENAME)

        # 1. yt-dlp로 오디오 추출 (ffmpeg 필요)
        command = [
            "yt-dlp",
            "--user-agent", "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36",
            "--extractor-args", "youtube:player_client=android",
            "--geo-bypass",
            "-x", # 오디오만 추출
            "--audio-format", "mp3",
            "--output", audio_path,
            video_url
        ]

        # 프록시 설정 추가 (랜덤 로테이션)
        proxy = self._get_random_proxy()
        if proxy:
            command.extend(["--proxy", proxy])
            logger.info(f"🔒 프록시 사용: {proxy.split('@')[0]}@***")

        logger.info(f"🎤 자막 없음: 오디오 추출 (STT 전환) 시작...")

        try:
            result = subprocess.run(command, check=True, capture_output=True, text=True, timeout=240)
            logger.debug(f"yt-dlp audio extraction stdout: {result.stdout}")
        except subprocess.CalledProcessError as e:
            logger.error(f"yt-dlp audio extraction stderr: {e.stderr}")
            logger.error(f"yt-dlp audio extraction stdout: {e.stdout}")
            logger.error(f"Return code: {e.returncode}")
            raise

        if not os.path.exists(audio_path):
             raise RuntimeError("오디오 파일 추출에 실패했습니다. (ffmpeg 미설치 또는 지원하지 않는 영상)")
        
        # 2. Whisper API로 STT 변환 (오디오 파일 업로드)
        logger.info("🗣️ Whisper API를 사용하여 음성 텍스트 변환 요청 중...")
        with open(audio_path, "rb") as audio_file:
            # Whisper 모델 사용
            transcript_response = self.client.audio.transcriptions.create(
                model="whisper-1", 
                file=audio_file,
                response_format="text"
            )
            
        full_transcript = transcript_response
        logger.info("✅ STT 변환 성공.")
        return full_transcript
    
    def _parse_vtt_to_text(self, vtt_filepath):
        """VTT 자막 파일을 읽어 텍스트만 추출하고 병합합니다."""
        try:
            captions = webvtt.read(vtt_filepath)
            # 줄바꿈과 HTML 태그를 공백으로 대체
            full_text = " ".join([c.text.replace('\n', ' ').replace('<', ' ').replace('>', ' ') for c in captions])
            # 불필요한 공백 제거
            return re.sub(r'\s+', ' ', full_text).strip()
            
        except Exception as e:
            logger.error(f"VTT 파일 파싱 실패: {vtt_filepath}, 오류: {e}")
            return ""
    
    def get_transcript(self, video_url_or_id, max_retries=1):
        """
        [자막 우선, STT 차선] 로직으로 텍스트를 추출합니다.
        """
        video_id = self.get_video_id_from_url(video_url_or_id)
        if not video_id:
            raise ValueError(f"유효하지 않은 YouTube URL입니다: {video_url_or_id}")
        
        wait_time = random.uniform(3.0, 5.0) 
        logger.info(f"⏳ AWS 차단 회피를 위해 {wait_time:.1f}초 대기 후 추출 시도")
        time.sleep(wait_time)

        # 임시 디렉토리 설정
        with tempfile.TemporaryDirectory() as temp_dir:
            
            # --- 1차 시도: 자막 파일 추출 ---
            transcript_found = False
            try:
                # 자막 다운로드 명령어 (yt-dlp)
                vtt_template = os.path.join(temp_dir, f"{video_id}.%(ext)s")

                # embed URL을 watch URL로 변환
                watch_url = f"https://www.youtube.com/watch?v={video_id}"

                vtt_command = [
                    "yt-dlp",
                    "--user-agent", "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.91 Mobile Safari/537.36",
                    "--extractor-args", "youtube:player_client=android",
                    "--geo-bypass",
                    "--write-auto-subs",
                    "--sub-langs", "ko",
                    "--skip-download",
                    "--output", vtt_template,
                    watch_url
                ]

                # 프록시 설정 추가 (랜덤 로테이션)
                proxy = self._get_random_proxy()
                if proxy:
                    vtt_command.extend(["--proxy", proxy])
                    logger.info(f"🔒 프록시 사용: {proxy.split('@')[0]}@***")

                result = subprocess.run(vtt_command, check=True, capture_output=True, text=True, timeout=45)
                logger.debug(f"yt-dlp subtitle extraction stdout: {result.stdout}")
                
                # VTT 파일 찾기
                vtt_files = [f for f in os.listdir(temp_dir) if f.endswith(".ko.vtt")]
                
                if vtt_files and os.path.getsize(os.path.join(temp_dir, vtt_files[0])) > 50:
                    # 🌟 자막 파일 존재 시 바로 파싱 및 반환
                    vtt_path = os.path.join(temp_dir, vtt_files[0])
                    full_text = self._parse_vtt_to_text(vtt_path)
                    if len(full_text) > 50:
                        logger.info("✅ 자막 파일 추출 성공 (자막 우선)")
                        return full_text
                # 자막이 없거나 짧은 경우, 1차 시도는 실패로 간주하고 다음으로 넘어갑니다.
                logger.warning("⚠️ 자막 파일을 찾았으나 내용이 짧거나 파싱 실패. STT로 전환합니다.")
            
            # 🌟 CalledProcessError를 잡아 실제 오류 메시지를 출력합니다.
            except subprocess.CalledProcessError as e:
                logger.error(f"❌ 자막 추출 실패 (yt-dlp 오류)")
                logger.error(f"yt-dlp stderr: {e.stderr}")
                logger.error(f"yt-dlp stdout: {e.stdout}")
                logger.error(f"Return code: {e.returncode}")
                raise RuntimeError(f"자막 추출에 실패했습니다. 이 영상에는 자막이 없거나 접근할 수 없습니다. stderr: {e.stderr}")
            except Exception as e:
                error_msg = str(e)
                logger.error(f"❌ 자막 추출 실패 ({type(e).__name__}): {error_msg}")
                raise RuntimeError(f"자막 추출 중 오류가 발생했습니다: {error_msg}")
            
            # --- 2차 시도: STT 변환 ---
            try:
                # 🌟 자막이 없을 경우 오디오 추출 및 STT 실행
                logger.info("🎤 STT 전환: 오디오 추출 및 변환 시도...")
                return self._extract_and_transcribe_audio(video_url_or_id, temp_dir)
            
            except Exception as e:
                raise RuntimeError(f"❌ 최종 텍스트 추출 실패 (STT 오류): {str(e)}")
    
    def summarize_text_with_gpt(self, transcript_text):
        """GPT-4o Mini 모델을 사용하여 KBO 경기를 요약합니다."""
        
        if not self.client:
            return "하이라이트 요약 모델을 사용할 수 없습니다."
        
        system_prompt = (
            "당신은 KBO 리그 경기를 전문적으로 분석하는 스포츠 해설가입니다.\n\n"
            "주어진 텍스트는 YouTube 자동 생성 자막으로, 오타나 잘못된 띄어쓰기가 있을 수 있습니다. "
            " KBO 공식 팀명 및 선수명으로 교정하여 정확성을 높이세요.\n\n"
            "KBO 경기 하이라이트를 다음 형식으로 요약해 주세요:\n\n"
            "1. **경기 개요 및 승패:** (확인된 팀명, 승리/패배만 정확히 언급(예시 삼성 라이온즈 vs SSG 랜더스  🎉삼성 라이온즈 승리🎉))\n"
            "2. **주요 장면/하이라이트:** (팀명,선수명, 상황 설명)\n"
            "3. **경기 MVP 및 분석:** (자막에서 확인된 선수 활약상을 정규화하여 요약)\n\n"
            "야구 전문 용어(예: 호수비, 결승타, 병살타, 루상)를 사용하되, 정확성을 최우선으로 하세요. "
            
        )
        
        try:
            response = self.client.chat.completions.create(
                model="gpt-4o-mini",
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": f"KBO 경기 하이라이트 자막:\n\n{transcript_text}"}
                ],
                temperature=0.2,  # 더 낮은 temperature로 일관성 향상
                max_tokens=700,   # 더 많은 토큰으로 상세한 분석
                top_p=0.9        # 더 집중된 응답
            )
            return response.choices[0].message.content
        
        except Exception as e:
            raise RuntimeError(f"GPT-4o Mini API 호출 중 오류 발생: {e}")
    
    def summarize_highlight(self, video_url_or_id):
        """
        하이라이트 영상 요약 생성 (메인 메서드)
        
        Args:
            video_url_or_id: YouTube URL 또는 video_id
            
        Returns:
            dict: {
                "success": True/False,
                "summary": 요약 텍스트,
                "video_id": 동영상 ID,
                "transcript_length": 자막 길이,
                "error": 에러 메시지 (실패 시)
            }
        """
        if not video_url_or_id:
            return {
                "success": False,
                "error": "video_url 또는 video_id가 필요합니다."
            }
        
        try:
            # 1단계: 자막 추출
            transcript = self.get_transcript(video_url_or_id)
            
            if not transcript or len(transcript) < 50:
                return {
                    "success": False,
                    "error": "자막을 찾을 수 없거나 너무 짧습니다. 이 영상에는 자막이 없을 수 있습니다."
                }
            
            # 2단계: GPT 요약
            summary = self.summarize_text_with_gpt(transcript)
            
            logger.info(f"하이라이트 요약 완료: video_id={self.get_video_id_from_url(video_url_or_id)}")
            
            return {
                "success": True,
                "summary": summary,
                "transcript_length": len(transcript),
                "video_id": self.get_video_id_from_url(video_url_or_id)
            }
            
        except ValueError as e:
            return {
                "success": False,
                "error": f"URL 형식 오류: {str(e)}"
            }
        except RuntimeError as e:
            return {
                "success": False,
                "error": str(e)
            }
        except Exception as e:
            logger.error(f"요약 생성 실패: {str(e)}")
            return {
                "success": False,
                "error": f"요약 생성 중 오류 발생: {str(e)}"
            }
    
    


# 전역 인스턴스
highlight_summarizer = HighlightSummarizer()