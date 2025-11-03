#!/bin/bash

# ================================
# 🚀 통합 실행 스크립트 (안정 버전)
# ================================

VENV_PATH="$VENV_PATH"

echo "📦 Python 가상환경 경로: $VENV_PATH"

# 1️⃣ Python 가상환경 활성화
if [ -f "$VENV_PATH" ]; then
    source "$VENV_PATH"
else
    echo "❌ 가상환경이 존재하지 않습니다: $VENV_PATH"
    exit 1
fi

# 2️⃣ Oracle SQL 사용자 생성
echo "🔧 Oracle 사용자 KBO 생성 중..."
echo "exit" | sqlplus -S system/ict1234@localhost:1521/XEPDB1 @sql/create_kbo.sql > /dev/null 2>&1
echo "✅ Oracle 사용자 초기화 완료"

# ================================
# React 프론트엔드
# ================================
echo ""
echo "📦 React 의존성 확인 중..."
cd "$(dirname "$0")"

if [ ! -d "node_modules" ]; then
  echo "📥 React 의존성 설치 중..."
  npm install || { echo "❌ React 의존성 설치 실패"; exit 1; }
else
  echo "✅ React 의존성 이미 설치됨 (건너뜀)"
fi

# ================================
# Python FastAPI
# ================================
echo ""
echo "📦 Python 의존성 확인 중..."
cd python

if [ ! -d "venv" ] && [ -f "requirements.txt" ]; then
  echo "📥 Python 의존성 설치 중..."
  pip install -r requirements.txt || { echo "❌ Python 의존성 설치 실패"; exit 1; }
else
  echo "✅ Python 의존성 이미 설치됨 (건너뜀)"
fi

cd ..

# ================================
# Spring Boot 서버 실행
# ================================
PORT=8080
PID=$(lsof -ti tcp:$PORT)

if [ -z "$PID" ]; then
  echo "✅ 포트 $PORT 사용 중인 프로세스 없음"
else
  echo "⚠️ 포트 $PORT 점유 프로세스(PID: $PID) 종료 중..."
  kill -9 $PID
fi

echo ""
echo "🚀 Spring Boot 서버 시작 중... (포트: 8080)"
cd springboot
./mvnw clean compile spring-boot:run &
cd ..

echo "⏳ Spring Boot 서버 시작 대기 중..."
until lsof -i :8080 > /dev/null 2>&1; do
    sleep 3
done
echo "✅ Spring Boot 서버 시작 완료!"

# ================================
# React 개발 서버 실행
# ================================
PORT=5173
PID=$(lsof -ti tcp:$PORT)

if [ -z "$PID" ]; then
  echo "✅ 포트 $PORT 사용 중인 프로세스 없음"
else
  echo "⚠️ 포트 $PORT 점유 프로세스(PID: $PID) 종료 중..."
  kill -9 $PID
fi

echo ""
echo "🚀 React 개발 서버 시작 중... (포트: 5173)"
npm run dev &

# ================================
# FastAPI 서버 실행
# ================================
PORT=8020
PID=$(lsof -ti tcp:$PORT)

if [ -z "$PID" ]; then
  echo "✅ 포트 $PORT 사용 중인 프로세스 없음"
else
  echo "⚠️ 포트 $PORT 점유 프로세스(PID: $PID) 종료 중..."
  kill -9 $PID
fi

echo ""
echo "🚀 FastAPI 서버 시작 중... (포트: 8020)"
cd python
python FastAPI_server.py &
cd ..

# ================================
# 완료 메시지
# ================================
echo ""
echo "========================================"
echo "   ✅ 모든 서비스가 성공적으로 시작됨!"
echo "========================================"
echo ""
echo "🌐 React 프론트엔드: http://localhost:5173"
echo "🔌 Spring Boot API: http://localhost:8080"
echo "⚡ FastAPI 서버: http://localhost:8020"
echo ""
echo "📘 API 문서:"
echo "- Spring Boot: http://localhost:8080/swagger-ui.html"
echo "- FastAPI:     http://localhost:8020/docs"
echo ""
