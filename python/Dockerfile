# Python 3.12 slim 이미지 사용
FROM python:3.12-slim
WORKDIR /app
# Oracle 클라이언트 및 Selenium용 시스템 패키지 설치
# rm -rf /var/lib/apt/lists/* : apt 캐시 삭제로 이미지 크기 최적화 (200-500MB 절약)
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    libaio1t64 \
    chromium \
    chromium-driver \
    ffmpeg \
    && rm -rf /var/lib/apt/lists/*

# 레이어 캐싱(변경 감지) 때문에 requirements.txt를 먼저 복사/실행
# requirements 파일 복사 (이 파일 변경 없으면, 아래 install 실행 안함)
COPY requirements.txt .
# Python 의존성 설치   , --no-cache-dir : 캐시를 남기지 않음 (이미지 최적화)
RUN pip install --no-cache-dir -r requirements.txt
# Python 파일 전체 복사
COPY . .
# 뉴스 크롤링 백업용 디렉토리 생성 (런타임에 CSV 저장)
RUN mkdir -p news_backup
# 포트 노출
EXPOSE 8020
# FastAPI 서버 실행  , FastAPI/Uvicorn은 이미 포그라운드 서버
CMD ["python", "FastAPI_server.py"]
