import pandas as pd
import oracledb
from sqlalchemy import create_engine, text
import logging

# 로그 설정
logging.basicConfig(level=logging.INFO)

# Oracle DB 연결 설정
user = "KBO"
password = "KBO"
host = "localhost"
port = "1521"
service_name = "XEPDB1"

try:
    oracledb.init_oracle_client()
    dsn = oracledb.makedsn(host, port, service_name=service_name)
    engine = create_engine(f"oracle+oracledb://{user}:{password}@{dsn}")
    
    print("데이터베이스 연결 성공")
    
    # 팀 순위 데이터 생성 (KBO 10개 팀 전체)
    team_rank_data = [
        {"YEAR": "2025", "team_name": "LG", "rank": "1", "games_played": "144", "wins": "85", "losses": "56", "ties": "3", "win_pct": "0.603", "gb": "0", "last_10_games": "-", "streak": "-", "home_record": "-", "away_record": "-", "RECORD_TYPE": "TEAMRANK"},
        {"YEAR": "2025", "team_name": "한화", "rank": "2", "games_played": "143", "wins": "83", "losses": "57", "ties": "3", "win_pct": "0.593", "gb": "1.5", "last_10_games": "-", "streak": "-", "home_record": "-", "away_record": "-", "RECORD_TYPE": "TEAMRANK"},
        {"YEAR": "2025", "team_name": "SSG", "rank": "3", "games_played": "143", "wins": "75", "losses": "64", "ties": "4", "win_pct": "0.54", "gb": "9", "last_10_games": "-", "streak": "-", "home_record": "-", "away_record": "-", "RECORD_TYPE": "TEAMRANK"},
        {"YEAR": "2025", "team_name": "삼성", "rank": "4", "games_played": "143", "wins": "74", "losses": "67", "ties": "2", "win_pct": "0.525", "gb": "11", "last_10_games": "-", "streak": "-", "home_record": "-", "away_record": "-", "RECORD_TYPE": "TEAMRANK"},
        {"YEAR": "2025", "team_name": "NC", "rank": "5", "games_played": "143", "wins": "70", "losses": "67", "ties": "6", "win_pct": "0.511", "gb": "13", "last_10_games": "-", "streak": "-", "home_record": "-", "away_record": "-", "RECORD_TYPE": "TEAMRANK"},
        {"YEAR": "2025", "team_name": "KT", "rank": "6", "games_played": "143", "wins": "71", "losses": "68", "ties": "4", "win_pct": "0.511", "gb": "13", "last_10_games": "-", "streak": "-", "home_record": "-", "away_record": "-", "RECORD_TYPE": "TEAMRANK"},
        {"YEAR": "2025", "team_name": "롯데", "rank": "7", "games_played": "143", "wins": "68", "losses": "72", "ties": "3", "win_pct": "0.486", "gb": "16.5", "last_10_games": "-", "streak": "-", "home_record": "-", "away_record": "-", "RECORD_TYPE": "TEAMRANK"},
        {"YEAR": "2025", "team_name": "두산", "rank": "8", "games_played": "143", "wins": "65", "losses": "75", "ties": "3", "win_pct": "0.464", "gb": "19.5", "last_10_games": "-", "streak": "-", "home_record": "-", "away_record": "-", "RECORD_TYPE": "TEAMRANK"},
        {"YEAR": "2025", "team_name": "KIA", "rank": "9", "games_played": "143", "wins": "62", "losses": "78", "ties": "3", "win_pct": "0.443", "gb": "22.5", "last_10_games": "-", "streak": "-", "home_record": "-", "away_record": "-", "RECORD_TYPE": "TEAMRANK"},
        {"YEAR": "2025", "team_name": "키움", "rank": "10", "games_played": "143", "wins": "58", "losses": "82", "ties": "3", "win_pct": "0.414", "gb": "26.5", "last_10_games": "-", "streak": "-", "home_record": "-", "away_record": "-", "RECORD_TYPE": "TEAMRANK"},
    ]
    
    # DataFrame 생성
    df = pd.DataFrame(team_rank_data)
    
    # 테이블 생성 (없을 때만)
    create_table_sql = """
    BEGIN
        EXECUTE IMMEDIATE 'CREATE TABLE team_rank_2025 (
            YEAR VARCHAR2(10),
            team_name VARCHAR2(100),
            rank VARCHAR2(10),
            games_played VARCHAR2(10),
            wins VARCHAR2(10),
            losses VARCHAR2(10),
            ties VARCHAR2(10),
            win_pct VARCHAR2(10),
            gb VARCHAR2(10),
            last_10_games VARCHAR2(20),
            streak VARCHAR2(20),
            home_record VARCHAR2(20),
            away_record VARCHAR2(20),
            RECORD_TYPE VARCHAR2(50),
            CONSTRAINT pk_team_rank_2025 PRIMARY KEY (YEAR, team_name)
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
        print("테이블 생성/확인 완료")
        
        # 기존 데이터 삭제
        delete_sql = "DELETE FROM team_rank_2025 WHERE YEAR = '2025'"
        conn.execute(text(delete_sql))
        print("기존 데이터 삭제 완료")
        
        # 새 데이터 삽입
        insert_sql = """
        INSERT INTO team_rank_2025 
        (YEAR, team_name, rank, games_played, wins, losses, ties, win_pct, gb, 
         last_10_games, streak, home_record, away_record, RECORD_TYPE)
        VALUES 
        (:YEAR, :team_name, :rank, :games_played, :wins, :losses, :ties, :win_pct, :gb,
         :last_10_games, :streak, :home_record, :away_record, :RECORD_TYPE)
        """
        
        for _, row in df.iterrows():
            conn.execute(text(insert_sql), {
                'YEAR': row['YEAR'],
                'team_name': row['team_name'],
                'rank': row['rank'],
                'games_played': row['games_played'],
                'wins': row['wins'],
                'losses': row['losses'],
                'ties': row['ties'],
                'win_pct': row['win_pct'],
                'gb': row['gb'],
                'last_10_games': row['last_10_games'],
                'streak': row['streak'],
                'home_record': row['home_record'],
                'away_record': row['away_record'],
                'RECORD_TYPE': row['RECORD_TYPE']
            })
        
        print("팀 순위 데이터 삽입 완료!")
        
        # 삽입된 데이터 확인
        select_sql = "SELECT * FROM team_rank_2025 WHERE YEAR = '2025' ORDER BY CAST(rank AS NUMBER)"
        result = conn.execute(text(select_sql))
        rows = result.fetchall()
        
        print(f"총 {len(rows)}개 팀의 순위 데이터가 저장되었습니다:")
        for row in rows:
            print(f"순위 {row[2]}: {row[1]} - {row[3]}경기 {row[4]}승 {row[5]}패 {row[6]}무 (승률: {row[7]})")

except Exception as e:
    print(f"오류 발생: {e}")
    print("Oracle 데이터베이스가 실행 중인지 확인해주세요.")
    print("또는 데이터베이스 연결 정보를 확인해주세요.")
