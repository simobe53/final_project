#!/bin/bash
set -e  # 에러 발생 시 스크립트 중단

# Docker 환경 최초 배포 시 한 번만 실행하는 DB 초기화 스크립트

echo "======================================"
echo "DB 초기화 시작"
echo "======================================"

# 0. 서버 시간대를 한국(KST)으로 설정
echo ""
echo "서버 시간대 설정 중..."
if sudo timedatectl set-timezone Asia/Seoul 2>/dev/null; then
    echo "✓ 서버 시간대를 Asia/Seoul(KST)로 설정 완료"
    echo "현재 시간: $(date)"
else
    echo "⚠️ 시간대 설정 권한 없음 (수동 설정 필요: sudo timedatectl set-timezone Asia/Seoul)"
fi
echo ""

# 1. Oracle 컨테이너가 완전히 시작될 때까지 대기
echo "Oracle 데이터베이스 시작 대기 중..."
RETRY=0
MAX_RETRY=60  # 5분 (60 * 5초)
until docker exec oracle-xe sqlplus -S system/ict1234@localhost:1521/XE <<< "SELECT 1 FROM DUAL;" > /dev/null 2>&1; do
  echo "Oracle 대기 중... ($RETRY/$MAX_RETRY)"
  sleep 5
  RETRY=$((RETRY + 1))
  if [ $RETRY -ge $MAX_RETRY ]; then
    echo "✗ Oracle 시작 타임아웃"
    exit 1
  fi
done
echo "✓ Oracle 준비 완료!"

# 2. KBO 사용자 생성 (기존 사용자가 있으면 삭제 후 재생성)
echo "KBO 사용자 생성 중..."
docker exec -i oracle-xe sqlplus -S system/ict1234@localhost:1521/XEPDB1 <<EOF
-- 기존 사용자 삭제 (있으면)
BEGIN
  EXECUTE IMMEDIATE 'DROP USER KBO CASCADE';
EXCEPTION
  WHEN OTHERS THEN
    IF SQLCODE != -1918 THEN  -- ORA-01918: user does not exist
      RAISE;
    END IF;
END;
/

CREATE USER KBO IDENTIFIED BY KBO;
GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO KBO;
GRANT UNLIMITED TABLESPACE TO KBO;
EXIT;
EOF

if [ $? -eq 0 ]; then
    echo "✓ KBO 사용자 생성 완료"
else
    echo "✗ KBO 사용자 생성 실패"
    exit 1
fi

# 3. Spring Boot 컨테이너가 시작될 때까지 대기
echo ""
echo "Spring Boot 서버 시작 대기 중..."
RETRY=0
MAX_RETRY=60  # 5분
until docker logs kbo-springboot 2>&1 | grep -q "Started SpringbootApplication"; do
  echo "Spring Boot 대기 중... ($RETRY/$MAX_RETRY)"
  sleep 5
  RETRY=$((RETRY + 1))
  if [ $RETRY -ge $MAX_RETRY ]; then
    echo "✗ Spring Boot 시작 타임아웃"
    exit 1
  fi
done
echo "✓ Spring Boot 준비 완료!"

# 4. FastAPI 컨테이너 준비 대기
echo ""
echo "FastAPI 서버 시작 대기 중..."
RETRY=0
MAX_RETRY=40  # 약 3분
until docker exec kbo-fastapi python -c "print('ready')" > /dev/null 2>&1; do
  echo "FastAPI 대기 중... ($RETRY/$MAX_RETRY)"
  sleep 5
  RETRY=$((RETRY + 1))
  if [ $RETRY -ge $MAX_RETRY ]; then
    echo "✗ FastAPI 시작 타임아웃"
    exit 1
  fi
done
echo "✓ FastAPI 준비 완료!"

# 5. 뉴스 데이터 복원 (FastAPI 컨테이너에서)
echo ""
echo "뉴스 데이터 복원 중..."
if docker exec kbo-fastapi python /app/restore_news_from_csv.py; then
    echo "✓ 뉴스 데이터 복원 완료"
else
    echo "! 뉴스 백업 파일 없음 (최초 실행 시 정상)"
fi

echo ""
echo "======================================"
echo "DB 초기화 완료!"
echo "======================================"
echo ""
echo "선수 데이터는 Spring Boot가 자동으로 로드합니다."
echo "애플리케이션: http://localhost"
