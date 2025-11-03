# -*- coding: utf-8 -*-
"""
이미지 생성 서비스 모듈
FastAPI 서버에서 import하여 사용
"""
from fastapi import HTTPException
from fastapi.responses import FileResponse
from pydantic import BaseModel
from typing import List, Optional
import requests
from PIL import Image
from io import BytesIO
import base64
import os
from datetime import datetime
from openai import OpenAI
import re

class ImageGenerationRequest(BaseModel):
    korean_prompt: str
    size: Optional[str] = "1024x1024"
    save_to_file: Optional[bool] = True
    user_id: Optional[int] = 1  # 사용자 ID (기본값 1)
    team_name: Optional[str] = None  # 팀 이름 추가


class ImageService:
    def __init__(self, openai_client: OpenAI, save_dir: str = "generated_images"):
        self.client = openai_client
        self.save_dir = save_dir
        os.makedirs(save_dir, exist_ok=True)
        
        # 팀별 유니폼 프롬프트 정의 (프롬프트 엔지니어링 강화)
        self.team_prompts = {
            "삼성": "Official KBO league Samsung Lions baseball uniform design, 2024 season style, home game jersey. Front view of professional baseball uniform on white background, no person wearing it, just the uniform displayed flat. Team colors are royal blue and white. Classic traditional style with a majestic lion emblem prominently displayed in the center of the chest. Clean and sophisticated appearance with visible fabric texture and stitching details. High-quality product photography with studio lighting.",
            
            "KIA": "Official KBO league KIA Tigers baseball uniform design, 2024 season style, home game jersey. Front view of professional baseball uniform on white background, no person wearing it, just the uniform displayed flat. Team colors are vibrant red and black. Aggressive bold style with a powerful tiger symbol prominently displayed in the center of the chest. Dynamic and striking appearance with visible fabric texture and stitching details. High-quality product photography with studio lighting.",
            
            "한화": "Official KBO league Hanwha Eagles baseball uniform design, 2024 season style, home game jersey. Front view of professional baseball uniform on white background, no person wearing it, just the uniform displayed flat. Team colors are bright orange and black. Dynamic energetic design with an eagle logo prominently displayed in the center of the chest. Powerful and modern appearance with visible fabric texture and stitching details. High-quality product photography with studio lighting.",
            
            "두산": "Official KBO league Doosan Bears baseball uniform design, 2024 season style, home game jersey. Front view of professional baseball uniform on white background, no person wearing it, just the uniform displayed flat. Team colors are navy blue and white. Classic traditional style with a bear-shaped logo prominently displayed in the center of the chest. Sophisticated and timeless appearance with visible fabric texture and stitching details. High-quality product photography with studio lighting.",
            
            "롯데": "Official KBO league Lotte Giants baseball uniform design, 2024 season style, home game jersey. Front view of professional baseball uniform on white background, no person wearing it, just the uniform displayed flat. Team colors are navy blue and red. Refined maritime-inspired style with an anchor logo prominently displayed in the center of the chest. Elegant and professional appearance with visible fabric texture and stitching details. High-quality product photography with studio lighting.",
            
            "NC": "Official KBO league NC Dinos baseball uniform design, 2024 season style, home game jersey. Front view of professional baseball uniform on white background, no person wearing it, just the uniform displayed flat. Team colors are metallic gold and navy blue. Futuristic modern design with a dinosaur silhouette logo prominently displayed in the center of the chest. Innovative and cutting-edge appearance with visible fabric texture and stitching details. High-quality product photography with studio lighting.",
            
            "LG": "Official KBO league LG Twins baseball uniform design, 2024 season style, home game jersey. Front view of professional baseball uniform on white background, no person wearing it, just the uniform displayed flat. Team colors are hot pink and black. Modern stylish design with a bold 'T' shaped logo prominently displayed in the center of the chest. Contemporary and fashionable appearance with visible fabric texture and stitching details. High-quality product photography with studio lighting.",
            
            "SSG": "Official KBO league SSG Landers baseball uniform design, 2024 season style, home game jersey. Front view of professional baseball uniform on white background, no person wearing it, just the uniform displayed flat. Team colors are bright red and white. Energetic dynamic design with a lightning bolt logo prominently displayed in the center of the chest. Powerful and modern appearance with visible fabric texture and stitching details. High-quality product photography with studio lighting.",
            
            "키움": "Official KBO league Kiwoom Heroes baseball uniform design, 2024 season style, home game jersey. Front view of professional baseball uniform on white background, no person wearing it, just the uniform displayed flat. Team colors are burgundy wine and white. Clean noble design with a hero's shield logo prominently displayed in the center of the chest. Heroic and dignified appearance with visible fabric texture and stitching details. High-quality product photography with studio lighting.",
            
            "KT": "Official KBO league KT Wiz baseball uniform design, 2024 season style, home game jersey. Front view of professional baseball uniform on white background, no person wearing it, just the uniform displayed flat. Team colors are black and red. Contemporary tech-inspired style with a lightning pattern logo prominently displayed in the center of the chest. Modern and sleek appearance with visible fabric texture and stitching details. High-quality product photography with studio lighting."
        }
    
    def get_team_prompt(self, team_name: str, user_prompt: str) -> str:
        """팀 이름을 기반으로 전문적인 유니폼 프롬프트를 반환합니다."""
        # 팀 이름 매핑 (다양한 입력 형식 지원)
        team_mapping = {
            "삼성": ["삼성", "라이온즈", "삼성라이온즈"],
            "KIA": ["KIA", "키아", "타이거즈", "KIA타이거즈"],
            "한화": ["한화", "이글스", "한화이글스"],
            "두산": ["두산", "베어스", "두산베어스"],
            "롯데": ["롯데", "자이언츠", "롯데자이언츠"],
            "NC": ["NC", "엔씨", "다이노스", "NC다이노스"],
            "LG": ["LG", "엘지", "트윈스", "LG트윈스"],
            "SSG": ["SSG", "에스에스지", "랜더스", "SSG랜더스"],
            "키움": ["키움", "히어로즈", "키움히어로즈"],
            "KT": ["KT", "케이티", "위즈", "KT위즈"]
        }
        
        # 팀 이름 찾기
        matched_team = None
        for team_key, team_variants in team_mapping.items():
            if team_name and any(variant in team_name for variant in team_variants):
                matched_team = team_key
                break
            # user_prompt에서도 팀 이름 찾기
            if user_prompt:
                for variant in team_variants:
                    if variant in user_prompt:
                        matched_team = team_key
                        break
            if matched_team:
                break
        
        # 매칭된 팀의 프롬프트 반환
        if matched_team and matched_team in self.team_prompts:
            base_prompt = self.team_prompts[matched_team]
            # 사용자 프롬프트에 추가 요청사항이 있으면 결합
            if user_prompt and not any(keyword in user_prompt for keyword in ["유니폼", "생성", "만들어"]):
                return f"{base_prompt} Additional details: {user_prompt}"
            return base_prompt
        
        # 매칭 실패 시 사용자 프롬프트 그대로 사용
        return user_prompt
    
    def translate_to_english(self, korean_prompt: str, team_name: str = None) -> str:
        """한국어 프롬프트를 영어로 번역하거나 팀별 프롬프트를 반환합니다."""
        try:
            # 팀 이름이 제공된 경우 또는 프롬프트에서 팀 이름을 찾을 수 있는 경우
            team_prompt = self.get_team_prompt(team_name, korean_prompt)
            
            # 팀 프롬프트가 이미 영어인 경우 (매칭 성공)
            if team_prompt != korean_prompt and not any(ord(c) > 127 for c in team_prompt):
                return team_prompt
            
            # 매칭 실패 시 GPT로 번역
            messages = [
                {
                    'role': 'system',
                    'content': '당신은 이미지 생성용 프롬프트 번역 전문가입니다. 한국어로 된 이미지 생성 프롬프트를 영어로 번역해주세요. 번역할 때는 DALL-E가 이해하기 쉽도록 구체적이고 명확한 영어로 번역해주세요.'
                },
                {
                    'role': 'user',
                    'content': f'다음 한국어 프롬프트를 영어로 번역해주세요:\n\n{korean_prompt}'
                }
            ]

            response = self.client.chat.completions.create(
                model='gpt-4o',
                messages=messages,
                temperature=0.3,
                max_tokens=500
            )

            return response.choices[0].message.content.strip()
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"번역 오류: {str(e)}")

    def generate_image(self, english_prompt: str, size: str = "1024x1024") -> dict:
        """DALL-E를 사용하여 이미지를 생성합니다."""
        try:
            response = self.client.images.generate(
                model="dall-e-3",
                prompt=english_prompt,
                size=size
            )

            # 이미지 데이터 추출
            if response.data[0].url:
                # URL에서 이미지 다운로드
                image_response = requests.get(response.data[0].url)
                image = Image.open(BytesIO(image_response.content))
                image_url = response.data[0].url
            elif response.data[0].b64_json:
                # Base64 데이터 사용
                image_base64 = response.data[0].b64_json
                image = Image.open(BytesIO(base64.b64decode(image_base64)))
                image_url = None
            else:
                raise HTTPException(status_code=500, detail="이미지 데이터를 찾을 수 없습니다")

            return {
                "image": image,
                "image_url": image_url
            }
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"이미지 생성 오류: {str(e)}")

    def save_image(self, image: Image.Image, filename: str) -> str:
        """이미지를 파일로 저장합니다."""
        file_path = os.path.join(self.save_dir, filename)
        image.save(file_path)
        return file_path

    def get_next_uniform_number(self) -> int:
        """다음 유니폼 번호를 가져옵니다."""
        try:
            existing_files = [f for f in os.listdir(self.save_dir) if f.startswith("ai_uniform_") and f.endswith(".png")]
            if not existing_files:
                return 1
            
            # 파일명에서 번호 추출
            numbers = []
            for filename in existing_files:
                try:
                    # "ai_uniform_1.png" -> "1"
                    number_str = filename.replace("ai_uniform_", "").replace(".png", "")
                    if number_str.isdigit():
                        numbers.append(int(number_str))
                except:
                    continue
            
            return max(numbers) + 1 if numbers else 1
        except:
            return 1


    def generate_single_image(self, request: ImageGenerationRequest):
        """단일 이미지 생성"""
        try:
            # 한국어 프롬프트를 영어로 번역 (팀 이름 전달)
            english_prompt = self.translate_to_english(request.korean_prompt, request.team_name)

            # 이미지 생성
            result = self.generate_image(english_prompt, request.size)
            image = result["image"]
            image_url = result["image_url"]

            # 이미지를 Base64로 변환 (CORS 문제 해결용)
            buffered = BytesIO()
            image.save(buffered, format="PNG")
            image_base64 = base64.b64encode(buffered.getvalue()).decode('utf-8')
            image_size_bytes = len(buffered.getvalue())

            response_data = {
                "success": True,
                "message": "이미지가 성공적으로 생성되었습니다",
                "english_prompt": english_prompt,
                "image_url": image_url,
                "image_base64": image_base64,  # Base64 이미지 데이터 추가
                "file_size": image_size_bytes,  # 파일 크기 추가
                "korean_prompt": request.korean_prompt  # 한국어 프롬프트 추가
            }

            # 파일명 생성 (DB 저장용)
            if request.save_to_file:
                uniform_number = self.get_next_uniform_number()
                filename = f"ai_uniform_{uniform_number}.png"
                response_data["filename"] = filename

            return response_data

        except Exception as e:
            return {
                "success": False,
                "message": f"이미지 생성 실패: {str(e)}"
            }


    def translate_prompt_only(self, korean_prompt: str):
        """프롬프트 번역만 수행"""
        try:
            english_prompt = self.translate_to_english(korean_prompt)
            return {
                "success": True,
                "korean_prompt": korean_prompt,
                "english_prompt": english_prompt
            }
        except Exception as e:
            return {
                "success": False,
                "error": str(e)
            }

    def list_saved_images(self):
        """저장된 이미지 목록 조회"""
        try:
            files = []
            for filename in os.listdir(self.save_dir):
                if filename.lower().endswith(('.png', '.jpg', '.jpeg')):
                    file_path = os.path.join(self.save_dir, filename)
                    file_size = os.path.getsize(file_path)
                    file_time = os.path.getmtime(file_path)
                    files.append({
                        "filename": filename,
                        "size": file_size,
                        "created_at": datetime.fromtimestamp(file_time).isoformat()
                    })
            
            # 생성 시간 순으로 정렬 (최신순)
            files.sort(key=lambda x: x["created_at"], reverse=True)
            
            return {
                "total": len(files),
                "files": files
            }
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"파일 목록 조회 오류: {str(e)}")

    def download_image(self, filename: str):
        """저장된 이미지 다운로드"""
        file_path = os.path.join(self.save_dir, filename)
        if os.path.exists(file_path):
            return FileResponse(
                path=file_path,
                filename=filename,
                media_type="image/png"
            )
        else:
            raise HTTPException(status_code=404, detail="파일을 찾을 수 없습니다")
