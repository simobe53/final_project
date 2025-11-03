import pandas as pd
import oracledb
from sqlalchemy import create_engine, text

"""
CSV 백업에서 뉴스 데이터를 DB로 복원하는 스크립트
"""

# Oracle DB 연결
user = "KBO"
password = "KBO"
host = "localhost"
port = "1521"
service_name = "XEPDB1"

oracledb.init_oracle_client()
dsn = oracledb.makedsn(host, port, service_name=service_name)
engine = create_engine(f"oracle+oracledb://{user}:{password}@{dsn}")

def restore_from_csv(csv_filename=None):
    """
    CSV 파일에서 뉴스 데이터를 읽어서 DB에 저장
    """
    import os
    
    # csv_filename이 None이면 스크립트 위치 기준으로 경로 설정
    if csv_filename is None:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        csv_filename = os.path.join(script_dir, "news_backup", "kbo_news_latest.csv")
    try:
        print(f"CSV 파일 읽는 중: {csv_filename}")
        df = pd.read_csv(csv_filename, encoding='utf-8-sig')
        print(f"읽은 데이터: {len(df)}건")
        
        if len(df) == 0:
            print("복원할 데이터가 없습니다.")
            return
        
        # 데이터 저장
        success_count = 0
        error_count = 0
        
        with engine.begin() as conn:
            for idx, (_, row) in enumerate(df.iterrows(), 1):
                try:
                    merge_sql = """
                    MERGE INTO KBO_NEWS tgt
                    USING (
                        SELECT :TITLE AS TITLE, :LINK AS LINK, :IMAGE_URL AS IMAGE_URL, 
                               :CONTENT AS CONTENT, :TEAM_ID AS TEAM_ID, :TEAM_NAME AS TEAM_NAME FROM dual
                    ) src
                    ON (tgt.LINK = src.LINK)
                    WHEN MATCHED THEN
                      UPDATE SET tgt.TITLE=src.TITLE, tgt.IMAGE_URL=src.IMAGE_URL, tgt.CONTENT=src.CONTENT,
                               tgt.TEAM_ID=src.TEAM_ID, tgt.TEAM_NAME=src.TEAM_NAME
                    WHEN NOT MATCHED THEN
                      INSERT (ID, TITLE, LINK, IMAGE_URL, CONTENT, TEAM_ID, TEAM_NAME)
                      VALUES (KBO_NEWS_SEQ.NEXTVAL, src.TITLE, src.LINK, src.IMAGE_URL, src.CONTENT, src.TEAM_ID, src.TEAM_NAME)
                    """
                    conn.execute(text(merge_sql), row.to_dict())
                    success_count += 1
                    
                    if idx % 10 == 0:
                        print(f"[진행중] {idx}/{len(df)} 복원 완료")
                        
                except Exception as e:
                    error_count += 1
                    print(f"[에러] 데이터 {idx} 저장 실패: {str(e)}")
        
        print(f"\n✅ 복원 완료: 성공 {success_count}건, 실패 {error_count}건")
        
    except FileNotFoundError:
        print(f"❌ CSV 파일을 찾을 수 없습니다: {csv_filename}")
        print("\n사용 가능한 백업 파일:")
        import os
        backup_dir = "python/news_backup"
        if os.path.exists(backup_dir):
            files = [f for f in os.listdir(backup_dir) if f.endswith('.csv')]
            for f in files:
                print(f"  - {backup_dir}/{f}")
        else:
            print("  백업 디렉토리가 없습니다.")
    except Exception as e:
        print(f"❌ 복원 실패: {str(e)}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    import sys
    
    # 명령행 인자로 CSV 파일 지정 가능
    if len(sys.argv) > 1:
        csv_file = sys.argv[1]
    else:
        csv_file = None  # None이면 함수 내부에서 자동으로 경로 설정
    
    print("=" * 60)
    print("KBO 뉴스 복원 스크립트")
    print("=" * 60)
    restore_from_csv(csv_file)
    print("\n완료!")

