import time
import pandas as pd
import oracledb
import logging
from bs4 import BeautifulSoup
import schedule
import threading

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from sqlalchemy import create_engine, text

"""
    로그 설정
"""
logging.basicConfig(
    filename="kbo_news_crawl.log",
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)

"""
    Oracle DB 연결
"""
user = "KBO"
password = "KBO"
host = "localhost"
#host = "43.200.66.80" #배포 서버
port = "1521"
service_name = "XEPDB1"

oracledb.init_oracle_client()
dsn = oracledb.makedsn(host, port, service_name=service_name)
engine = create_engine(f"oracle+oracledb://{user}:{password}@{dsn}")

"""
    Selenium 설정 (전역 변수)
"""
driver = None
wait = None

BASE_URL = "https://www.koreabaseball.com/MediaNews/News/BreakingNews/List.aspx"

# 팀 매핑 정보 (teams 테이블의 idKey와 매핑)
TEAM_MAPPING = {
    "LG": "LG",
    "한화": "HH", 
    "두산": "OB",
    "삼성": "SS",
    "키움": "WO",
    "KIA": "HT",
    "KT": "KT",
    "NC": "NC",
    "SSG": "SK",  # SSG는 SK로 매핑
    "롯데": "LT"
}

def extract_team_from_text(title, content):
    """
    제목과 본문에서 팀명을 추출하여 teams 테이블의 idKey로 매핑
    제목에 있는 팀을 우선시하고, 여러 팀이 있으면 모두 추출하여 가장 많이 언급된 팀 반환
    """
    if not title and not content:
        return None, None
    
    # 팀명 우선순위대로 검색 (긴 이름부터)
    team_priority = [
        ("SSG 랜더스", "SK"),
        ("LG 트윈스", "LG"),
        ("한화 이글스", "HH"),
        ("두산 베어스", "OB"),
        ("삼성 라이온즈", "SS"),
        ("키움 히어로즈", "WO"),
        ("KIA 타이거즈", "HT"),
        ("KT 위즈", "KT"),
        ("NC 다이노스", "NC"),
        ("롯데 자이언츠", "LT"),
        ("SSG", "SK"),
        ("LG", "LG"),
        ("한화", "HH"),
        ("두산", "OB"),
        ("삼성", "SS"),
        ("키움", "WO"),
        ("KIA", "HT"),
        ("KT", "KT"),
        ("NC", "NC"),
        ("롯데", "LT")
    ]
    
    # 1. 제목에서 팀 찾기 (제목 우선 - 실제 출현 위치 기준)
    title = title or ""
    title_teams = {}  # team_id를 키로 사용하여 중복 제거
    for team_name, team_id in team_priority:
        if team_name in title:
            position = title.find(team_name)
            # 같은 team_id가 이미 있으면 더 앞에 나온 것만 유지
            if team_id not in title_teams or position < title_teams[team_id][0]:
                title_teams[team_id] = (position, team_name)
    
    # 제목에서 가장 먼저 나타나는 팀 반환
    if title_teams:
        # (position, team_name)에서 position으로 정렬
        sorted_teams = sorted(title_teams.items(), key=lambda x: x[1][0])
        return sorted_teams[0][0], sorted_teams[0][1][1]  # team_id, team_name 반환
    
    # 2. 본문에서 모든 팀 찾기 및 빈도수 계산
    content = content or ""
    team_counts = {}
    for team_name, team_id in team_priority:
        if team_name in content:
            count = content.count(team_name)
            # 같은 team_id의 다른 이름이 이미 있으면 더 많이 언급된 것 사용
            if team_id in team_counts:
                if count > team_counts[team_id][0]:
                    team_counts[team_id] = (count, team_name)
            else:
                team_counts[team_id] = (count, team_name)
    
    # 3. 가장 많이 언급된 팀 반환
    if team_counts:
        most_mentioned_team_id = max(team_counts.items(), key=lambda x: x[1][0])
        return most_mentioned_team_id[0], most_mentioned_team_id[1][1]
    
    return None, None


def create_table_if_not_exists():
    """
    뉴스 테이블 생성 (없으면 자동 생성)
    """
    create_table_sql = """
    BEGIN
        EXECUTE IMMEDIATE '
        CREATE TABLE KBO_NEWS (
            ID NUMBER PRIMARY KEY,
            TITLE VARCHAR2(500),
            LINK VARCHAR2(1000),
            IMAGE_URL VARCHAR2(1000),
            CONTENT CLOB,
            TEAM_ID VARCHAR2(50),
            TEAM_NAME VARCHAR2(50)
        )';
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE != -955 THEN
                RAISE;
            END IF;
    END;
    """

    with engine.begin() as conn:
        conn.execute(text(create_table_sql))


def crawl_news(pages=2):
    """
    뉴스 크롤링
    """
    global driver, wait
    
    if driver is None:
        service = Service(ChromeDriverManager().install())
        driver = webdriver.Chrome(service=service)
        wait = WebDriverWait(driver, 10)
    
    driver.get(BASE_URL)
    time.sleep(2)

    news_data = []
    seen_links = set()  # 중복 링크 방지

    for page in range(1, pages+1):
        print(f"[INFO] 페이지 {page} 크롤링 시작")
        soup = BeautifulSoup(driver.page_source, "html.parser")

        items = soup.select("ul.boardPhoto > li")
        print(f"[INFO] 페이지 {page}에서 {len(items)}개 기사 발견")
        
        for idx, item in enumerate(items, 1):
            title_tag = item.select_one("div.txt > strong > a")
            if not title_tag:
                continue

            title = title_tag.text.strip()
            link = "https://www.koreabaseball.com/MediaNews/News/BreakingNews/" + title_tag["href"]
            
            # 중복 링크 체크
            if link in seen_links:
                print(f"[SKIP] 페이지 {page}, 기사 {idx}: 중복 기사 건너뛰기")
                continue
            
            seen_links.add(link)
            img_tag = item.select_one("span.photo > a > img")
            img_url = img_tag["src"] if img_tag else None

            print(f"[PROC] 페이지 {page}, 기사 {idx}: {title[:20]}... 처리 중")
            
            # 기사 본문
            try:
                driver.execute_script("window.open(arguments[0]);", link)
                driver.switch_to.window(driver.window_handles[-1])
                time.sleep(1)
                article_soup = BeautifulSoup(driver.page_source, "html.parser")
                
                # div.detail 안의 p 태그들에서 본문 추출
                detail_divs = article_soup.select("div.detail")
                content_parts = []
                for detail_div in detail_divs:
                    paragraphs = detail_div.find_all('p')
                    for p in paragraphs:
                        text = p.text.strip()
                        if text:  # 빈 문자열이 아닌 경우만 추가
                            content_parts.append(text)
                
                content = "\n\n".join(content_parts) if content_parts else ""
                
                driver.close()
                driver.switch_to.window(driver.window_handles[0])
                print(f"[SUCCESS] 페이지 {page}, 기사 {idx}: 기사 내용 수집 완료 ({len(content)}자)")
            except Exception as e:
                print(f"[ERROR] 페이지 {page}, 기사 {idx}: 기사 내용 수집 실패 - {str(e)}")
                content = ""
                # 윈도우 정리
                if len(driver.window_handles) > 1:
                    driver.close()
                    driver.switch_to.window(driver.window_handles[0])

            # 팀 정보 추출 (제목 우선, 본문 빈도수 고려)
            team_id, team_name = extract_team_from_text(title, content)
            
            news_data.append({
                "TITLE": title,
                "LINK": link,
                "IMAGE_URL": img_url,
                "CONTENT": content,
                "TEAM_ID": team_id,
                "TEAM_NAME": team_name
            })

        # 다음 페이지 이동
        if page < pages:  # 마지막 페이지가 아니면 다음 페이지로 이동
            try:
                next_btn = driver.find_element(By.ID, "cphContents_cphContents_cphContents_ucPager_btnNext")
                next_btn.click()
                time.sleep(3)  # AJAX 로딩을 위해 대기 시간 증가
                print(f"[SUCCESS] 페이지 {page+1}로 이동 완료")
            except:
                print(f"[END] 페이지 {page+1}로 이동 실패 - 마지막 페이지 도달")
                break

    print(f"[COMPLETE] 크롤링 완료: 총 {len(news_data)}건 수집")
    return pd.DataFrame(news_data)


def save_to_oracle(df):
    """
    뉴스 데이터를 Oracle DB에 저장 (기존 데이터 삭제 후 ID 1부터 순차 저장)
    """
    create_table_if_not_exists()
    
    success_count = 0
    error_count = 0

    with engine.begin() as conn:
        # 1. 기존 데이터 모두 삭제
        try:
            conn.execute(text("DELETE FROM KBO_NEWS"))
            print(f"[DB_CLEAR] 기존 뉴스 데이터 삭제 완료")
        except Exception as e:
            print(f"[WARNING] 기존 데이터 삭제 중 오류 (테이블이 비어있을 수 있음): {str(e)}")
        
        # 2. 새로운 데이터 삽입 (ID를 1부터 순차적으로)
        for idx, (_, row) in enumerate(df.iterrows(), 1):
            try:
                insert_sql = """
                INSERT INTO KBO_NEWS (ID, TITLE, LINK, IMAGE_URL, CONTENT, TEAM_ID, TEAM_NAME)
                VALUES (:ID, :TITLE, :LINK, :IMAGE_URL, :CONTENT, :TEAM_ID, :TEAM_NAME)
                """
                data = row.to_dict()
                data['ID'] = idx  # ID를 1부터 순차적으로 할당
                conn.execute(text(insert_sql), data)
                success_count += 1
                
                if idx % 10 == 0:  # 10개마다 진행상황 출력
                    print(f"[PROGRESS] {idx}/{len(df)} 저장 완료")
                    
            except Exception as e:
                error_count += 1
                print(f"[ERROR] 데이터 {idx} 저장 실패: {str(e)}")

    print(f"[DB_COMPLETE] 저장 완료: 성공 {success_count}건, 실패 {error_count}건")


def main():
    """
    메인 실행 함수
    """
    global driver, wait
    
    try:
        df_news = crawl_news(pages=5)  # 최근 5페이지 크롤링 (더 많은 팀 뉴스 확보)
        
        if len(df_news) == 0:
            print("[WARNING] 크롤링된 데이터가 없습니다.")
        else:
            # CSV 저장 (백업용)
            try:
                import os
                from datetime import datetime
                
                # 백업 디렉토리 생성
                backup_dir = "news_backup"
                if not os.path.exists(backup_dir):
                    os.makedirs(backup_dir)
                
                # 타임스탬프 포함된 파일명
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                csv_filename = f"{backup_dir}/kbo_news_{timestamp}.csv"
                
                df_news.to_csv(csv_filename, index=False, encoding='utf-8-sig')
                print(f"[CSV] CSV 백업 저장 완료: {csv_filename}")
                
                # 최신 버전도 latest.csv로 저장
                latest_filename = f"{backup_dir}/kbo_news_latest.csv"
                df_news.to_csv(latest_filename, index=False, encoding='utf-8-sig')
                print(f"[CSV] 최신 CSV 저장 완료: {latest_filename}")
                
            except Exception as e:
                print(f"[ERROR] CSV 저장 실패: {str(e)}")
            
            # Oracle DB에 직접 저장
            try:
                save_to_oracle(df_news)
                print(f"[ORACLE] Oracle DB 저장 완료: {len(df_news)}건")
            except Exception as e:
                print(f"[ERROR] Oracle DB 저장 실패: {str(e)}")
                
    except Exception as e:
        print(f"[ERROR] 크롤링 중 오류 발생: {str(e)}")
        logging.error(f"크롤링 오류: {str(e)}")
    finally:
        if driver:
            driver.quit()
            driver = None
            wait = None
        print("[END] WebDriver 종료 완료")


# 스케줄링 설정
def run_threaded(job_func):
    """
    스레드로 작업 실행
    """
    job_thread = threading.Thread(target=job_func)
    job_thread.start()


if __name__ == "__main__":
    import sys
    
    # 즉시 실행 모드
    if len(sys.argv) > 1 and sys.argv[1] == "--now":
        print("즉시 실행 모드")
        main()
    else:
        print("KBO 뉴스 크롤러가 시작되었습니다. 매일 14:00에 실행됩니다.")
        schedule.every().day.at("14:00").do(run_threaded, main)
        
        while True:
            schedule.run_pending()
            time.sleep(1)
