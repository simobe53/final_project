from langchain_core.tools import tool
import requests
import pandas as pd

@tool
def get_player_data(player_name: str) -> str:
    """
    선수 이름을 받아서 DB에서 데이터를 조회하고 JSON 형태로 반환한다.
    """
    url = "http://localhost:8080/api/ai/chat/player"
    params={"playerName":player_name}
    response = requests.get(url, params=params, allow_redirects=False)
    data = response.json()

    return data

@tool
def get_team_data(team_name, csv_path="팀별_종합_2025.csv"):
    """
    CSV에서 팀 데이터 추출한 후 장단점 분석용 dict값을 반환한다.
    """
    try:
        df = pd.read_csv(csv_path, encoding="utf-8-sig")

        df['팀명'] = df['팀명'].str.strip()

        team_data = df[df["팀명"] == team_name]

        if team_data.empty:
            return f"'{team_name}' 팀 데이터를 찾을 수 없습니다."

        return team_data.to_dict(orient="records")[0]

    except FileNotFoundError:
        return f"'{csv_path}' 파일을 찾을 수 없습니다."
    except Exception as e:
        return f"데이터 처리 중 오류가 발생했습니다: {e}"

@tool
def get_place_data(place_name: str) -> list | str:
    """
    구장 이름을 받아서 맛집을 검색한다.
    """
    url = "http://localhost:8080/api/ai/chat/places"
    params={"place_name":place_name}
    response = requests.get(url, params=params, allow_redirects=False)
    print(response.status_code)
    print(response.text)

    data = response.json()

    return data


# @tool
# def get_news_summary(news_id: int) -> str:
#     """
#     뉴스 ID를 받아서 해당 뉴스를 AI로 요약하여 반환한다.
#     """

#     try:
#         connection = oracledb.connect(
#             user=os.getenv("DB_USER"),
#             password=os.getenv("DB_PASSWORD"),
#             dsn=f"{os.getenv('DB_HOST')}:{os.getenv('DB_PORT')}/{os.getenv('DB_SERVICENAME')}"
#         )
#         cursor = connection.cursor()

#         cursor.execute("""
#             SELECT TITLE, CONTENT, TEAM_NAME
#             FROM KBO_NEWS
#             WHERE ID = :news_id
#         """, {"news_id": news_id})

#         result = cursor.fetchone()

#         if not result:
#             cursor.close()
#             connection.close()
#             return f"뉴스 ID {news_id}를 찾을 수 없습니다."

#         # LOB 타입 데이터 읽기
#         title = result[0]
#         content = result[1].read() if hasattr(result[1], 'read') else result[1]
#         team_name = result[2] if result[2] else ""

#         cursor.close()
#         connection.close()

#         # 뉴스 요약 생성
#         news_data = {
#             "title": title,
#             "content": content,
#             "team_name": team_name
#         }

#         summary = news_summarizer.summarize_news(news_data)
#         return summary

#     except Exception as e:
#         print(f"  ✗ [Tool Error] 뉴스 요약 실패: {e}")
#         return f"뉴스 요약 중 오류가 발생했습니다: {e}"