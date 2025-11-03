import os
from dotenv import load_dotenv
import pandas as pd
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import StrOutputParser
from langchain_core.prompts import ChatPromptTemplate
from langchain_core.runnables import RunnablePassthrough

load_dotenv()
OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")

# CSV에서 팀 정보를 읽어온다.
def load_csv_data():
    """CSV 파일 전체를 읽어 DataFrame으로 반환."""
    csv_path="team_status_2025.csv"
    try:
        df = pd.read_csv(csv_path, encoding="utf-8-sig")
        df['팀명'] = df['팀명'].str.strip()
        return df
    except Exception as e:
        print(f"CSV 파일 로드 오류: {e}")
        return None

def filter_team_data(team_name: str, df: pd.DataFrame) -> str:
    """DataFrame에서 특정 팀의 데이터를 찾아 JSON 문자열로 반환."""
    if df is None:
        return "팀 데이터 파일을 불러오는 데 실패했습니다."
    
    team_data = df[df['팀명'].str.contains(team_name, case=False, na=False)]
    
    if team_data.empty:
        return f"'{team_name}' 팀을 찾을 수 없습니다."
        
    return team_data.to_dict(orient="records")[0]

TEAM_NAME_MAP = {
    # NC
    "엔씨": "NC",
    "NC 다이노스": "NC",
    # LG
    "엘지": "LG",
    "LG 트윈스": "LG",
    # 두산
    "두산 베어스": "두산",
    # 롯데
    "롯데 자이언츠": "롯데",
    # 한화
    "한화 이글스": "한화",
    # 삼성
    "삼성 라이온즈": "삼성",
    # 키움
    "키움 히어로즈": "키움",
    # KT
    "KT 위즈": "KT",
    "케이티": "KT",
    # KIA
    "기아": "KIA",
    "KIA 타이거즈": "KIA",
    # SSG
    "쓱": "SSG",
    "SSG 랜더스": "SSG"
}

def team_answer():
    llm = ChatOpenAI(model="gpt-4.1-mini", temperature=0)

    rephrase_prompt = ChatPromptTemplate.from_template(
    """
    너는 KBO 야구 프로팀 관련 질문을 대답하는 AI야.
    - 질문이 불명확하거나 짧으면 이해하기 쉽게 재작성해줘.
    
    - 질문: {user_question}
    - 재구성된 질문:
    """)

    extract_name_prompt = ChatPromptTemplate.from_template(
    """다음 질문에서 KBO 야구팀 이름만 정확히 뽑아줘: "{rephrased_question}" """)

    rephrase_chain = rephrase_prompt | llm | StrOutputParser()
    extract_name_chain = extract_name_prompt | llm | StrOutputParser()

    team_data_prompt = ChatPromptTemplate.from_template("""
    너는 KBO 데이터를 분석하고 설명하는 전문가야.
    사용자의 질문이 특정 팀({team_name})에 대한 내용이면, 팀의 스타일과 경기 운영 성향을 중심으로 분석해.

    규칙:
    1. 반드시 [팀 전체 데이터]를 기반으로 해석하되, 수치 대신 팀의 성향과 특징을 설명해.
    2. 선수 이름은 언급하지 마.
    3. 2~3문장 이내로, 팀의 색깔이 느껴지게 작성해.
    4. 공손한 말투로 작성하되, “이 팀은 ~한 팀이다”처럼 문체 변화를 줘.

    ---
    [전체 팀 데이터]:
    {team_data}

    [사용자 질문]:
    {user_question}

    [전문가 요약 답변]:
    롯데는 공격에서 장타력보다는 꾸준한 출루와
    적시타를 통해 점수를 만들어가는 팀입니다.
    투수진은 다소 실점이 있는 편이지만, 경기 운영에서
    적극적인 주루와 수비로 균형을 맞추려는 성향이 강한 팀입니다.                            
    """)
    
    final_chain = (
        RunnablePassthrough.assign(rephrased_question=rephrase_chain)
        | RunnablePassthrough.assign(
            # LLM이 추출한 이름을 표준 이름으로 매핑
            team_name=lambda x: TEAM_NAME_MAP.get(
                extract_name_chain.invoke({"rephrased_question": x["rephrased_question"]}).strip(),
                extract_name_chain.invoke({"rephrased_question": x["rephrased_question"]}).strip()
            )
        )
        | RunnablePassthrough.assign(
            # CSV 전체를 문자열로 변환하여 team_data에 할당
            team_data=lambda _: load_csv_data().to_string()
        )
        | RunnablePassthrough.assign(
            # 팀 이름으로 출력하도록 표준화
            team_name=lambda x: TEAM_NAME_MAP.get(x["team_name"], x["team_name"])
        )
        | {
            "team_name": lambda x: x["team_name"],
            "team_data": lambda x: x["team_data"],
            "user_question": lambda x: x["user_question"]
        }
        | team_data_prompt
        | llm
        | StrOutputParser()
    )

    return final_chain