# GitHub Actions Workflow 파일 상세 설명

## 프로젝트 구조

이 프로젝트는 **모노레포(Monorepo)** 구조로 Frontend, Backend, AI/ML 서비스가 하나의 저장소에 통합되어 있습니다.

```
myball/
├── .github/
│   └── workflows/
│       └── deploy.yml          # 통합 CI/CD 워크플로우
├── src/                         # React 프론트엔드 소스
├── springboot/                  # Spring Boot 백엔드
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── resources/
│               └── static/      # 프론트엔드 빌드 결과가 여기에 저장됨
├── python/                      # FastAPI AI/ML 서비스
│   ├── Dockerfile
│   ├── requirements.txt
│   └── FastAPI_server.py
├── nginx/                       # Nginx 리버스 프록시 설정
│   └── default.conf
├── docker-compose.yml           # 운영 배포용 (Docker Hub 이미지 사용)
├── docker-compose.development.yml  # 개발/빌드용 (로컬 빌드)
├── Dockerfile.nginx             # Nginx Docker 이미지
├── package.json                 # 프론트엔드 의존성
└── vite.config.js              # Vite 빌드 설정
```

---

## GitHub Actions 동작 원리

### 파일 위치와 자동 인식

GitHub은 **`.github/workflows/` 폴더의 모든 `.yml` 파일을 자동으로 GitHub Actions 워크플로우로 인식**합니다.

```
myball/
└── .github/
    └── workflows/
        └── deploy.yml  ← GitHub이 자동으로 인식!
```

### 워크플로우 트리거 (언제 실행되나?)

```yaml
on:
  push:
    branches: [ master ]  # master 브랜치에 푸시하면 자동 실행
  pull_request:
    branches: [ master ]  # master 브랜치로 PR 시 실행
  workflow_dispatch:      # GitHub UI에서 수동 실행 가능
```

### 동시 실행 방지

```yaml
concurrency:
  group: build-deploy
  cancel-in-progress: false  # 진행 중인 작업 취소 안함 (순차 대기)
```

→ 비슷한 시간에 여러 push가 발생해도 순차적으로 실행되어 안전함

---

## 전체 배포 흐름 다이어그램

```
로컬 컴퓨터          GitHub 서버          GitHub Runner        Docker Hub            EC2 서버
    │                   │                      │                     │                     │
    │ git push master   │                      │                     │                     │
    ├──────────────────>│                      │                     │                     │
    │                   │                      │                     │                     │
    │                   │ deploy.yml 감지      │                     │                     │
    │                   │ "master 푸시 감지!"  │                     │                     │
    │                   │                      │                     │                     │
    │                   │ Ubuntu Runner 할당   │                     │                     │
    │                   ├─────────────────────>│                     │                     │
    │                   │                      │                     │                     │
    │                   │                      │ 1. Checkout code    │                     │
    │                   │                      │ 2. Setup Node.js 20 │                     │
    │                   │                      │ 3. npm ci           │                     │
    │                   │                      │ 4. npm run build    │                     │
    │                   │                      │    → springboot/    │                     │
    │                   │                      │      src/main/      │                     │
    │                   │                      │      resources/     │                     │
    │                   │                      │      static/        │                     │
    │                   │                      │                     │                     │
    │                   │                      │ 5. Setup Docker     │                     │
    │                   │                      │    Buildx           │                     │
    │                   │                      │ 6. Docker Hub Login │                     │
    │                   │                      │                     │                     │
    │                   │                      │ 7. Build Spring Boot│                     │
    │                   │                      │    Docker Image     │                     │
    │                   │                      │    (Frontend 포함)  │                     │
    │                   │                      ├────────────────────>│                     │
    │                   │                      │                     │ kbo-springboot:     │
    │                   │                      │                     │     latest 저장!    │
    │                   │                      │                     │                     │
    │                   │                      │ 8. Build FastAPI    │                     │
    │                   │                      │    Docker Image     │                     │
    │                   │                      ├────────────────────>│                     │
    │                   │                      │                     │ kbo-fastapi:        │
    │                   │                      │                     │     latest 저장!    │
    │                   │                      │                     │                     │
    │                   │                      │ 9. SSH to EC2       │                     │
    │                   │                      ├──────────────────────────────────────────>│
    │                   │                      │                     │                     │
    │                   │                      │                     │ docker-compose pull │
    │                   │                      │                     │<────────────────────┤
    │                   │                      │                     │  최신 이미지 다운   │
    │                   │                      │                     ├────────────────────>│
    │                   │                      │                     │                     │
    │                   │                      │                     │ docker-compose up -d│
    │                   │                      │                     │ 컨테이너 재시작     │
    │                   │                      │                     │                     │
    │                   │                      │<──────────────────────────────────────────┤
    │                   │                      │  배포 완료!          │                     │
    │                   │<─────────────────────┤                     │                     │
    │                   │                      │                     │                     │
    │ 이메일/알림 받음   │                      │                     │                     │
    │<──────────────────┤                      │                     │                     │
    │                   │                      │                     │                ✅ 서비스 운영 중!
    │                   │                      │                     │                  - Oracle DB
    │                   │                      │                     │                  - Spring Boot
    │                   │                      │                     │                  - FastAPI
    │                   │                      │                     │                  - Nginx
```

---

## deploy.yml 단계별 상세 설명

### Step 1: 코드 체크아웃

```yaml
- name: Checkout code
  uses: actions/checkout@v4
  with:
    submodules: true
```

→ GitHub 레포의 모든 코드를 GitHub Runner에 복사
→ 서브모듈이 있다면 함께 체크아웃

### Step 2: Node.js 환경 설정

```yaml
- name: Setup Node.js
  uses: actions/setup-node@v4
  with:
    node-version: '20'
    cache: 'npm'
```

→ Node.js 20 버전 설치
→ npm 캐시 활성화로 의존성 설치 속도 향상

### Step 3: 의존성 설치

```yaml
- name: Install dependencies
  run: npm ci
```

→ `npm install`보다 빠르고 안전한 `npm ci` 사용
→ `package-lock.json` 기반으로 정확한 버전 설치

### Step 4: 프론트엔드 빌드 ⭐

```yaml
- name: Build frontend
  run: npm run build
  env:
    NODE_ENV: production
```

→ Vite로 프론트엔드 빌드 실행
→ **빌드 결과물 위치**: `springboot/src/main/resources/static/` (vite.config.js에 설정됨)

**NODE_ENV=production 효과:**
1. ✅ Vite가 자동으로 `.env.production` 파일 읽음
2. ✅ 프로덕션 최적화 활성화 (코드 압축, Tree Shaking, 디버그 제거)
3. ✅ Spring Boot의 static 리소스 폴더에 직접 빌드
4. ✅ Spring Boot 이미지에 프론트엔드가 자동으로 포함됨

**경로 흐름:**
```
npm run build
  ↓
vite.config.js 설정 참조
  ↓
outDir: 'springboot/src/main/resources/static'
  ↓
Spring Boot JAR 빌드 시 자동으로 포함됨
```

### Step 5: Docker Buildx 설정

```yaml
- name: Set up Docker Buildx
  uses: docker/setup-buildx-action@v2
```

→ Docker 멀티 플랫폼 빌드 및 캐싱 기능 활성화
→ 빌드 성능 최적화

### Step 6: Docker Hub 로그인

```yaml
- name: Log in to Docker Hub
  uses: docker/login-action@v3
  with:
    username: ${{ secrets.DOCKERHUB_USERNAME }}  # ruyahct
    password: ${{ secrets.DOCKERHUB_TOKEN }}     # dckr_pat_...
```

→ Docker Hub에 이미지를 푸시하기 위한 인증

### Step 7: Spring Boot Docker 이미지 빌드 & Push ⭐

```yaml
- name: Build and push Docker image
  uses: docker/build-push-action@v5
  with:
    context: ./springboot
    file: ./springboot/Dockerfile
    push: true
    tags: ruyahct/kbo-springboot:latest
    cache-from: type=registry,ref=ruyahct/kbo-springboot:buildcache
    cache-to: type=registry,ref=ruyahct/kbo-springboot:buildcache,mode=max
```

→ **핵심!** Spring Boot Dockerfile의 멀티스테이지 빌드 실행:

**멀티스테이지 빌드 과정 (springboot/Dockerfile):**

```dockerfile
# 1단계: 빌드 스테이지
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN mvn dependency:go-offline -B  # 의존성 다운로드 (캐시 가능)
COPY src ./src                     # 소스 코드 복사 (프론트엔드 포함!)
RUN mvn clean package -DskipTests  # JAR 빌드

# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/springboot-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

→ **빌드 캐시 활용**: 이전 빌드의 레이어를 재사용하여 속도 향상
→ **최종 이미지는 JRE만 포함**: 빌드 도구 제외로 이미지 크기 최소화

**프론트엔드 포함 과정:**
```
1. npm run build → springboot/src/main/resources/static/
2. COPY src ./src → Dockerfile에서 static 폴더 복사
3. mvn clean package → JAR에 static 파일 포함
4. JAR 실행 시 Spring Boot가 static 파일 서빙
```

### Step 8: FastAPI Docker 이미지 빌드 & Push

```yaml
- name: Build and push FastAPI Docker image
  uses: docker/build-push-action@v5
  with:
    context: ./python
    file: ./python/Dockerfile
    push: true
    tags: ruyahct/kbo-fastapi:latest
    cache-from: type=registry,ref=ruyahct/kbo-fastapi:buildcache
    cache-to: type=registry,ref=ruyahct/kbo-fastapi:buildcache,mode=max
```

**FastAPI Dockerfile 과정 (python/Dockerfile):**

```dockerfile
FROM python:3.12-slim
WORKDIR /app
# Oracle 클라이언트 및 Selenium용 시스템 패키지 설치
RUN apt-get update && apt-get install -y \
    wget unzip libaio1t64 chromium chromium-driver ffmpeg \
    && rm -rf /var/lib/apt/lists/*
# 의존성 설치 (레이어 캐싱 최적화)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
# Python 파일 전체 복사
COPY . .
RUN mkdir -p news_backup
EXPOSE 8020
CMD ["python", "FastAPI_server.py"]
```

→ Python 3.12 slim 이미지 사용
→ Chromium/Selenium 포함 (웹 크롤링용)
→ FFmpeg 포함 (오디오 처리용)

### Step 9: EC2 배포 ⭐

```yaml
- name: Deploy Spring Boot and FastAPI to EC2
  uses: appleboy/ssh-action@v1.0.0
  with:
    host: ${{ secrets.EC2_HOST }}
    username: ${{ secrets.EC2_USER }}
    key: ${{ secrets.EC2_SSH_KEY }}
    script: |
      cd myball

      # Spring Boot 컨테이너 배포
      docker-compose pull springboot
      docker-compose up -d springboot

      # FastAPI 컨테이너 배포
      docker-compose pull fastapi
      docker-compose up -d fastapi

      # 불필요 이미지 정리
      docker image prune -f

      # 상태 확인
      echo "Waiting for services to be healthy..."
      sleep 30
      docker-compose ps
      echo "Deployment completed at $(date)"
```

→ EC2 서버에 SSH 접속
→ Docker Hub에서 최신 이미지 다운로드 (`pull`)
→ 컨테이너 재시작 (`up -d`)
→ 구 이미지 자동 삭제 (`prune -f`)
→ 30초 안정화 대기 (Spring Boot 시작 시간 고려)

**⚠️ 중요: Nginx는 재배포하지 않음**
- Nginx 설정은 자주 변경되지 않음
- 필요시 수동으로 재배포

### Step 10: 배포 완료 메시지

```yaml
- name: Deployment complete
  run: echo "ALL Deployments are Completed!"
```

→ GitHub Actions 로그에 완료 메시지 출력

---

## GitHub UI에서 확인하는 방법

### 1. GitHub 저장소 → Actions 탭

```
https://github.com/your-username/myball/actions
```

### 2. 실행 내역 확인

```
Actions 탭 → Build and Deploy 워크플로우 선택
→ 각 실행 기록 클릭 → 상세 로그 확인
```

### 3. 수동 실행

```
Actions 탭 → Build and Deploy
→ "Run workflow" 버튼 클릭 (workflow_dispatch 덕분)
→ master 브랜치 선택
→ "Run workflow" 버튼 클릭
```

### 4. 실행 로그 예시

```
✅ Build and Deploy #42
   master 브랜치 · 8분 전

   📦 build-and-deploy (ubuntu-latest) - 8m 15s
      ✅ Checkout code                    12s
      ✅ Setup Node.js                    18s
      ✅ Install dependencies           2m 34s
      ✅ Build frontend                 1m 12s
      ✅ Set up Docker Buildx              8s
      ✅ Log in to Docker Hub              3s
      ✅ Build and push Docker image    2m 45s
      ✅ Build and push FastAPI image   1m 28s
      ✅ Deploy to EC2                    42s
      ✅ Deployment complete               1s
```

---

## docker-compose.yml 구조

### 운영 배포용 (docker-compose.yml)

**특징:**
- ✅ Docker Hub의 이미지를 사용 (`image:` 지정)
- ✅ 로컬 빌드 없음 (`build:` 섹션 없음)
- ✅ EC2 서버에서 사용

**서비스 구성:**

```yaml
services:
  oracle:
    image: container-registry.oracle.com/database/express:18.4.0-xe
    # Oracle Database

  springboot:
    image: ruyahct/kbo-springboot:latest  # Docker Hub에서 다운로드
    depends_on:
      oracle:
        condition: service_healthy

  fastapi:
    image: ruyahct/kbo-fastapi:latest     # Docker Hub에서 다운로드
    depends_on:
      oracle:
        condition: service_healthy

  nginx:
    image: ruyahct/kbo-nginx:latest       # Docker Hub에서 다운로드
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /etc/letsencrypt:/etc/letsencrypt:ro  # SSL 인증서
```

**컨테이너 간 통신:**
```yaml
networks:
  kbo-network:
    driver: bridge  # 같은 네트워크의 컨테이너끼리만 통신
```

→ 서비스 이름으로 DNS 자동 등록
→ 예: `http://springboot:8080`, `http://fastapi:8020`

### 개발/빌드용 (docker-compose.development.yml)

**특징:**
- ✅ 로컬에서 직접 빌드 (`build:` 섹션 있음)
- ✅ Docker Hub에 푸시하기 위한 설정
- ✅ **실행(up) 금지** - 빌드와 푸시만 사용

**사용 방법:**
```bash
# 이미지 빌드
docker-compose -f docker-compose.development.yml build

# Docker Hub에 푸시
docker-compose -f docker-compose.development.yml push

# 빌드 + 푸시 한 번에
docker-compose -f docker-compose.development.yml build && \
docker-compose -f docker-compose.development.yml push
```

**⚠️ 주의사항:**
- `docker-compose -f docker-compose.development.yml up` **절대 금지**
- `container_name`이 없어서 랜덤 이름으로 컨테이너가 생성됨
- 실행은 반드시 `docker-compose.yml`로만!

---

## 배포 시나리오

### 시나리오 1: 프론트엔드 수정

```
1. src/ 폴더의 React 코드 수정
   ↓
2. git add . && git commit -m "Update frontend"
   ↓
3. git push origin master
   ↓
4. GitHub Actions 자동 실행
   ├─ Frontend 빌드 → springboot/src/main/resources/static/
   ├─ Spring Boot 이미지 빌드 (프론트엔드 포함)
   ├─ Docker Hub 업로드
   └─ EC2 배포
   ↓
5. EC2에서 Spring Boot 컨테이너 재시작
   └─ ✅ 프론트엔드 업데이트 완료!
```

### 시나리오 2: 백엔드 수정

```
1. springboot/ 폴더의 Java 코드 수정
   ↓
2. git add . && git commit -m "Update backend"
   ↓
3. git push origin master
   ↓
4. GitHub Actions 자동 실행
   ├─ Frontend 빌드 (변경 없어도 실행)
   ├─ Spring Boot 이미지 빌드
   ├─ Docker Hub 업로드
   └─ EC2 배포
   ↓
5. EC2에서 Spring Boot 컨테이너 재시작
   └─ ✅ 백엔드 업데이트 완료!
```

### 시나리오 3: FastAPI 수정

```
1. python/ 폴더의 Python 코드 수정
   ↓
2. git add . && git commit -m "Update FastAPI"
   ↓
3. git push origin master
   ↓
4. GitHub Actions 자동 실행
   ├─ Frontend 빌드 (변경 없어도 실행)
   ├─ Spring Boot 이미지 빌드
   ├─ FastAPI 이미지 빌드
   ├─ Docker Hub 업로드
   └─ EC2 배포
   ↓
5. EC2에서 FastAPI 컨테이너 재시작
   └─ ✅ FastAPI 업데이트 완료!
```

### 시나리오 4: Nginx 설정 수정

```
1. nginx/default.conf 수정
   ↓
2. 로컬에서 수동 빌드 & 푸시
   docker build -f Dockerfile.nginx -t ruyahct/kbo-nginx:latest .
   docker push ruyahct/kbo-nginx:latest
   ↓
3. EC2 서버에 SSH 접속
   cd myball
   docker-compose pull nginx
   docker-compose up -d nginx
   ↓
4. ✅ Nginx 업데이트 완료!
```

→ Nginx는 자주 변경되지 않으므로 수동 배포

---

## 핵심 검증 포인트

### 1. 브랜치 설정

```yaml
on:
  push:
    branches: [ master ]  ✅
```

### 2. 프론트엔드 빌드 경로

```javascript
// vite.config.js
build: {
  outDir: 'springboot/src/main/resources/static'  ✅
}
```

### 3. Docker Hub 이미지 태그

```yaml
tags: ruyahct/kbo-springboot:latest  ✅
tags: ruyahct/kbo-fastapi:latest     ✅
```

### 4. EC2 배포 경로

```bash
cd myball  ✅
```

### 5. docker-compose 서비스명

```bash
docker-compose pull springboot  ✅
docker-compose up -d springboot ✅

docker-compose pull fastapi  ✅
docker-compose up -d fastapi ✅
```

### 6. 환경 변수 설정

**GitHub Secrets 등록 필요:**
- `DOCKERHUB_USERNAME` : ruyahct
- `DOCKERHUB_TOKEN` : Docker Hub Access Token
- `EC2_HOST` : EC2 Public IP
- `EC2_USER` : ubuntu (또는 EC2 사용자명)
- `EC2_SSH_KEY` : SSH Private Key

---

## 비용

### Private Repository:
- **월 2,000분** 무료
- 초과 시 **$0.008/분** (Linux runner)
- 현재 워크플로우: 약 **8-12분/빌드**
- **월 200회 미만 빌드** → 완전 무료 ✅

### Public Repository:
- **무제한 무료** ✅

---

## 최적화 팁

### 1. 빌드 캐시 활용

```yaml
cache-from: type=registry,ref=ruyahct/kbo-springboot:buildcache
cache-to: type=registry,ref=ruyahct/kbo-springboot:buildcache,mode=max
```

→ Docker Hub에 빌드 캐시 저장
→ 변경된 레이어만 재빌드하여 속도 향상

### 2. npm 캐시

```yaml
- name: Setup Node.js
  with:
    cache: 'npm'
```

→ npm 의존성 캐싱으로 설치 속도 향상

### 3. 동시 실행 방지

```yaml
concurrency:
  group: build-deploy
  cancel-in-progress: false
```

→ 순차 실행으로 안전한 배포

### 4. Dockerfile 레이어 캐싱

```dockerfile
# 의존성 먼저 복사 (변경 빈도 낮음)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 소스 코드 나중에 복사 (변경 빈도 높음)
COPY . .
```

→ 변경되지 않은 레이어는 캐시 재사용

---

## 트러블슈팅

### 문제 1: 빌드 실패

**증상:** `npm run build` 실패
**원인:** 프론트엔드 코드 오류
**해결:**
```bash
# 로컬에서 먼저 테스트
npm run build
```

### 문제 2: Docker Hub 푸시 실패

**증상:** `denied: requested access to the resource is denied`
**원인:** Docker Hub 인증 실패
**해결:** GitHub Secrets 확인
- `DOCKERHUB_USERNAME` 정확한지 확인
- `DOCKERHUB_TOKEN` 유효한지 확인

### 문제 3: EC2 배포 실패

**증상:** `Permission denied (publickey)`
**원인:** SSH 키 오류
**해결:**
```bash
# SSH 키 형식 확인 (개행 포함)
cat ~/.ssh/id_rsa
```
→ GitHub Secrets에 전체 내용 복사

### 문제 4: 컨테이너 시작 실패

**증상:** `docker-compose up -d` 후 컨테이너 즉시 종료
**원인:** 환경 변수 누락 또는 DB 연결 실패
**해결:**
```bash
# EC2에서 로그 확인
docker-compose logs springboot
docker-compose logs fastapi

# .env 파일 확인
cat .env
```

---

## 요약

### ✅ 모노레포의 장점

1. **단일 워크플로우**: 하나의 deploy.yml로 모든 서비스 관리
2. **프론트엔드 자동 통합**: vite.config.js 설정으로 Spring Boot에 자동 포함
3. **일관된 배포**: 모든 서비스가 동시에 업데이트됨
4. **간편한 관리**: 하나의 레포지토리만 관리하면 됨

### ✅ 배포 과정 요약

```
git push master
  ↓
GitHub Actions 실행
  ↓
Frontend 빌드 → Spring Boot static 폴더
  ↓
Spring Boot + FastAPI 이미지 빌드
  ↓
Docker Hub 업로드
  ↓
EC2 배포
  ↓
✅ 완료!
```

**별도 설정 없이 코드만 푸시하면 자동으로 배포됩니다!** 🚀

---

## 참고 자료

- [GitHub Actions 공식 문서](https://docs.github.com/en/actions)
- [Docker Build Push Action](https://github.com/docker/build-push-action)
- [SSH Action](https://github.com/appleboy/ssh-action)
- [Vite 빌드 설정](https://vitejs.dev/config/build-options.html)
