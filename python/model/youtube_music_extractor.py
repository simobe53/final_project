# -*- coding: utf-8 -*-
"""
YouTube 음악 추출 및 가사 생성 모듈
"""
import os
import yt_dlp
from pathlib import Path
from openai import OpenAI
import time
import re
import librosa
import numpy as np
import random

class YouTubeMusicExtractor:
    def __init__(self, openai_client: OpenAI):
        self.client = openai_client
        self.audio_dir = Path("static_audio")
        self.audio_dir.mkdir(exist_ok=True)

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

    def download_audio(self, youtube_url: str) -> str:
        """YouTube에서 오디오만 다운로드 (subprocess + yt-dlp CLI, 하이라이트 방식)"""
        timestamp = int(time.time())
        audio_path = str(self.audio_dir / f'youtube_{timestamp}.mp3')

        # 프록시 설정 (활성화)
        proxy = self._get_random_proxy()

        # Android User-Agent로 우회 (봇 감지 회피)
        user_agents = [
            # Android Chrome
            'Mozilla/5.0 (Linux; Android 13; SM-S908B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
            # Android Samsung Internet
            'Mozilla/5.0 (Linux; Android 13; SAMSUNG SM-S908B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/20.0 Chrome/106.0.5249.126 Mobile Safari/537.36',
            # Android Firefox
            'Mozilla/5.0 (Android 13; Mobile; rv:109.0) Gecko/120.0 Firefox/120.0',
        ]
        selected_ua = random.choice(user_agents)

        # subprocess로 yt-dlp CLI 실행 (하이라이트 방식)
        command = [
            "yt-dlp",
            "--user-agent", selected_ua,
            "--extractor-args", "youtube:player_client=android",
            "--geo-bypass",
            "-x",  # 오디오만 추출
            "--audio-format", "mp3",
            "--audio-quality", "192K",
            # 광고/인트로/아웃트로 제거 (SponsorBlock)
            "--sponsorblock-remove", "sponsor,intro,outro,selfpromo",
            "--output", audio_path,
            youtube_url
        ]

        # 프록시 추가
        if proxy:
            command.extend(["--proxy", proxy])
            # 디버깅용: 프록시 형식 확인 (비밀번호 마스킹)
            proxy_parts = proxy.split('@')
            masked_proxy = f"{proxy_parts[0].split(':')[0]}:***@{proxy_parts[1]}" if len(proxy_parts) == 2 else "invalid format"
            print(f'🔒 프록시 사용 (CLI): {masked_proxy}')
        else:
            print(f'⚠️ 프록시 없음 - YT_PROXY_URL 환경변수를 확인하세요')

        print(f'📥 YouTube 오디오 다운로드 중 (CLI 방식, 오디오 스트림만): {youtube_url}')
        print(f'🤖 User-Agent: {selected_ua[:50]}...')

        try:
            import subprocess
            result = subprocess.run(command, check=True, capture_output=True, text=True, timeout=120)
            print(f'✅ yt-dlp CLI 실행 성공')
        except subprocess.CalledProcessError as e:
            print(f'❌ yt-dlp CLI 실행 실패')
            print(f'stderr: {e.stderr}')
            print(f'stdout: {e.stdout}')
            raise Exception(f"YouTube 오디오 다운로드 실패: {e.stderr}")
        except subprocess.TimeoutExpired:
            raise Exception("YouTube 오디오 다운로드 타임아웃 (120초 초과)")

        # 오디오 파일 확인
        if os.path.exists(audio_path):
            print(f'✅ 오디오 다운로드 완료 (CLI, 순수 오디오 스트림): {audio_path}')
            return audio_path

        raise FileNotFoundError(f"오디오 파일을 찾을 수 없습니다: {audio_path}")

    def _check_ffmpeg(self) -> bool:
        """ffmpeg 설치 여부 확인"""
        import shutil
        return shutil.which('ffmpeg') is not None

    def get_video_id(self, youtube_url: str) -> str:
        """YouTube URL에서 video ID 추출"""
        patterns = [
            r'(?:v=|\/)([0-9A-Za-z_-]{11}).*',
            r'(?:embed\/)([0-9A-Za-z_-]{11})',
            r'^([0-9A-Za-z_-]{11})$'
        ]
        for pattern in patterns:
            match = re.search(pattern, youtube_url)
            if match:
                return match.group(1)
        raise ValueError("Invalid YouTube URL")

    def extract_lyrics_whisper(self, audio_path: str) -> dict:
        """Whisper API로 가사 추출 (폴백) - 타임스탬프 포함"""
        print(f'🎤 Whisper API로 가사 추출 중...')

        try:
            with open(audio_path, 'rb') as audio_file:
                # verbose_json 포맷으로 타임스탬프 정보 포함
                # language 파라미터 제거 - 영어+한국어 혼합 가사 자동 인식
                transcript = self.client.audio.transcriptions.create(
                    model="whisper-1",
                    file=audio_file,
                    response_format="verbose_json",
                    timestamp_granularities=["segment"]  # 세그먼트 레벨 타임스탬프
                )

            # 타임스탬프 포함된 세그먼트 정보 추출
            timed_lyrics = []
            full_text = []

            if hasattr(transcript, 'segments') and transcript.segments:
                for segment in transcript.segments:
                    # segment는 객체이므로 속성으로 접근
                    text = segment.text.strip()
                    timed_lyrics.append({
                        'text': text,
                        'start': segment.start,
                        'duration': segment.end - segment.start
                    })
                    full_text.append(text)

                print(f'✅ Whisper 가사 추출 완료 (타임스탬프 포함, {len(timed_lyrics)}개 세그먼트)')
                return {
                    'has_timing': True,
                    'timed_lyrics': timed_lyrics,
                    'full_text': '\n'.join(full_text),
                    'method': 'whisper_with_timing'
                }
            else:
                # 타임스탬프 없이 텍스트만 있는 경우
                print(f'✅ Whisper 가사 추출 완료 (텍스트만)')
                return {
                    'has_timing': False,
                    'full_text': transcript.text if hasattr(transcript, 'text') else str(transcript),
                    'method': 'whisper'
                }

        except Exception as e:
            print(f'⚠️ Whisper 상세 추출 실패, 기본 텍스트 모드로 재시도: {e}')
            # 폴백: 기본 텍스트 모드
            with open(audio_path, 'rb') as audio_file:
                transcript = self.client.audio.transcriptions.create(
                    model="whisper-1",
                    file=audio_file,
                    language="ko",
                    response_format="text"
                )

            print(f'✅ Whisper 가사 추출 완료 (기본 모드)')
            return {
                'has_timing': False,
                'full_text': transcript,
                'method': 'whisper'
            }

    def analyze_music(self, audio_path: str) -> dict:
        """음악 파일 분석 - BPM, 비트, 구조 등"""
        print(f'🎼 음악 분석 중... (파일: {audio_path})')

        try:
            # 파일 존재 확인
            if not os.path.exists(audio_path):
                raise FileNotFoundError(f"오디오 파일을 찾을 수 없습니다: {audio_path}")

            print(f'📂 파일 확인 완료, librosa로 로딩 중...')
            # 오디오 파일 로드 (전체 로드하되 최대 5분으로 제한)
            y, sr = librosa.load(audio_path, duration=300)  # 최대 5분 (300초)
            print(f'✅ 오디오 로드 완료: sr={sr}, duration={len(y)/sr:.1f}초')

            # 1. BPM (템포) 추출
            tempo, beats = librosa.beat.beat_track(y=y, sr=sr)
            tempo = float(tempo)

            # 2. 비트 타임스탬프
            beat_times = librosa.frames_to_time(beats, sr=sr)

            # 3. 비트 간격 (평균)
            beat_intervals = np.diff(beat_times)
            avg_beat_interval = float(np.mean(beat_intervals))

            # 4. 스펙트럴 중심 (음색 분석)
            spectral_centroids = librosa.feature.spectral_centroid(y=y, sr=sr)[0]
            avg_spectral_centroid = float(np.mean(spectral_centroids))

            # 5. 에너지 (RMS)
            rms = librosa.feature.rms(y=y)[0]
            avg_energy = float(np.mean(rms))

            # 6. 음악 구조 추정 (onset detection)
            onset_env = librosa.onset.onset_strength(y=y, sr=sr)
            onset_frames = librosa.onset.onset_detect(onset_envelope=onset_env, sr=sr)
            onset_times = librosa.frames_to_time(onset_frames, sr=sr)

            # 7. 템포 분류
            if tempo < 90:
                tempo_category = "느림 (Slow)"
            elif tempo < 120:
                tempo_category = "중간 (Medium)"
            elif tempo < 140:
                tempo_category = "빠름 (Fast)"
            else:
                tempo_category = "매우 빠름 (Very Fast)"

            result = {
                'bpm': round(tempo, 1),
                'tempo_category': tempo_category,
                'beat_count': len(beat_times),
                'avg_beat_interval': round(avg_beat_interval, 3),
                'beat_times': beat_times.tolist(),  # 모든 비트 (가사 매칭용)
                'beat_times_sample': beat_times[:20].tolist(),  # 처음 20개 (로깅용)
                'onset_count': len(onset_times),
                'onset_times': onset_times[:20].tolist(),  # 처음 20개 onset
                'avg_energy': round(avg_energy, 4),
                'spectral_brightness': round(avg_spectral_centroid, 2)
            }

            print(f'✅ 음악 분석 완료: BPM={tempo:.1f}, 템포={tempo_category}')
            return result

        except Exception as e:
            import traceback
            print(f'⚠️ 음악 분석 실패: {str(e)}')
            traceback.print_exc()
            # 기본값 반환
            return {
                'bpm': 120,
                'tempo_category': '중간 (Medium)',
                'beat_count': 0,
                'avg_beat_interval': 0.5,
                'beat_times': [],
                'onset_count': 0,
                'onset_times': [],
                'avg_energy': 0.1,
                'spectral_brightness': 2000
            }

    # NOTE: 사용되지 않음 - GPT에게 리듬 분석 요청하는 함수였으나 실제로 호출되는 곳 없음
    # def analyze_rhythm(self, lyrics_data: dict) -> dict:
    #     """원본 가사의 리듬과 구조 분석 (타이밍 정보 활용)"""
    #     print(f'🎵 가사 리듬 분석 중...')
    #
    #     original_lyrics = lyrics_data['full_text']
    #     has_timing = lyrics_data.get('has_timing', False)
    #
    #     # 타이밍 정보가 있으면 더 정확한 분석 가능
    #     timing_info = ""
    #     if has_timing and 'timed_lyrics' in lyrics_data:
    #         timing_info = "\n\n**타이밍 정보 (처음 15개 세그먼트):**\n"
    #         for i, segment in enumerate(lyrics_data['timed_lyrics'][:15]):
    #             timing_info += f"- {segment['start']:.1f}초: \"{segment['text']}\" (길이: {segment['duration']:.1f}초)\n"
    #
    #     prompt = f"""
    # 다음은 야구 응원가의 가사입니다. 이 가사의 리듬, 구조, 특징을 분석해주세요:
    #
    # **가사:**
    # {original_lyrics[:500]}{"..." if len(original_lyrics) > 500 else ""}
    # {timing_info}
    #
    # 다음 항목을 분석해주세요:
    # 1. 음절 패턴 (예: 3-3-4-4, 각 라인의 음절 수)
    # 2. 반복 구조 (후렴구, 반복되는 구절)
    # 3. 리듬감과 템포 (빠른/느린/중간, BPM 추정)
    # 4. 특징적인 표현 방식 (라임, 운율)
    # 5. 호흡 구간 (브레스 포인트)
    #
    # 분석 결과를 상세히 작성해주세요.
    # """
    #
    #     response = self.client.chat.completions.create(
    #         model="gpt-4o",
    #         messages=[
    #             {"role": "system", "content": "당신은 음악 및 가사 분석 전문가입니다. 리듬, 박자, 음절 구조를 정확히 분석합니다."},
    #             {"role": "user", "content": prompt}
    #         ],
    #         temperature=0.3
    #     )
    #
    #     analysis = response.choices[0].message.content.strip()
    #     print(f'✅ 리듬 분석 완료')
    #     return {
    #         "analysis": analysis,
    #         "original_lyrics": original_lyrics,
    #         "has_timing": has_timing
    #     }

    def map_lyrics_to_beats(self, lyrics_data: dict, music_analysis: dict) -> str:
        """가사를 비트에 맞춰 음절 배분 패턴 추출 (GPT가 이해할 수 있는 형태)"""
        if not lyrics_data or not lyrics_data.get('has_timing'):
            return None

        timed_lyrics = lyrics_data.get('timed_lyrics', [])
        beat_times = music_analysis.get('beat_times', [])

        if not timed_lyrics or not beat_times:
            return None

        print(f'🎵 비트별 가사 배분 분석 중...')

        beat_based_lyrics = []

        for segment in timed_lyrics:
            text = segment['text'].strip()
            if not text:
                continue

            start = segment['start']
            end = start + segment['duration']

            # 이 세그먼트가 몇 개 비트에 걸쳐있는지 계산
            beats_in_segment = [b for b in beat_times if start <= b < end]
            num_beats = max(1, len(beats_in_segment))  # 최소 1비트

            # 음절 수 계산
            korean_syllables = sum(1 for c in text if '가' <= c <= '힣')
            english_words = len([w for w in text.split() if any('a' <= c.lower() <= 'z' for c in w)])

            if korean_syllables > 0 and english_words > 0:
                # 한글+영어 혼합
                beat_based_lyrics.append(f'"{text}" → 한글 {korean_syllables}음절 + 영어 {english_words}단어 ({num_beats}비트)')
            elif korean_syllables > 0:
                # 한글만
                beat_based_lyrics.append(f'"{text}" → {korean_syllables}음절 ({num_beats}비트)')
            elif english_words > 0:
                # 영어만
                beat_based_lyrics.append(f'"{text}" → {english_words}단어 ({num_beats}비트)')

        if not beat_based_lyrics:
            return None

        # 처음 20개 세그먼트만 (GPT 프롬프트 길이 제한)
        pattern = "\n".join(beat_based_lyrics[:20])
        print(f'✅ 비트별 가사 배분 분석 완료 ({len(beat_based_lyrics)}개 세그먼트)')

        return pattern

    # NOTE: 라인별 음절 분석 방식은 실제 리듬과 무관하므로 사용 안 함 (줄바꿈 기준)
    # def analyze_lyrics_structure(self, lyrics: str) -> dict:
    #     """원본 가사의 라인별 음절 구조 분석"""
    #     lines = [line.strip() for line in lyrics.split('\n') if line.strip()]
    #
    #     structure = []
    #     for i, line in enumerate(lines[:30]):  # 처음 30줄만 분석
    #         # 한글 음절 수 계산
    #         korean_syllables = sum(1 for c in line if '가' <= c <= '힣')
    #         # 영어 단어 수 계산
    #         english_words = len([w for w in line.split() if any('a' <= c.lower() <= 'z' for c in w)])
    #         # 총 글자 수
    #         total_chars = len(line.replace(' ', ''))
    #
    #         structure.append({
    #             'line': line,
    #             'korean_syllables': korean_syllables,
    #             'english_words': english_words,
    #             'total_chars': total_chars
    #         })
    #
    #     return structure

    def generate_cheer_lyrics(self, music_analysis: dict, player_name: str, mood: str, original_lyrics: str = None, beat_pattern: str = None) -> str:
        """음악 분석 기반 응원가 가사 생성 (원곡 비트 배분 정밀 매칭)"""
        print(f'✍️ 새로운 응원가 가사 생성 중...')

        # 음악 분석 정보
        music_info = f"""
**음악 분석 결과:**
- BPM: {music_analysis['bpm']} ({music_analysis['tempo_category']})
- 비트 간격: 약 {music_analysis['avg_beat_interval']}초마다 비트
- 에너지 레벨: {'높음' if music_analysis['avg_energy'] > 0.15 else '중간' if music_analysis['avg_energy'] > 0.08 else '낮음'}
"""

        # 원본 가사 비트 배분 패턴
        lyrics_structure = ""
        if beat_pattern:
            lyrics_structure = f"""
**원본 가사 비트별 음절 배분 패턴:**
{beat_pattern}

⚠️ **중요**: Suno가 원곡 리듬에 맞춰 부르려면 음절 수가 비슷해야 합니다!
- 각 구절의 음절 수를 ±2음절 오차 범위 내로 유지
- 예: "11음절 (5비트)" → 새 가사도 9~13음절 (5비트 길이 유지)
- 예: "3단어 (3비트)" → 한글 6~9음절 (영어 1단어 ≈ 2-3음절)
- 비트 수가 같으면 음절 수도 비슷하게 맞춰야 Suno가 자연스럽게 부름

**전체 원본 가사 (참고용):**
{original_lyrics if original_lyrics else ''}

"""
        elif original_lyrics:
            lyrics_structure = f"""
**전체 원본 가사:**
{original_lyrics}

"""

        # NOTE: 품질 개선이 필요하면 아래 주석 해제
        # 추가 가이드 (창의성, 프로세스, 품질 기준):
        # - 영어는 직역 금지, 의미와 감정 살려 창의적 변환
        # - 후렴구는 특히 강렬하고 반복하기 쉽게 작성
        # - 각 섹션마다 차별화: Verse는 스토리텔링, Chorus는 감정 절정, Bridge는 전환
        # - 중요 구절 2-3회 반복으로 응원 효과 극대화
        # - 분위기별 표현: 빠름("불타올라", "질주"), 파워("천둥같은"), 감동("꿈을 향해")
        # - 리듬 강조: 강조 단어는 반복/느낌표 사용 ("달려라! 달려라!")

        prompt = f"""
{music_info}
{lyrics_structure}

**응원가 조건:**
선수: {player_name} | 분위기: {mood} | BPM: {music_analysis['bpm']}

**핵심 규칙:**
1. 원본 비트별 음절 수 비슷하게 유지 (위 패턴의 각 구절 음절 수 ±2 오차)
   - Suno는 원곡 비트에 가사를 맞춰 부르므로 음절 수가 중요!
   - "11음절 (5비트)" 구절 → 새 가사 9~13음절로 작성
2. 영어 → 한글 변환: 1단어 ≈ 2-3음절, 직역 금지하고 창의적 변환
   예: "Go Tigers"→"달려라 우리 팀" (단순 "가자 타이거즈" 금지)
       "Let's fight"→"승리를 향해" (단순 "싸우자" 금지)
3. 선수 이름은 후렴구 위주 사용 (매 줄 반복 금지), 호칭 혼용 ("영웅", "별", "챔피언")
4. 복합어 띄어쓰기: "홈런왕"→"홈런 왕", "화이팅"→"화이 팅"
5. 발음 쉬운 단어 우선, 받침 연속 지양: "않는"→"아니야"

**표현 가이드:**
- 야구 타자 용어: 홈런, 안타, 득점, 클러치, 타격, 장타 (투수 용어 금지: 삼진, 마운드 등)
- 감정: 열정, 환호, 감동, 전율 | 경기장: 팬들, 함성, 함께
- 드라마틱 표현: "달려라 바람처럼", "터뜨려라 홈런"
- 라임 활용: "승리/기쁨이", "빛나/피어나"

**출력 형식:**
[Verse 1]
가사...

[Chorus]
후렴구...

[Verse 2]
가사...

[Chorus]
후렴구...

[Bridge]
브릿지...

[Chorus]
후렴구...

- 섹션 태그([Verse], [Chorus], [Bridge]) 대괄호 필수
- {"원본 섹션 구조 동일하게 유지" if original_lyrics else "자연스러운 전개"}
- 번호/따옴표 사용 금지, 가사만 출력
"""

        response = self.client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": "당신은 KBO 야구 응원가 전문 작사가입니다. 원곡 리듬과 음절 패턴을 정확히 분석하여 감동적이고 중독성 있는 한글 응원가를 창작합니다. 발음이 명확하고 경기장 전체가 함께 부를 수 있는 강렬한 가사를 만드세요."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.7  # 더 창의적인 표현을 위해 높임
        )

        lyrics = response.choices[0].message.content.strip()
        print(f'✅ 응원가 가사 생성 완료')
        return lyrics

    def cleanup(self, audio_path: str):
        """임시 파일 삭제"""
        try:
            if os.path.exists(audio_path):
                os.remove(audio_path)
                print(f'🗑️ 임시 파일 삭제: {audio_path}')
        except Exception as e:
            print(f'⚠️ 파일 삭제 실패: {e}')
