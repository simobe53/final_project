import os
import logging
from openai import OpenAI
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()

# 로깅 설정
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class NewsSummarizer:
    def __init__(self):
        """OpenAI API를 사용한 한국어 뉴스 요약 모델 초기화"""
        try:
            # OpenAI API 키 확인
            api_key = os.getenv("OPENAI_API_KEY")
            if not api_key or api_key == "your_openai_api_key_here" or api_key == "test-key-for-now":
                logger.error("OpenAI API 키가 설정되지 않았습니다.")
                self.client = None
                return
                
            self.client = OpenAI(api_key=api_key)
            logger.info("OpenAI 기반 뉴스 요약 모델 초기화 완료")
        except Exception as e:
            logger.error(f"뉴스 요약 모델 초기화 실패: {str(e)}")
            self.client = None
    
    def summarize_news(self, news_data):
        """
        뉴스 데이터를 받아서 요약을 생성합니다.
        
        Args:
            news_data (dict): 뉴스 데이터 (title, content, team_name 등)
            
        Returns:
            str: 요약된 텍스트
        """
        if not self.client:
            logger.error("OpenAI 클라이언트가 초기화되지 않았습니다.")
            raise Exception("OpenAI API를 사용할 수 없습니다. API 키를 확인해주세요.")
        
        if not news_data:
            logger.error("뉴스 데이터가 없습니다.")
            raise Exception("요약할 뉴스 데이터가 없습니다.")
            
        if not news_data.get('content'):
            logger.error("뉴스 내용이 없습니다.")
            raise Exception("요약할 뉴스 내용이 없습니다.")
        
        try:
            title = news_data.get('title', '')
            content = news_data.get('content', '')
            team_name = news_data.get('team_name', '')
            
            # 뉴스 내용이 너무 짧으면 제목만 반환
            if len(content.strip()) < 50:
                logger.warning(f"뉴스 내용이 너무 짧습니다: {len(content.strip())}자")
                return f"📰 {title}\n\n뉴스 내용이 너무 짧아 요약하기 어렵습니다."
            
            logger.info(f"뉴스 요약 시작: 제목={title}, 내용 길이={len(content)}자")
            
            # OpenAI API를 사용한 뉴스 요약 요청
            prompt = f"""다음은 KBO 리그 야구 뉴스입니다.
이 뉴스의 핵심 내용을 300자 내외로 간단명료하게 요약해주세요.
요약에는 주요 선수, 경기 결과, 중요한 내용을 포함해주세요.

제목: {title}
팀: {team_name}
내용: {content}

요약:"""

            response = self.client.chat.completions.create(
                model="gpt-3.5-turbo",
                messages=[
                    {"role": "system", "content": "당신은 KBO 리그 야구 뉴스 전문 요약가입니다. 주어진 뉴스를 정확하고 간결하게 요약해주세요."},
                    {"role": "user", "content": prompt}
                ],
                max_tokens=300,
                temperature=0.3
            )
            
            summary = response.choices[0].message.content.strip()
            logger.info(f"뉴스 요약 완료: {len(content)}자 -> {len(summary)}자 요약")
            return summary
            
        except Exception as e:
            logger.error(f"뉴스 요약 중 오류 발생: {str(e)}", exc_info=True)
            # OpenAI API 오류 시 간단한 통계 기반 요약 제공
            return self._fallback_summary(news_data)
    
    
    
    def _fallback_summary(self, news_data):
        """OpenAI API 실패 시 사용할 대체 요약 방법"""
        try:
            title = news_data.get('title', '')
            content = news_data.get('content', '')
            team_name = news_data.get('team_name', '')
            
            # 제목에서 핵심 키워드 추출
            keywords = []
            if team_name:
                keywords.append(team_name)
            
            # 내용에서 주요 키워드 추출
            important_keywords = ['승리', '패배', '홈런', '타점', '득점', '실점', '승부', '경기']
            for keyword in important_keywords:
                if keyword in content:
                    keywords.append(keyword)
            
            if keywords:
                return f"{title} - 주요 키워드: {', '.join(keywords[:3])}"
            else:
                return title
                
        except Exception as e:
            return f"뉴스 요약 처리 중 오류가 발생했습니다: {str(e)}"
    
    def _fallback_multiple_summary(self, news_list):
        """OpenAI API 실패 시 사용할 대체 종합 요약 방법"""
        try:
            team_counts = {}
            for news in news_list:
                team_name = news.get('team_name', '')
                if team_name:
                    team_counts[team_name] = team_counts.get(team_name, 0) + 1
            
            most_mentioned_team = max(team_counts.items(), key=lambda x: x[1])[0] if team_counts else ""
            
            return f"총 {len(news_list)}개의 뉴스가 있으며, {most_mentioned_team} 관련 뉴스가 {team_counts.get(most_mentioned_team, 0)}개로 가장 많이 언급되었습니다."
            
        except Exception as e:
            return f"총 {len(news_list)}개의 뉴스가 있습니다."

# 전역 인스턴스
news_summarizer = NewsSummarizer()