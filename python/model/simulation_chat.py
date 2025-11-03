import os
from openai import OpenAI
from dotenv import load_dotenv

# 환경 변수 로드
load_dotenv()

class SimulationAI:
    def __init__(self):
        """OpenAI API를 사용한 모델 초기화"""
        try:
            # OpenAI API 키 확인
            api_key = os.getenv("OPENAI_API_KEY")
            if not api_key or api_key == "your_openai_api_key_here" or api_key == "test-key-for-now":
                self.client = None
                return
                
            self.client = OpenAI(api_key=api_key)
        except Exception as e:
            self.client = None

    def check_toxic(self, messages):
        """
            메시지를 5개정도 배열로 받아서 분위기를 감지합니다.
        """
        if not self.client:
            return "OpenAI API를 사용할 수 없습니다. API 키를 확인해주세요."
        
        if not messages:
            return False
        
        try:
            str_messages = "#$" + "#$".join(messages) + "#$"
            
            # OpenAI API를 사용해 분위기 감지 요청
            prompt = f""" 
{len(messages)}개의 메시지를 #$로 감싸 연결하여 보낼테니 해당되는 메시지가 절반 이상이라면 TRUE라고 답하고,
아닐시 FALSE라고 답해주세요.

{str_messages}
"""

            response = self.client.chat.completions.create(
                model="gpt-3.5-turbo",
                messages=[
                    {"role": "system", "content": "당신은 텍스트의 어조와 사용된 단어들을 통해 기분이 나쁘게하는 말, 욕설, 혹은 비꼬는 느낌이 나는지를 감지하는 전문가입니다. "},
                    {"role": "user", "content": prompt}
                ],
                max_tokens=10,
                temperature=0.3
            )
            
            summary = response.choices[0].message.content.strip()
            return summary
        
        except Exception as e:
            print(e)
            return 
        
simulation_AI = SimulationAI()