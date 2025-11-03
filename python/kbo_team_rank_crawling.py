import time
from io import StringIO
import pandas as pd
import oracledb
import logging
import os
import re

import schedule
import threading

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select, WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from sqlalchemy import create_engine, text

# ===============================
# 1.로그 설정
# ===============================
logging.basicConfig(
    filename="kbo_crawl.log",
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s"
)
FAIL_LOG_FILE = "failed_tasks.txt"

# ===============================
# 2.Oracle DB 연결 설정
# ===============================
user = "KBO"
password = "KBO"
host = "localhost"
port = "1521"
service_name = "XEPDB1" 

oracledb.init_oracle_client() 
dsn = oracledb.makedsn(host, port, service_name=service_name)
engine = create_engine(f"oracle+oracledb://{user}:{password}@{dsn}")

# ===============================
# 3.Selenium WebDriver 설정
# ===============================
service = Service(ChromeDriverManager().install())
driver = webdriver.Chrome(service=service)
wait = WebDriverWait(driver, 10)

# ===============================
# 4.크롤링 대상
# ===============================
record_info = {
    "팀순위": {
        "url": "https://www.koreabaseball.com/Record/TeamRank/TeamRank.aspx",
        "years": [2025], # ✅ 2025년만 크롤링하도록 수정
    },
}

# ===============================
# 5.실패 기록 로드
# ===============================
failed_tasks = set()
if os.path.exists(FAIL_LOG_FILE):
    with open(FAIL_LOG_FILE, "r", encoding="utf-8") as f:
        for line in f:
            failed_tasks.add(line.strip())

def log_failure(record_type, year, team):
    task_str = f"{record_type}|{year}|{team}"
    failed_tasks.add(task_str)
    with open(FAIL_LOG_FILE, "a", encoding="utf-8") as f:
        f.write(task_str + "\n")

def remove_failure(record_type, year, team):
    task_str = f"{record_type}|{year}|{team}"
    if task_str in failed_tasks:
        failed_tasks.remove(task_str)
        with open(FAIL_LOG_FILE, "w", encoding="utf-8") as f:
            for t in failed_tasks:
                f.write(t + "\n")

def clean_column_names(df):
    """
    DataFrame 컬럼명 앞뒤 공백 제거 및 특수문자 제거
    """
    df.columns = (
        df.columns
        .str.strip()
        .str.replace(r"\s+", "_", regex=True)
        .str.replace(r"[^\w]", "", regex=True)
    )
    return df

def upsert_to_oracle(df, table_name, engine, key_cols):
    # 1) 한글 → 영어 컬럼 매핑 (이미 영문이면 영향 없음)
    col_map = {
        "순위": "rank",
        "팀명": "team_name",
        "경기": "games_played",
        "승": "wins",
        "패": "losses",
        "무": "ties",
        "승률": "win_pct",
        "게임차": "gb",
        "최근10경기": "last_10_games",
        "연속": "streak",
        "홈": "home_record",
        "방문": "away_record",
    }
    df = df.copy()
    df.rename(columns=col_map, inplace=True)

    # key_cols도 같은 규칙으로 영문화
    key_cols = [col_map.get(c, c) for c in key_cols]

    # 2) Oracle 저장 전 null 정리(숫자변환 X, 전부 문자열 취급)
    df = df.replace({pd.NA: None, pd.NaT: None})
    df = df.where(pd.notnull(df), None)

    with engine.begin() as conn:

        # 3) 테이블 생성(없을 때만). dtype 기반으로 타입 결정
        col_defs = []
        for col in df.columns:
            if df[col].dtype in ["int64", "int32"]:
                col_type = "NUMBER"
            elif df[col].dtype in ["float64", "float32"]:
                col_type = "NUMBER(10,3)"
            else:
                col_type = "VARCHAR2(255)"
            col_defs.append(f"{col} {col_type}")

        create_sql = f"""
        BEGIN
            EXECUTE IMMEDIATE 'CREATE TABLE {table_name} ({', '.join(col_defs)})';
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE != -955 THEN
                    RAISE;
                END IF;
        END;
        """
        conn.execute(text(create_sql))


        # 4) MERGE(UPSERT) 구성
        on_clause   = " AND ".join([f"TGT.{c} = SRC.{c}" for c in key_cols])
        set_clause  = ", ".join([f"TGT.{c} = SRC.{c}" for c in df.columns if c not in key_cols])
        insert_cols = ", ".join(df.columns)
        insert_vals = ", ".join([f"SRC.{c}" for c in df.columns])
        bind_select = ", ".join([f":{c} {c}" for c in df.columns])

        merge_sql = f"""
        MERGE INTO {table_name} TGT
        USING (SELECT {bind_select} FROM dual) SRC
        ON ({on_clause})
        WHEN MATCHED THEN
            UPDATE SET {set_clause}
        WHEN NOT MATCHED THEN
            INSERT ({insert_cols})
            VALUES ({insert_vals})
        """

        # 5) 행 단위 바인딩 실행 (모두 문자열/NULL로 전달)
        for _, row in df.iterrows():
            params = {c: (None if pd.isna(row[c]) else str(row[c])) for c in df.columns}
            conn.execute(text(merge_sql), params)

    # 6) DB 저장 완료
    print(f"[SAVED] DB 저장 완료: {table_name}")

# ===============================
# 6.팀 순위 크롤링 함수
# ===============================
def crawl_team_rank(year):
    try:
        # 연도 선택
        year_dropdown = wait.until(
            EC.presence_of_element_located((By.ID, "cphContents_cphContents_cphContents_ddlYear"))
        )
        Select(year_dropdown).select_by_visible_text(str(year))
        time.sleep(1)

        # 정규시즌 선택
        series_dropdown = wait.until(
            EC.presence_of_element_located((By.ID, "cphContents_cphContents_cphContents_ddlSeries"))
        )
        Select(series_dropdown).select_by_value("0")
        time.sleep(1)

        # 순위 테이블 추출
        table_element = wait.until(
            EC.presence_of_element_located((By.CSS_SELECTOR, "#cphContents_cphContents_cphContents_udpRecord table"))
        )
        table_html = table_element.get_attribute("outerHTML")
        df = pd.read_html(StringIO(table_html), flavor="lxml")[0]

        df["YEAR"] = year
        df["RECORD_TYPE"] = "TEAMRANK"

        # 컬럼명 정리
        df = clean_column_names(df)

        print(f"[SUCCESS] {year} 팀순위 {len(df)}행")
        return df

    except Exception as e:
        logging.error(f"팀순위 실패: {year} | {e}")
        log_failure("팀순위", year, "ALL")
        print(f"[ERROR] 팀순위 실패: {year} | {e}")
        return None

def main():
    try:
        global driver, wait
        # 웹드라이버가 없으면 새로 시작
        if not driver:
            driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()))
            wait = WebDriverWait(driver, 10)

        for record_type, info in record_info.items():
            url = info["url"]
            years = info.get("years", [None])

            driver.get(url)
            time.sleep(3)

            if record_type == "팀순위":
                for year in years:
                    df = crawl_team_rank(year)
                    if df is not None:
                        table_name = "team_rank_2025"
                        key_columns = ["YEAR", "team_name"]
                        upsert_to_oracle(df, table_name, engine, key_columns)

        print("=== 실패한 작업 재시도 시작 ===")
        retry_list = list(failed_tasks)
        for task in retry_list:
            record_type, year, team = task.split("|")
            try:
                if record_type == "팀순위":
                    df = crawl_team_rank(int(year))
                    if df is not None:
                        table_name = "team_rank_2025"
                        key_columns = ["YEAR", "team_name"]
                        upsert_to_oracle(df, table_name, engine, key_columns)
                        remove_failure(record_type, year, team)
            except Exception as e:
                logging.error(f"재시도 실패: {task} | {e}")
                continue

    except Exception as e:
        logging.error(f"메인 실행 중 오류 발생: {e}")
    finally:
        # 드라이버가 존재하면 종료
        if driver:
            driver.quit()
            driver = None
            wait = None
        print("=== 모든 기록 수집 완료 ===")

# ✅ 스케줄링 설정
def run_threaded(job_func):
    job_thread = threading.Thread(target=job_func)
    job_thread.start()



if __name__ == "__main__":
    import sys
    
    # 즉시 실행 모드
    if len(sys.argv) > 1 and sys.argv[1] == "--now":
        print("즉시 실행 모드")
        main()
    else:
        print("KBO 팀 순위 크롤러가 시작되었습니다. 매일 14:00에 실행됩니다.")
        schedule.every().day.at("15:49").do(run_threaded, main)

        while True:
            schedule.run_pending()
            time.sleep(1)