# -*- coding: utf-8 -*-
"""
이미지 생성 API 클라이언트 예제
포트 8001에서 실행되는 이미지 생성 서버 사용
"""
import requests
import json
from typing import List, Dict, Any

class ImageClient:
    def __init__(self, base_url: str = "http://localhost:8020"):
        self.base_url = base_url
    
    def generate_single_image(self, korean_prompt: str, size: str = "1024x1024", save_to_file: bool = True) -> Dict[str, Any]:
        """단일 이미지 생성"""
        url = f"{self.base_url}/generate-image"
        data = {
            "korean_prompt": korean_prompt,
            "size": size,
            "save_to_file": save_to_file
        }
        
        response = requests.post(url, json=data)
        return response.json()
    
    def generate_batch_images(self, korean_prompts: List[str], size: str = "1024x1024", save_to_file: bool = True) -> Dict[str, Any]:
        """배치 이미지 생성"""
        url = f"{self.base_url}/generate-batch-images"
        data = {
            "korean_prompts": korean_prompts,
            "size": size,
            "save_to_file": save_to_file
        }
        
        response = requests.post(url, json=data)
        return response.json()
    
    def translate_prompt(self, korean_prompt: str) -> Dict[str, Any]:
        """프롬프트 번역"""
        url = f"{self.base_url}/translate-prompt"
        data = {"korean_prompt": korean_prompt}
        
        response = requests.post(url, json=data)
        return response.json()
    
    def list_images(self) -> Dict[str, Any]:
        """저장된 이미지 목록 조회"""
        url = f"{self.base_url}/images"
        response = requests.get(url)
        return response.json()
    
    def download_image(self, filename: str, save_path: str = None) -> bool:
        """이미지 다운로드"""
        url = f"{self.base_url}/download/{filename}"
        response = requests.get(url)
        
        if response.status_code == 200:
            if save_path is None:
                save_path = filename
            with open(save_path, 'wb') as f:
                f.write(response.content)
            return True
        return False

# 사용 예제
if __name__ == "__main__":
    # 클라이언트 초기화
    client = ImageClient()
    
    print("🚀 통합 AI 서비스 이미지 생성 테스트")
    print("=" * 50)
    
    # 1. 단일 이미지 생성 테스트
    print("\n1️⃣ 단일 이미지 생성 테스트")
    single_result = client.generate_single_image(
        korean_prompt="한국 야구 저지 앞면만, 핑크색 베이스에 흰 글자, 토끼 캐릭터 디테일, 플랫 일러스트레이션 스타일, 단순한 배경"
    )
    print(f"결과: {json.dumps(single_result, ensure_ascii=False, indent=2)}")
    
    # 2. 배치 이미지 생성 테스트
    print("\n2️⃣ 배치 이미지 생성 테스트")
    batch_prompts = [
        "한국 야구 유니폼 저지, 진한 빨간색과 검은색 색상, 가슴에 대각선 스트라이프, 단순한 배경, 3D 렌더링 스타일",
        "한국 야구 저지 앞면만, 핑크색 베이스에 흰 글자, 토끼 캐릭터 디테일, 플랫 일러스트레이션 스타일, 단순한 배경"
    ]
    batch_result = client.generate_batch_images(korean_prompts=batch_prompts)
    print(f"결과: {json.dumps(batch_result, ensure_ascii=False, indent=2)}")
    
    # 3. 프롬프트 번역 테스트
    print("\n3️⃣ 프롬프트 번역 테스트")
    translate_result = client.translate_prompt("한국 야구 선수 유니폼, 파란색과 흰색 조합")
    print(f"번역 결과: {json.dumps(translate_result, ensure_ascii=False, indent=2)}")
    
    # 4. 저장된 이미지 목록 조회
    print("\n4️⃣ 저장된 이미지 목록 조회")
    images_list = client.list_images()
    print(f"이미지 목록: {json.dumps(images_list, ensure_ascii=False, indent=2)}")
    
    print("\n✅ 모든 테스트 완료!")
