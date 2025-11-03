#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from datetime import date
import time
import re
import oracledb
from sqlalchemy import create_engine, text

from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.chrome.service import Service
from webdriver_manager.chrome import ChromeDriverManager

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

# 크롤링 설정 (여기서 년도/월 설정)
TARGET_MONTH = None  # None으로 설정하면 모든 월

# =========================
# 핵심 함수들
# =========================

def add_columns():
    """테이블 필요한 컬럼 추가 : HIGHLIGHT_URL, HIGHLIGHT_THUMB"""
    try:
        with engine.connect() as conn:
            result = conn.execute(text(f"""
                ALTER TABLE KBO_SCHEDULE
                    ADD (HIGHLIGHT_URL VARCHAR2(500))
                    ADD (HIGHLIGHT_THUMB VARCHAR2(500))
            """))
            
            count = result.fetchall()
            print(f"테이블 컬럼 추가됨")
            return count
    except Exception as e:
        print(f"❌ DB 수정 실패: {e}")
        return
def get_highlight_games(year):
    """하이라이트가 있는 경기들을 가져옵니다"""
    try:
        with engine.connect() as conn:
            # SQL 쿼리 조건 구성 (숫자 타입으로 변환)
            if TARGET_MONTH is None:
                where_condition = "YEAR = :target_year"
                params = {'target_year': int(year)}
                print(f"🎯 크롤링 대상: {year}년 모든 월")
            else:
                where_condition = "YEAR = :target_year AND MONTH = :target_month"
                params = {'target_year': int(year), 'target_month': int(TARGET_MONTH)}
                print(f"🎯 크롤링 대상: {year}년 {TARGET_MONTH}월")
            
            # 하이라이트 페이지가 있는 모든 경기들 조회 (URL 업데이트를 위해)
            result = conn.execute(text(f"""
                SELECT 
                    GAME_DATE, GAME_TIME, AWAY_TEAM, HOME_TEAM, BOARDSE,
                    HIGHLIGHT_URL, HIGHLIGHT_THUMB
                FROM KBO_SCHEDULE 
                WHERE BOARDSE IS NOT NULL 
                AND HIGHLIGHT_URL IS NULL
                AND {where_condition}
                ORDER BY GAME_DATE, GAME_TIME
            """), params)
            
            games = result.fetchall()
            print(f"🔍 하이라이트 크롤링 대상: {len(games)}개 경기")
            return games
            
    except Exception as e:
        print(f"❌ DB 조회 실패: {e}")
        return []

def extract_youtube_video_id(embed_url):
    """YouTube embed URL에서 비디오 ID를 추출합니다"""
    try:
        pattern = r'youtube\.com/embed/([a-zA-Z0-9_-]+)'
        match = re.search(pattern, embed_url)
        return match.group(1) if match else None
    except Exception as e:
        print(f"❌ 비디오 ID 추출 실패: {e}")
        return None

def get_youtube_thumbnail_url(video_id):
    """YouTube 비디오 ID로 썸네일 URL을 생성합니다"""
    if not video_id:
        return None
    return f"https://img.youtube.com/vi/{video_id}/maxresdefault.jpg"

def crawl_highlight_data(boardse_url, game_info):
    """하이라이트 페이지에서 동영상 URL과 썸네일을 크롤링합니다"""
    try:
        game_date, away_team, home_team = game_info
        print(f"  🔍 {game_date} {away_team} vs {home_team} 하이라이트 크롤링 중...")
        
        # 페이지 로드
        driver.get(boardse_url)
        time.sleep(1)  # 3초에서 1초로 단축
        
        # YouTube iframe 찾기
        try:
            youtube_iframe = wait.until(EC.presence_of_element_located((By.XPATH, '//iframe[contains(@src, "youtube") or contains(@src, "naver")]')))
        except:
            print(f"    ⚠️ 하이라이트 동영상이 없음")
            return None, None
        
        # YouTube embed URL 추출
        embed_url = youtube_iframe.get_attribute('src')
        if not embed_url:
            print(f"    ❌ YouTube URL을 찾을 수 없음")
            return None, None
        
        print(f"    ✅ YouTube embed URL 발견: {embed_url}")
        
        # 비디오 ID 추출
        video_id = extract_youtube_video_id(embed_url)
        if not video_id:
            print(f"    ❌ 비디오 ID 추출 실패")
            return embed_url, None
        
        print(f"    ✅ 비디오 ID: {video_id}")
        
        # 썸네일 URL 생성
        thumbnail_url = get_youtube_thumbnail_url(video_id)
        if thumbnail_url:
            print(f"    ✅ 썸네일 URL: {thumbnail_url}")
        
        return embed_url, thumbnail_url
        
    except Exception as e:
        print(f"    ❌ 하이라이트 크롤링 실패: {e}")
        return None, None

def update_highlight_data(game_date, away_team, home_team, highlight_url, highlight_thumb):
    """DB에 하이라이트 데이터를 업데이트합니다"""
    try:
        with engine.connect() as conn:
            conn.execute(text("""
                UPDATE KBO_SCHEDULE 
                SET HIGHLIGHT_URL = :highlight_url,
                    HIGHLIGHT_THUMB = :highlight_thumb
                WHERE GAME_DATE = :game_date 
                AND AWAY_TEAM = :away_team 
                AND HOME_TEAM = :home_team
            """), {
                'highlight_url': highlight_url,
                'highlight_thumb': highlight_thumb,
                'game_date': game_date,
                'away_team': away_team,
                'home_team': home_team
            })
            
            conn.commit()
            return True
            
    except Exception as e:
        print(f"    ❌ DB 업데이트 실패: {e}")
        return False

def main():
    """메인 실행 함수"""
    try:
        print("🎬 하이라이트 동영상 및 썸네일 크롤링 시작")
        print("=" * 60)

        # 테이블 수정
        # add_columns()
        
        # 크롤링 대상 경기들 가져오기
        for year in list(range(2020, 2026)):
            games = get_highlight_games(year)
            
            if not games:
                print("⚠️ 크롤링할 하이라이트 페이지가 없습니다!")
                return
            
            # 각 경기에 대해 하이라이트 크롤링
            success_count = 0
            total_count = len(games)
            
            for i, game in enumerate(games, 1):
                game_date, game_time, away_team, home_team, boardse_url, existing_video, existing_thumb = game
                
                # 기존 데이터 상태 표시
                has_existing = existing_video is not None and existing_thumb is not None
                status = "🔄 업데이트" if has_existing else "🆕 신규"
                
                print(f"\n[{i}/{total_count}] {status} {game_date} {game_time} {away_team} vs {home_team}")
                
                # 하이라이트 데이터 크롤링
                highlight_url, highlight_thumb = crawl_highlight_data(boardse_url, (game_date, away_team, home_team))
                
                if highlight_url or highlight_thumb:
                    # DB 업데이트
                    if update_highlight_data(game_date, away_team, home_team, 
                                        highlight_url, highlight_thumb):
                        action = "업데이트" if has_existing else "저장"
                        print(f"    ✅ DB {action} 완료")
                        success_count += 1
                    else:
                        print(f"    ❌ DB 업데이트 실패")
                else:
                    print(f"    ⚠️ 하이라이트 데이터 없음")
                
                # 다음 요청 전 잠시 대기
                time.sleep(0.5)  # 1초에서 0.5초로 단축
            
            # 최종 결과
            print(f"\n🎯 하이라이트 크롤링 완료!")
            print(f"성공: {success_count}/{total_count}개 ({success_count/total_count*100:.1f}%)")
            
            # 최종 상황 확인
            with engine.connect() as conn:
                # SQL 쿼리 조건 구성 (숫자 타입으로 변환)
                if TARGET_MONTH is None:
                    where_condition = "YEAR = :target_year"
                    params = {'target_year': int(year)}
                else:
                    where_condition = "YEAR = :target_year AND MONTH = :target_month"
                    params = {'target_year': int(year), 'target_month': int(TARGET_MONTH)}
                
                result = conn.execute(text(f"""
                    SELECT 
                        COUNT(*) as total_games,
                        COUNT(CASE WHEN BOARDSE IS NOT NULL THEN 1 END) as with_highlight_page,
                        COUNT(CASE WHEN HIGHLIGHT_URL IS NOT NULL THEN 1 END) as with_video_url,
                        COUNT(CASE WHEN HIGHLIGHT_THUMB IS NOT NULL THEN 1 END) as with_thumbnail
                    FROM KBO_SCHEDULE
                    WHERE {where_condition}
                """), params)
                
                row = result.fetchone()
                total, with_page, with_video, with_thumb = row
                
                if TARGET_MONTH is None:
                    print(f"\n📊 {year}년 하이라이트 데이터 현황:")
                else:
                    print(f"\n📊 {year}년 {TARGET_MONTH}월 하이라이트 데이터 현황:")
                print(f"총 경기 수: {total:,}개")
                print(f"하이라이트 페이지 있음: {with_page:,}개 ({with_page/total*100:.1f}%)")
                print(f"동영상 URL 있음: {with_video:,}개 ({with_video/total*100:.1f}%)")
                print(f"썸네일 URL 있음: {with_thumb:,}개 ({with_thumb/total*100:.1f}%)")
        
    except Exception as e:
        print(f"❌ 메인 실행 실패: {e}")
        import traceback
        traceback.print_exc()
    
    finally:
        driver.quit()

def highlight_today():
    """오늘치 하이라이트 크롤링"""
    today = date.today()
    year = today.year
    try:
        
        print("🎬 하이라이트 동영상 및 썸네일 크롤링 시작")
        print("=" * 60)

        # 테이블 수정
        # add_columns()
        
        # 크롤링 대상 경기들 가져오기
        games = get_highlight_games(year)
        
        if not games:
            print("⚠️ 크롤링할 하이라이트 페이지가 없습니다!")
            return
        
        # 각 경기에 대해 하이라이트 크롤링
        success_count = 0
        total_count = len(games)
        
        for i, game in enumerate(games, 1):
            game_date, game_time, away_team, home_team, boardse_url, existing_video, existing_thumb = game
            
            # 기존 데이터 상태 표시
            has_existing = existing_video is not None and existing_thumb is not None
            status = "🔄 업데이트" if has_existing else "🆕 신규"
            
            print(f"\n[{i}/{total_count}] {status} {game_date} {game_time} {away_team} vs {home_team}")
            
            # 하이라이트 데이터 크롤링
            highlight_url, highlight_thumb = crawl_highlight_data(boardse_url, (game_date, away_team, home_team))
            
            if highlight_url or highlight_thumb:
                # DB 업데이트
                if update_highlight_data(game_date, away_team, home_team, 
                                    highlight_url, highlight_thumb):
                    action = "업데이트" if has_existing else "저장"
                    print(f"    ✅ DB {action} 완료")
                    success_count += 1
                else:
                    print(f"    ❌ DB 업데이트 실패")
            else:
                print(f"    ⚠️ 하이라이트 데이터 없음")
            
            # 다음 요청 전 잠시 대기
            time.sleep(0.5)  # 1초에서 0.5초로 단축
        
        # 최종 결과
        print(f"\n🎯 하이라이트 크롤링 완료!")
        print(f"성공: {success_count}/{total_count}개 ({success_count/total_count*100:.1f}%)")
        
    except Exception as e:
        print(f"❌ 메인 실행 실패: {e}")
        import traceback
        traceback.print_exc()
    
    finally:
        driver.quit()

if __name__ == "__main__":
    main()
