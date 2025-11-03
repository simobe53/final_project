#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from datetime import date
import threading
import time
import pandas as pd
import oracledb
import csv

import os
import sys
from io import StringIO

import schedule
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import Select, WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager
from sqlalchemy import create_engine, text

from highlight_crawler import highlight_today

# =========================
# 설정
# =========================

# Oracle DB 연결
user = "KBO"
password = "KBO"
host = "43.200.66.80"
port = "1521"
service_name = "XEPDB1"

# oracledb.init_oracle_client()
dsn = oracledb.makedsn(host, port, service_name=service_name)
engine = create_engine(f"oracle+oracledb://{user}:{password}@{dsn}")

# Selenium WebDriver 설정
service = Service(ChromeDriverManager().install())
driver = webdriver.Chrome(service=service)
wait = WebDriverWait(driver, 10)

# 크롤링 설정 (여기서 년도 설정)
YEARS_TO_CRAWL = list(range(2020, 2026))  # 2024년부터 2025년까지

# CSV 저장 설정
CSV_OUTPUT_DIR = "./csv_exports_schedule"

# =========================
# CSV 저장 함수들
# =========================
def ensure_csv_directory():
    """CSV 저장 디렉토리 생성"""
    if not os.path.exists(CSV_OUTPUT_DIR):
        os.makedirs(CSV_OUTPUT_DIR)
        print(f"📁 CSV 출력 디렉토리 생성: {CSV_OUTPUT_DIR}")

def save_dataframe_to_csv(df, filename, description="데이터"):
    """DataFrame을 CSV 파일로 저장"""
    try:
        ensure_csv_directory()
        filepath = os.path.join(CSV_OUTPUT_DIR, filename)
        
        # CSV로 저장 (UTF-8 BOM 포함)
        df.to_csv(filepath, index=False, encoding='utf-8-sig')
        
        print(f"📁 CSV 저장: {filename} ({len(df):,}개 레코드)")
        return True
    except Exception as e:
        print(f"❌ CSV 저장 실패: {filename}")
        return False

# =========================
# 데이터 검증 함수들
# =========================
def validate_data_types(df, year, month):
    """데이터 타입 검증 함수"""
    try:
        print(f"🔍 {year}년 {month}월 데이터 검증 중...")
        
        # 날짜 검증
        invalid_dates = df[df['GAME_DATE'].isnull()].shape[0]
        if invalid_dates > 0:
            print(f"⚠️ 잘못된 날짜: {invalid_dates}개")
        
        # 시간 검증
        invalid_times = df[df['GAME_TIME'].isnull()].shape[0]
        if invalid_times > 0:
            print(f"⚠️ 잘못된 시간: {invalid_times}개")
        
        # 점수 검증
        invalid_away_scores = df[df['AWAY_TEAM_SCORE'].isnull()].shape[0]
        invalid_home_scores = df[df['HOME_TEAM_SCORE'].isnull()].shape[0]
        if invalid_away_scores > 0 or invalid_home_scores > 0:
            print(f"⚠️ 잘못된 점수: 원정팀 {invalid_away_scores}개, 홈팀 {invalid_home_scores}개")
        
        # 연도, 월 검증
        invalid_years = df[df['YEAR'] != year].shape[0]
        invalid_months = df[df['MONTH'] != month].shape[0]
        if invalid_years > 0 or invalid_months > 0:
            print(f"⚠️ 잘못된 연도/월: 연도 {invalid_years}개, 월 {invalid_months}개")
        
        # 데이터 타입 확인
        print(f"📊 데이터 타입 확인:")
        print(f"  GAME_DATE: {df['GAME_DATE'].dtype}")
        print(f"  GAME_TIME: {df['GAME_TIME'].dtype}")
        print(f"  AWAY_TEAM_SCORE: {df['AWAY_TEAM_SCORE'].dtype}")
        print(f"  HOME_TEAM_SCORE: {df['HOME_TEAM_SCORE'].dtype}")
        print(f"  YEAR: {df['YEAR'].dtype}")
        print(f"  MONTH: {df['MONTH'].dtype}")
        
        print(f"✅ 데이터 검증 완료")
        
    except Exception as e:
        print(f"❌ 데이터 검증 오류: {e}")

# =========================
# 핵심 함수들
# =========================
def upsert_to_oracle(df, table_name, engine, key_cols):
    """Oracle DB에 데이터 저장"""
    with engine.begin() as conn:
        # 🔥 컬럼별 타입 정의 (새로운 구조)
        col_defs = []
        for col in df.columns:
            if col == 'GAME_DATE':
                col_defs.append(f"{col} DATE")
            elif col == 'GAME_TIME':
                col_defs.append(f"{col} VARCHAR2(5)")
            elif col in ['AWAY_TEAM_SCORE', 'HOME_TEAM_SCORE']:
                col_defs.append(f"{col} NUMBER(3)")
            elif col in ['YEAR', 'MONTH']:
                col_defs.append(f"{col} NUMBER(4)" if col == 'YEAR' else f"{col} NUMBER(2)")
            else:
                col_defs.append(f"{col} VARCHAR2(500)")
        
        create_sql = f"""
        BEGIN
            EXECUTE IMMEDIATE 'CREATE TABLE {table_name} (
                ID NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                {', '.join(col_defs)}
            )';
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE != -955 THEN RAISE; END IF;
        END;
        """
        conn.execute(text(create_sql))

        # MERGE를 위해 테이블 ID를 자동생성으로 변경
        alter_sql = """ALTER TABLE KBO_SCHEDULE MODIFY ID GENERATED BY DEFAULT AS IDENTITY"""
        conn.execute(text(alter_sql))

        # MERGE 수행 (NULL 값 처리 포함)
        for _, row in df.iterrows():
            # 🔥 NULL 값 처리: nan을 None으로 변환
            row_dict = row.to_dict()
            for key, value in row_dict.items():
                if pd.isna(value) or str(value) in ['nan', 'NaN']:
                    row_dict[key] = None
            
            set_cols = [c for c in df.columns if c not in key_cols]
            merge_sql = f"""
            MERGE INTO {table_name} tgt
            USING (SELECT {', '.join([f":{c} AS {c}" for c in df.columns])} FROM dual) src
            ON ({' AND '.join([f'tgt.{c}=src.{c}' for c in key_cols])})
            WHEN MATCHED THEN UPDATE SET {', '.join([f'tgt.{c}=src.{c}' for c in set_cols])}
            WHEN NOT MATCHED THEN INSERT ({', '.join(df.columns)}) VALUES ({', '.join([f'src.{c}' for c in df.columns])})
            """
            conn.execute(text(merge_sql), row_dict)

def find_highlight_column_index(table_rows):
    """하이라이트 열 인덱스 찾기"""
    if not table_rows:
        return 4  # 기본값
    
    header_row = table_rows[0]
    header_cells = header_row.find_elements(By.TAG_NAME, 'td')
    
    for i, cell in enumerate(header_cells):
        if '하이라이트' in cell.text.strip():
            return i
    
    return 4  # 기본값

def extract_highlight_url(row_element, highlight_index):
    """하이라이트 URL 추출"""
    cells = row_element.find_elements(By.TAG_NAME, 'td')
    
    if len(cells) <= highlight_index:
        return None
    
    highlight_cell = cells[highlight_index]
    links = highlight_cell.find_elements(By.TAG_NAME, 'a')
    
    for link in links:
        href = link.get_attribute('href')
        if href and 'section=HIGHLIGHT' in href:
            if href.startswith('/'):
                href = f"https://www.koreabaseball.com{href}"
            return href
    
    return None

def parse_game_result(game_str):
    """경기 결과 파싱"""
    if pd.isna(game_str) or 'vs' not in str(game_str):
        return '', '', '', '', ''
    
    try:
        parts = str(game_str).split('vs')
        if len(parts) != 2:
            return '', '', '', '', ''
        
        left_part, right_part = parts[0].strip(), parts[1].strip()
        
        # 왼쪽에서 숫자 분리
        left_team, left_score = '', ''
        for i, char in enumerate(left_part):
            if char.isdigit():
                left_team, left_score = left_part[:i], left_part[i:]
                break
        else:
            left_team = left_part
        
        # 오른쪽에서 숫자 분리
        right_score, right_team = '', ''
        for i, char in enumerate(right_part):
            if not char.isdigit():
                right_score, right_team = right_part[:i], right_part[i:]
                break
        else:
            right_score = right_part
        
        # 승리팀 결정
        winner = ''
        if left_score and right_score:
            try:
                left_int, right_int = int(left_score), int(right_score)
                if left_int > right_int:
                    winner = left_team
                elif right_int > left_int:
                    winner = right_team
                else:
                    winner = '무승부'
            except:
                pass
        
        return left_team, left_score, right_team, right_score, winner
        
    except Exception as e:
        print(f"파싱 오류: {game_str} | {e}")
        return '', '', '', '', ''

def crawl_schedule_month(year, month):
    """월별 경기일정 크롤링"""
    month_str = f"{month:02d}"
    print(f"📊 {year}년 {month}월 경기일정 수집 중...")
    
    # 월 선택
    month_dropdown = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '#ddlMonth')))
    select_month = Select(month_dropdown)
    select_month.select_by_value(month_str)
    time.sleep(1)

    valid_series = ["KBO 시범경기 일정", "KBO 정규시즌 일정", "KBO 포스트시즌 일정"]

    # 각 시리즈별로 크롤링
    for series in valid_series: 
        print(f"\n🎯 {series} 데이터 크롤링 시작")

        # 시리즈 선택
        series_dropdown = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '#ddlSeries')))
        select_series = Select(series_dropdown)
        select_series.select_by_visible_text(series)
        time.sleep(1)
        print(f"📅 {series} 선택 완료")

        try:

            # 테이블 찾기
            table_element = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '#tblScheduleList')))
            
            # 테이블 데이터 추출
            table_html = table_element.get_attribute("outerHTML")
            df_list = pd.read_html(StringIO(table_html), flavor="lxml")
            
            if not df_list or df_list[0].empty or len(df_list[0]) == 0:
                print(f"⚠️ {year}년 {month}월 {series}: 데이터 없음")
                continue
            
            df = df_list[0]
            
            # "데이터가 없습니다" 체크
            if len(df) == 1 and '데이터가 없습니다' in str(df.iloc[0]).lower():
                print(f"⚠️ {year}년 {month}월 {series}: 실제 경기 데이터 없음")
                continue
            
            # 컬럼명 정리
            col_rename_map = {
                "날짜": "GAME_DATE", "시간": "GAME_TIME", "경기": "ORIGINAL_GAME_RESULT",
                "게임센터": "TEMP_GAME_CENTER", "하이라이트": "TEMP_HIGHLIGHT",
                "TV": "TEMP_TV", "라디오": "TEMP_RADIO", "구장": "STADIUM", "비고": "REMARKS"
            }
            df = df.rename(columns=col_rename_map)
            
            # 하이라이트 URL 추출
            print(f"🔍 {year}년 {month}월 {series}: 하이라이트 URL 추출 중...")
            df['BOARDSE'] = ''
            
            table_rows = driver.find_elements(By.CSS_SELECTOR, '#tblScheduleList tr')
            highlight_index = find_highlight_column_index(table_rows)
            
            for idx, row in df.iterrows():
                actual_row_index = idx + 1
                if actual_row_index < len(table_rows):
                    row_element = table_rows[actual_row_index]
                    highlight_url = extract_highlight_url(row_element, highlight_index)
                    if highlight_url == None: highlight_url = extract_highlight_url(row_element, highlight_index - 1)
                    df.at[idx, 'BOARDSE'] = highlight_url if highlight_url else ''
            
            # 경기 결과 파싱
            parsed_data = df['ORIGINAL_GAME_RESULT'].apply(parse_game_result)
            df['AWAY_TEAM'] = [x[0] for x in parsed_data]
            df['AWAY_TEAM_SCORE'] = [x[1] for x in parsed_data]
            df['HOME_TEAM'] = [x[2] for x in parsed_data]
            df['HOME_TEAM_SCORE'] = [x[3] for x in parsed_data]
            df['VICTORY_TEAM'] = [x[4] for x in parsed_data]
            
            # 임시 컬럼 제거 및 메타데이터 추가
            df = df.drop(['ORIGINAL_GAME_RESULT', 'TEMP_GAME_CENTER', 'TEMP_HIGHLIGHT', 'TEMP_TV', 'TEMP_RADIO'], axis=1)
            df["YEAR"] = year  # 숫자로 저장
            df["MONTH"] = month  # 숫자로 저장
            df["RECORD_TYPE"] = "경기일정"
            
            # 🔥 데이터 타입 변환 로직 (새로운 구조)
            try:
                # 1. GAME_DATE: 월일 + 연도 → 년월일 (DATE 타입)
                df['GAME_DATE'] = df['GAME_DATE'].astype(str)
                
                # 날짜 변환 로직 (실제 데이터 형태에 맞게)
                def convert_to_date(date_str, year):
                    if not date_str or date_str == 'nan' or date_str == 'None':
                        return None
                    try:
                        # "05.31(일)" 형태를 "2024-05-31"로 변환
                        import re
                        
                        # 요일 제거: (일), (월), (화), (수), (목), (금), (토)
                        date_clean = re.sub(r'\([일월화수목금토]\)', '', date_str)
                        
                        # 점(.)으로 구분된 월일 추출
                        if '.' in date_clean:
                            parts = date_clean.split('.')
                            if len(parts) == 2:
                                month, day = parts
                                return f"{year}-{month.zfill(2)}-{day.zfill(2)}"
                        
                        # 하이픈(-)으로 구분된 경우도 처리
                        elif '-' in date_clean:
                            parts = date_clean.split('-')
                            if len(parts) == 2:
                                month, day = parts
                                return f"{year}-{month.zfill(2)}-{day.zfill(2)}"
                        
                        return None
                        
                    except Exception:
                        return None
                
                df['GAME_DATE'] = df['GAME_DATE'].apply(lambda x: convert_to_date(x, year))
                
                # 명시적인 날짜 형식 지정으로 경고 제거
                df['GAME_DATE'] = pd.to_datetime(df['GAME_DATE'], format='%Y-%m-%d', errors='coerce')
                
                # 2. GAME_TIME: 시분만 유지 (VARCHAR2 타입)
                df['GAME_TIME'] = df['GAME_TIME'].astype(str)
                df['GAME_TIME'] = df['GAME_TIME'].apply(lambda x: x if x and x != 'nan' and ':' in x else None)
                
                # 3. 점수 변환 (NUMBER 타입) - nan을 None으로 변환
                df['AWAY_TEAM_SCORE'] = pd.to_numeric(df['AWAY_TEAM_SCORE'], errors='coerce')
                df['HOME_TEAM_SCORE'] = pd.to_numeric(df['HOME_TEAM_SCORE'], errors='coerce')
                
                # 4. 연도, 월은 이미 숫자로 설정됨
                # df['YEAR'] = year (이미 숫자)
                # df['MONTH'] = month (이미 숫자)
                
                # 5. NULL 값 처리 (nan을 None으로 변환)
                df['GAME_DATE'] = df['GAME_DATE'].where(pd.notnull(df['GAME_DATE']), None)
                df['GAME_TIME'] = df['GAME_TIME'].where(pd.notnull(df['GAME_TIME']), None)
                df['AWAY_TEAM_SCORE'] = df['AWAY_TEAM_SCORE'].where(pd.notnull(df['AWAY_TEAM_SCORE']), None)
                df['HOME_TEAM_SCORE'] = df['HOME_TEAM_SCORE'].where(pd.notnull(df['HOME_TEAM_SCORE']), None)
                
                # 6. 추가 NULL 값 처리 (pandas의 nan을 Python의 None으로 변환)
                import numpy as np
                df['GAME_DATE'] = df['GAME_DATE'].replace({pd.NaT: None})
                df['AWAY_TEAM_SCORE'] = df['AWAY_TEAM_SCORE'].replace({np.nan: None})
                df['HOME_TEAM_SCORE'] = df['HOME_TEAM_SCORE'].replace({np.nan: None})
                
                # 7. 최종 NULL 값 검증 및 변환 (더 강력한 처리)
                for col in ['GAME_DATE', 'AWAY_TEAM_SCORE', 'HOME_TEAM_SCORE']:
                    df[col] = df[col].apply(lambda x: None if pd.isna(x) or str(x) == 'nan' or str(x) == 'NaN' else x)
                
                # 7-1. 추가 NULL 값 처리 (모든 가능한 nan 형태 처리)
                df['AWAY_TEAM_SCORE'] = df['AWAY_TEAM_SCORE'].apply(lambda x: None if pd.isna(x) or str(x) in ['nan', 'NaN', 'None'] else x)
                df['HOME_TEAM_SCORE'] = df['HOME_TEAM_SCORE'].apply(lambda x: None if pd.isna(x) or str(x) in ['nan', 'NaN', 'None'] else x)
                
                # 8. BOARDSE 특별 처리
                df['BOARDSE'] = df['BOARDSE'].astype(str).replace('nan', '').replace('', None)
                
                # 9. 나머지 컬럼들 문자열 처리 (YEAR, MONTH는 숫자로 유지)
                for col in df.columns:
                    if col not in ['GAME_DATE', 'GAME_TIME', 'AWAY_TEAM_SCORE', 'HOME_TEAM_SCORE', 'YEAR', 'MONTH', 'BOARDSE']:
                        df[col] = df[col].astype(str).replace('nan', '')
                
                # 10. 최종 데이터 타입 확인 및 디버깅
                print(f"🔍 데이터 타입 확인:")
                print(f"  GAME_DATE 샘플: {df['GAME_DATE'].iloc[0] if len(df) > 0 else 'None'} (타입: {type(df['GAME_DATE'].iloc[0]) if len(df) > 0 else 'None'})")
                print(f"  AWAY_TEAM_SCORE 샘플: {df['AWAY_TEAM_SCORE'].iloc[0] if len(df) > 0 else 'None'} (타입: {type(df['AWAY_TEAM_SCORE'].iloc[0]) if len(df) > 0 else 'None'})")
                print(f"  YEAR 샘플: {df['YEAR'].iloc[0] if len(df) > 0 else 'None'} (타입: {type(df['YEAR'].iloc[0]) if len(df) > 0 else 'None'})")
                print(f"  MONTH 샘플: {df['MONTH'].iloc[0] if len(df) > 0 else 'None'} (타입: {type(df['MONTH'].iloc[0]) if len(df) > 0 else 'None'})")
                
                print(f"✅ 데이터 타입 변환 완료: {len(df)}개 레코드")
                
                # 🔍 데이터 검증
                validate_data_types(df, year, month)
                
            except Exception as e:
                print(f"⚠️ 데이터 타입 변환 오류: {e}")
                # 오류 발생 시 기본 문자열 처리
                for col in df.columns:
                    if col == 'BOARDSE':
                        df[col] = df[col].astype(str).replace('nan', '').replace('', None)
                    else:
                        df[col] = df[col].astype(str).replace('nan', '')
            
            # 🔥 DB 저장 전 최종 NULL 값 검증 (더 직접적인 방법)
            print(f"🔍 DB 저장 전 최종 검증:")
            
            # 점수 컬럼의 모든 nan 값을 None으로 직접 변환
            df['AWAY_TEAM_SCORE'] = df['AWAY_TEAM_SCORE'].apply(lambda x: None if pd.isna(x) else x)
            df['HOME_TEAM_SCORE'] = df['HOME_TEAM_SCORE'].apply(lambda x: None if pd.isna(x) else x)
            df['GAME_DATE'] = df['GAME_DATE'].apply(lambda x: None if pd.isna(x) else x)
            
            # 추가 검증: 문자열 형태의 nan도 처리
            df['AWAY_TEAM_SCORE'] = df['AWAY_TEAM_SCORE'].apply(lambda x: None if str(x) in ['nan', 'NaN', 'None'] else x)
            df['HOME_TEAM_SCORE'] = df['HOME_TEAM_SCORE'].apply(lambda x: None if str(x) in ['nan', 'NaN', 'None'] else x)
            
            print(f"✅ NULL 값 변환 완료")
            
            # DB 저장
            upsert_to_oracle(df, "KBO_SCHEDULE", engine, 
                        ["YEAR", "MONTH", "RECORD_TYPE", "GAME_DATE", "GAME_TIME", "AWAY_TEAM", "HOME_TEAM"])
            
            print(f"✅ {year}년 {month}월 {series}: {len(df)}개 경기 저장 완료")
        except Exception as e:
            print(f"❌ 실패: {year}년 {month}월 {series} | {e}")

def main():
    """메인 실행 함수"""
    # 년도 유효성 검사
    current_year = 2024
    valid_years = [year for year in YEARS_TO_CRAWL if 2000 <= year <= current_year + 1]
    
    if not valid_years:
        print("❌ 크롤링할 유효한 년도가 없습니다.")
        sys.exit(1)
    
    print(f"📅 크롤링할 년도: {valid_years}")
    
    try:
        url = "https://www.koreabaseball.com/Schedule/Schedule.aspx"
        driver.get(url)
        time.sleep(1)
        
        # 각 년도별로 크롤링
        for year in valid_years:
            print(f"\n🎯 {year}년 데이터 크롤링 시작")
            print("=" * 50)

            # 연도 선택
            year_dropdown = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '#ddlYear')))
            select_year = Select(year_dropdown)
            select_year.select_by_value(str(year))
            time.sleep(1)
            print(f"📅 {year}년 선택 완료")
            
            # 월별 수집 (3월~11월)
            for month in range(3, 12):
                try:
                    crawl_schedule_month(year, month)
                except Exception as e:
                    print(f"⚠️ {year}년 {month}월 크롤링 실패: {e}")
                    continue
        
        print("\n=== 모든 기록 수집 완료 ===")
        
        # CSV 파일 저장
        print(f"\n📁 CSV 파일 저장 시작")
        try:
            # 전체 데이터를 CSV로 내보내기
            with engine.begin() as conn:
                # 전체 경기일정 데이터 조회
                query = """
                SELECT * FROM KBO_SCHEDULE 
                ORDER BY YEAR, MONTH, GAME_DATE, GAME_TIME
                """
                df_all = pd.read_sql(query, conn)
                
                if not df_all.empty:
                    # 전체 데이터 CSV 저장
                    save_dataframe_to_csv(df_all, "kbo_schedule_all.csv", "전체 경기일정")
                    
                    # 연도별 데이터 CSV 저장
                    for year in valid_years:
                        year_df = df_all[df_all['YEAR'] == year]
                        if not year_df.empty:
                            save_dataframe_to_csv(year_df, f"kbo_schedule_{year}.csv", f"{year}년 경기일정")
                    
                    print(f"📁 CSV 파일 저장 완료: {len(df_all):,}개 레코드")
                    print(f"📁 CSV 파일 저장 위치: {os.path.abspath(CSV_OUTPUT_DIR)}")
                else:
                    print("⚠️ 저장할 데이터가 없습니다.")
                    
        except Exception as e:
            print(f"❌ CSV 저장 실패: {e}")
        
    except Exception as e:
        print(f"❌ 전체 크롤링 실패: {e}")
    finally:
        driver.quit()

def delete_duplicated():
    try:
        with engine.connect() as conn:
            # SQL 쿼리 조건 구성 (숫자 타입으로 변환)
            
            result = conn.execute(text(f"""
                DELETE FROM KBO_SCHEDULE
                WHERE id IN (
                    SELECT id FROM (
                        SELECT id,
                            ROW_NUMBER() OVER (
                                PARTITION BY STADIUM, GAME_DATE, GAME_TIME
                                ORDER BY id DESC
                            ) AS rn
                        FROM KBO_SCHEDULE
                    )
                    WHERE rn > 1
                )
            """))
            
            conn.commit()
            print(f"{result.rowcount}개 행 삭제 완료")
    except Exception as e:
        print(f"❌ 중복 삭제 실패: {e}")
    finally:
        driver.quit()


def today():
    """오늘치 스케쥴 크롤링"""
    # 오늘 날짜
    today = date.today()
    year = today.year
    today_month = today.month
    valid_series = ["KBO 시범경기 일정", "KBO 정규시즌 일정", "KBO 포스트시즌 일정"]
    
    try:
        url = "https://www.koreabaseball.com/Schedule/Schedule.aspx"
        driver.get(url)
        time.sleep(1)
        
        # 각 시리즈별로 크롤링
        for series in valid_series: 
            print(f"\n🎯 {series} 데이터 크롤링 시작")

            # 시리즈 선택
            series_dropdown = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '#ddlSeries')))
            select_series = Select(series_dropdown)
            select_series.select_by_visible_text(series)

            # 연도 선택
            year_dropdown = wait.until(EC.presence_of_element_located((By.CSS_SELECTOR, '#ddlYear')))
            select_year = Select(year_dropdown)
            select_year.select_by_value(str(year))
            time.sleep(1)

            for month in range(today_month, 13):
                try:
                    crawl_schedule_month(year, month)
                except Exception as e:
                    print(f"⚠️ {year}년 {month}월 크롤링 실패: {e}")
                    continue
        
        print("\n=== 오늘 치 기록 DB 저장 완료 ===")

        # 저장이 완료되었다면 겹치는 데이터 제거 (결정되지 않았던 팀 데이터 제거)
        delete_duplicated()

        # 저장이 완로되었다면 하이라이트 업데이트 실행
        highlight_today()

    except Exception as e:
        print(f"❌ 전체 크롤링 실패: {e}")
    finally:
        driver.quit()

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
        print("전체 즉시 실행 모드")
        main()
    elif len(sys.argv) > 1 and sys.argv[1] == "--today":
        print("오늘치만 즉시 실행 모드")
        today()
    elif len(sys.argv) > 1 and sys.argv[1] == "--delete":
        print("중복 row 삭제 모드")
        delete_duplicated()
    else:
        print("KBO 일정/하이라이트 크롤러가 시작되었습니다. 매일 00:00에 실행됩니다.")
        schedule.every().day.at("00:00").do(run_threaded, today)
        
        while True:
            schedule.run_pending()
            time.sleep(1)
