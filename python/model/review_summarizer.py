# -*- coding: utf-8 -*-
import os
import logging
from openai import OpenAI
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class ReviewSummarizer:
    def __init__(self):
        """한국어 리뷰 요약 모델 초기화"""
        try:
            # OpenAI API 키 확인
            api_key = os.getenv("OPENAI_API_KEY")
            if not api_key or api_key == "your_openai_api_key_here" or api_key == "test-key-for-now":
                logger.error("OpenAI API 키가 설정되지 않았습니다.")
                self.client = None
                return
                
            self.client = OpenAI(api_key=api_key)
            logger.info("OpenAI 기반 리뷰 요약 모델 초기화 완료")
        except Exception as e:
            logger.error(f"리뷰 요약 모델 초기화 실패: {str(e)}")
            self.client = None
    
    def summarize_reviews(self, reviews):
        """
        리뷰 목록을 받아서 요약을 생성합니다.
        
        Args:
            reviews (list): 리뷰 텍스트 리스트
            
        Returns:
            str: 요약된 텍스트
        """
        if not self.client:
            return "리뷰 요약 모델을 사용할 수 없습니다."
        
        if not reviews or len(reviews) == 0:
            return "요약할 리뷰가 없습니다."
        
        try:
            # 리뷰들을 하나로 합치기
            combined_reviews = "\n".join([f"- {review}" for review in reviews])
            
            # 텍스트가 너무 짧으면 그대로 반환
            if len(combined_reviews) < 20:
                return combined_reviews
            
            # 요약 생성
            prompt = f"""
            이 리뷰들을 종합하여 간단하고 명확한 요약을 작성해주세요. 
            요약은 2-3문장으로 작성하고, 긍정적인 부분과 부정적인 부분을 모두 포함해주세요.

                리뷰 내용:
                {combined_reviews}

                요약:"""

            response = self.client.chat.completions.create(
                model="gpt-3.5-turbo",
                messages=[
                    {"role": "system", "content": "당신은 한국어 리뷰 요약 전문가입니다. 주어진 리뷰들을 정확하고 간결하게 요약해주세요."},
                    {"role": "user", "content": prompt}
                ],
                max_tokens=200,
                temperature=0.3
            )

            summary = response.choices[0].message.content.strip()
            
            logger.info(f"리뷰 요약 완료: {len(reviews)}개 리뷰 -> {len(summary)}자 요약")
            return summary
            
        except Exception as e:
            logger.error(f"리뷰 요약 중 오류 발생: {str(e)}")
            # OpenAI API 오류 시 간단한 통계 기반 요약 제공
            return self._fallback_summary(reviews)
    
    def _fallback_summary(self, reviews):
        """OpenAI API 실패 시 사용할 대체 요약 방법"""
        try:
            positive_keywords = ['좋', '만족', '훌륭', '최고', '재미', '깔끔', '편리']
            negative_keywords = ['불편', '부족', '나쁘', '문제', '어려움']
            
            positive_count = sum(1 for review in reviews 
                               for keyword in positive_keywords 
                               if keyword in review)
            negative_count = sum(1 for review in reviews 
                               for keyword in negative_keywords 
                               if keyword in review)
            
            if positive_count > negative_count:
                sentiment = "전반적으로 긍정적인 평가"
            elif negative_count > positive_count:
                sentiment = "일부 부정적인 평가가 있음"
            else:
                sentiment = "복합적인 평가"
            
            return f"총 {len(reviews)}개의 리뷰가 있으며, {sentiment}를 받았습니다."
            
        except Exception as e:
            return f"총 {len(reviews)}개의 리뷰가 있습니다."

# 전역 인스턴스
review_summarizer = ReviewSummarizer()