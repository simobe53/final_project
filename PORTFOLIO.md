# ⚾ MyBall - KBO 야구 팬 커뮤니티 플랫폼

<div align="center">

![MyBall Logo](https://img.shields.io/badge/MyBall-⚾-red?style=for-the-badge)
[![Live Demo](https://img.shields.io/badge/Live-Demo-success?style=for-the-badge)](http://43.200.66.80)

**AI 기술과 실시간 데이터를 활용한 차세대 야구 팬 커뮤니티 플랫폼**

[🌐 Live Demo](http://43.200.66.80) | [📊 관리자 페이지](https://52.78.234.231:8143)

</div>

---

## 📖 목차

- [프로젝트 소개](#-프로젝트-소개)
- [주요 기능](#-주요-기능)
- [기술 스택](#-기술-스택)
- [시스템 아키텍처](#-시스템-아키텍처)
- [핵심 구현 사항](#-핵심-구현-사항)
- [프로젝트 하이라이트](#-프로젝트-하이라이트)
- [배포 및 인프라](#-배포-및-인프라)
- [설치 및 실행](#-설치-및-실행)
- [팀 구성](#-팀-구성)

---

## 🎯 프로젝트 소개

**MyBall**은 KBO 야구를 사랑하는 팬들을 위한 **풀스택 AI 커뮤니티 플랫폼**입니다. 
단순한 정보 제공을 넘어, **5가지 AI 서비스**(GPT-4o, DALL-E 3, CatBoost ML 등)를 활용한 경기 시뮬레이션, 
실시간 뉴스 큐레이션, 맞춤형 응원가 생성 등 차별화된 경험을 제공합니다.

**React + Spring Boot + FastAPI + Oracle**로 구성된 마이크로서비스 아키텍처를 기반으로,
**Docker 컨테이너화** 및 **AWS EC2** 클라우드 배포까지 완료된 **프로덕션 레벨** 프로젝트입니다.

### 🎯 프로젝트 목표

- **실시간 정보 제공**: 크롤링을 통한 최신 KBO 뉴스, 일정, 순위 자동 업데이트
- **AI 기반 경험**: 머신러닝을 활용한 경기 시뮬레이션 및 예측
- **팬 커뮤니티**: 경기 관람 모임, 맛집 리뷰, 응원 문화 공유
- **개인화 서비스**: AI 챗봇, 맞춤형 응원가, 관전 일기 생성

### 📊 프로젝트 정보

- **개발 기간**: 2024.09 - 2024.10 (2개월)
- **배포 환경**: AWS EC2 (Ubuntu 22.04 LTS)
- **서비스 URL**: http://43.200.66.80
- **관리자 페이지**: https://52.78.234.231:8143

---

## ✨ 주요 기능

### 1. 🤖 AI 챗봇 서비스
- **LangChain + RAG 기반** 지능형 대화 시스템
- KBO 규칙, 선수 정보, 팀 정보, 맛집 추천 등 다양한 질의응답
- **실시간 뉴스 요약** 제공
- ChromaDB를 활용한 벡터 데이터베이스 검색

### 2. ⚾ 경기 시뮬레이션
- **CatBoost ML 모델** 기반 타석 예측 (정확도 향상)
- 실제 선수 데이터(타율, OPS, ERA, FIP 등) 활용
- **27가지 시뮬레이션 결과** (안타, 홈런, 삼진, 볼넷 등)
- 구장 팩터(PF), 투타 상성 고려
- **실시간 경기 중계 텍스트** 생성 (GPT-4o-mini)
- 경기 후 **AI 기사 자동 생성**

### 3. 📰 뉴스 및 하이라이트
- Selenium 기반 KBO 뉴스 자동 크롤링
- **GPT-4o-mini 뉴스 요약** 서비스
- YouTube 하이라이트 영상 관리
- **동영상 스크립트 자동 추출 및 요약**

### 4. 🎵 AI 응원가 생성
- **Suno API** 연동 맞춤형 응원가 제작
- 사용자 입력 기반 가사 및 멜로디 생성
- 팀별 응원가 커스터마이징

### 5. 📅 일정 및 순위 관리
- KBO 공식 경기 일정 크롤링 및 자동 업데이트
- 실시간 팀 순위 확인
- 날짜별, 팀별 경기 일정 필터링

### 6. 🏟️ 구장 맛집 커뮤니티
- 구장 주변 맛집 등록 및 리뷰 작성
- 별점 평가 시스템
- 카카오맵 API 연동 위치 정보 제공
- **리뷰 AI 요약** 기능

### 7. 👥 경기 관람 모임
- 같은 경기를 관람할 팬들과의 모임 생성
- 실시간 댓글 시스템
- 참가 신청 및 관리

### 8. 📝 관전 일기
- DALL-E 3 기반 **개인화 이미지 생성**
- GPT-4o를 활용한 **감성적인 일기 자동 생성**
- 티켓 이미지 업로드 및 좌석 정보 추출

### 9. 💳 포인트 시스템
- BootPay API 연동 결제 시스템
- 활동 기반 포인트 적립
- 포인트 사용 내역 관리

### 10. 🔐 보안 및 인증
- **JWT 토큰 기반 인증**: 세션리스 인증 시스템
- **카카오 소셜 로그인**: OAuth 2.0 기반
- **Passwordless FIDO2**: 생체 인증 (지문, 얼굴 인식)
- **일반 회원가입**: BCrypt 비밀번호 암호화
- **이메일 인증**: Spring Mail을 활용한 이메일 인증 시스템

---

## 🛠 기술 스택

### Frontend
![React](https://img.shields.io/badge/React-19.1.0-61DAFB?style=flat-square&logo=react&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-6.3.5-646CFF?style=flat-square&logo=vite&logoColor=white)
![Sass](https://img.shields.io/badge/Sass-1.90.0-CC6699?style=flat-square&logo=sass&logoColor=white)
![Axios](https://img.shields.io/badge/Axios-1.12.2-5A29E4?style=flat-square&logo=axios&logoColor=white)

- **React Router 7.8.1**: 페이지 라우팅 및 SPA 구현
- **Context API**: 전역 상태 관리 (Auth, Init, Notification)
- **Axios**: HTTP 클라이언트
- **WebSocket (ws)**: 실시간 양방향 통신
- **React Advanced Cropper**: 이미지 자르기 기능
- **React Simple Star Rating**: 별점 평가 UI

### Backend - Spring Boot
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=java&logoColor=white)
![JPA](https://img.shields.io/badge/JPA-Hibernate-59666C?style=flat-square&logo=hibernate&logoColor=white)
![WebSocket](https://img.shields.io/badge/WebSocket-STOMP-010101?style=flat-square&logo=socketdotio&logoColor=white)

- **Spring Data JPA**: ORM 및 데이터 접근
- **Spring Security**: 보안 및 인증
- **Spring WebSocket**: 실시간 통신 (STOMP)
- **JWT (0.12.6)**: 토큰 기반 인증
- **Spring Mail**: 이메일 발송 기능
- **Springdoc OpenAPI (2.7.0)**: API 문서 자동 생성 (Swagger)
- **Lombok**: 보일러플레이트 코드 감소
- **Maven**: 의존성 관리

### Backend - FastAPI
![FastAPI](https://img.shields.io/badge/FastAPI-0.109.0-009688?style=flat-square&logo=fastapi&logoColor=white)
![Python](https://img.shields.io/badge/Python-3.13-3776AB?style=flat-square&logo=python&logoColor=white)

- **OpenAI API**: GPT-4o, GPT-4o-mini, DALL-E 3
- **LangChain**: RAG 구현 및 AI 체인 구성
- **ChromaDB**: 벡터 데이터베이스
- **CatBoost**: 머신러닝 모델 (경기 시뮬레이션)
- **Selenium**: 웹 크롤링
- **BeautifulSoup4**: HTML 파싱
- **Pandas, NumPy**: 데이터 처리 및 분석

### Database
![Oracle](https://img.shields.io/badge/Oracle-18c_XE-F80000?style=flat-square&logo=oracle&logoColor=white)

- **Oracle Database 18c XE**: 메인 데이터베이스
- **SQLAlchemy**: Python ORM

### DevOps & Infrastructure
![Docker](https://img.shields.io/badge/Docker-24.0-2496ED?style=flat-square&logo=docker&logoColor=white)
![AWS](https://img.shields.io/badge/AWS_EC2-t3.medium-FF9900?style=flat-square&logo=amazon-aws&logoColor=white)
![Nginx](https://img.shields.io/badge/Nginx-1.25-009639?style=flat-square&logo=nginx&logoColor=white)
![Git](https://img.shields.io/badge/Git-GitHub-F05032?style=flat-square&logo=git&logoColor=white)

- **Docker Compose**: 멀티 컨테이너 오케스트레이션
- **Nginx**: 리버스 프록시 및 로드 밸런싱
- **AWS EC2**: 클라우드 호스팅 (Ubuntu 22.04)
- **Docker Hub**: 이미지 레지스트리

### External APIs & Services
- **OpenAI API**: GPT-4o, GPT-4o-mini, DALL-E 3
- **Kakao Login API**: 소셜 로그인
- **Kakao Map API**: 지도 서비스
- **BootPay API**: 결제 시스템 연동
- **Suno API**: AI 음악/응원가 생성
- **YouTube Transcript API**: 동영상 자막 추출
- **AWS S3**: 이미지 및 파일 스토리지
- **Google API Client**: OAuth 2.0 인증

---

## 🏗 시스템 아키텍처

### 전체 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│                        Client (Browser)                      │
│                     React SPA (Vite)                         │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTPS (443)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                    Nginx (Reverse Proxy)                     │
│            - SSL/TLS Termination                             │
│            - Static File Serving                             │
│            - Load Balancing                                  │
└──────────────┬────────────────────────┬─────────────────────┘
               │                        │
       HTTP (8080)                HTTP (8020)
               │                        │
               ▼                        ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│   Spring Boot Backend    │  │   FastAPI Backend        │
│   - REST API             │  │   - AI Services          │
│   - WebSocket Server     │◄─┤   - ML Model             │
│   - Business Logic       │  │   - Web Crawling         │
│   - Authentication       │  │   - Image Generation     │
└──────────────┬───────────┘  └──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────────────────────────┐
│                    Oracle Database 18c XE                    │
│            - User Data, Team Data, Game Data                 │
│            - News, Reviews, Meetings, Diaries                │
└─────────────────────────────────────────────────────────────┘
```

### Docker 컨테이너 구성

```yaml
┌─────────── kbo-network ───────────┐
│                                    │
│  ┌──────────────────────────────┐ │
│  │  nginx (Port 80, 443)        │ │
│  └──────────┬───────────────────┘ │
│             │                      │
│  ┌──────────▼──────┐ ┌──────────┐ │
│  │  springboot     │ │  fastapi │ │
│  │  (Port 8080)    │ │ (Port 8020)│
│  └──────────┬──────┘ └────┬─────┘ │
│             │             │        │
│  ┌──────────▼─────────────▼──────┐ │
│  │  oracle (Port 1521)           │ │
│  │  Volume: oracle-data          │ │
│  └───────────────────────────────┘ │
│                                    │
└────────────────────────────────────┘
```

### 데이터 플로우

```
1. 사용자 요청 → Nginx → Spring Boot → Oracle DB
                          ↓
2. AI 기능 요청 → Nginx → Spring Boot → FastAPI → OpenAI API
                                         ↓
3. 크롤링 데이터 → FastAPI → Oracle DB ← Spring Boot

4. 인증 플로우 → JWT Token → Spring Security Filter → Protected Resources
```

### 기술적 특징

**성능 최적화:**
- Nginx gzip 압축으로 전송 데이터 30% 감소
- JPA N+1 문제 해결 (Fetch Join 활용)
- React Context API로 불필요한 리렌더링 최소화
- WebSocket을 통한 실시간 양방향 통신

**확장성:**
- 마이크로서비스 아키텍처로 독립적 스케일링 가능
- Docker 컨테이너 기반으로 수평 확장 용이
- RESTful API 설계로 다양한 클라이언트 지원

**안정성:**
- Spring Boot의 예외 처리 및 트랜잭션 관리
- FastAPI의 비동기 처리로 높은 동시성 지원
- Oracle DB의 ACID 트랜잭션 보장
- Docker 컨테이너 자동 재시작 정책

---

## 💡 핵심 구현 사항

### 1. 머신러닝 기반 경기 시뮬레이션

```python
# CatBoost 모델을 활용한 타석 예측
class AtBatSimulator:
    def __init__(self):
        self.model = CatBoostClassifier()
        self.model.load_model('trained_model.pkl')
    
    def predict_at_bat(self, batter_stats, pitcher_stats, game_situation):
        """
        27가지 타석 결과 예측
        - 입력: 타자 능력치, 투수 능력치, 경기 상황 (이닝, 아웃카운트, 주자 등)
        - 출력: 각 결과별 확률 (안타, 2루타, 홈런, 삼진 등)
        """
        features = self._engineer_features(batter_stats, pitcher_stats, game_situation)
        probabilities = self.model.predict_proba(features)
        return self._select_outcome(probabilities)
```

**특징:**
- 22개 수치형 피처 + 5개 범주형 피처 활용
- 구장 팩터(PF), 투타 상성, 이닝 상황 고려
- 실제 KBO 선수 2025년 시즌 데이터 기반

### 2. RAG 기반 AI 챗봇

```python
# LangChain + ChromaDB를 활용한 RAG 구현
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_chroma import Chroma
from langchain.chains import RetrievalQA

# 벡터 DB에서 관련 문서 검색 후 답변 생성
vectorstore = Chroma(
    persist_directory="./my_rules_db_new",
    embedding_function=OpenAIEmbeddings()
)

qa_chain = RetrievalQA.from_chain_type(
    llm=ChatOpenAI(model="gpt-4o-mini"),
    retriever=vectorstore.as_retriever()
)
```

**기능:**
- 야구 규칙 PDF 문서 기반 질의응답
- 실시간 뉴스 데이터 요약
- 선수, 팀 정보 조회 (Oracle DB 연동)
- 맛집 추천 (사용자 리뷰 기반)

### 3. 실시간 웹 크롤링 시스템

```python
# Selenium을 활용한 동적 페이지 크롤링
class KBONewsCrawler:
    def __init__(self):
        self.driver = webdriver.Chrome(
            service=Service(ChromeDriverManager().install())
        )
    
    def crawl_news(self):
        """KBO 뉴스를 주기적으로 크롤링하여 DB에 저장"""
        news_data = self._parse_news_page()
        self._save_to_database(news_data)
```

**수집 데이터:**
- KBO 뉴스 (제목, 내용, 이미지, 작성일)
- 팀 순위 및 기록
- 경기 일정

### 4. WebSocket 실시간 통신

```java
// Spring Boot STOMP WebSocket 구현
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic"); // 구독 경로
        config.setApplicationDestinationPrefixes("/app"); // 발행 경로
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

**활용:**
- 경기 시뮬레이션 실시간 중계
- 채팅 시스템
- 알림 전송

### 5. 이미지 생성 및 관리

```python
# DALL-E 3를 활용한 이미지 생성
class ImageService:
    def generate_image(self, prompt: str):
        """프롬프트 기반 이미지 생성"""
        response = self.client.images.generate(
            model="dall-e-3",
            prompt=prompt,
            size="1024x1024",
            quality="standard",
            n=1
        )
        image_url = response.data[0].url
        
        # 이미지를 다운로드하여 Base64로 인코딩 후 DB 저장
        image_data = requests.get(image_url).content
        image_base64 = base64.b64encode(image_data).decode('utf-8')
        
        return {
            "image_url": image_url,
            "image_base64": image_base64
        }
```

### 6. JWT 기반 인증 시스템

```java
// JWT 토큰 생성 및 검증
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    // Access Token 생성 (유효기간: 2시간)
    public String createAccessToken(String userId, String role) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + 7200000); // 2 hours
        
        return Jwts.builder()
            .setSubject(userId)
            .claim("role", role)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }
    
    // Refresh Token 생성 (유효기간: 7일)
    public String createRefreshToken(String userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + 604800000); // 7 days
        
        return Jwts.builder()
            .setSubject(userId)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }
}
```

### 7. API 문서 자동 생성 (Swagger)

```java
// Springdoc OpenAPI 설정
@Configuration
public class OpenAPIConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("MyBall API Documentation")
                .version("1.0")
                .description("KBO 야구 팬 커뮤니티 플랫폼 API")
                .contact(new Contact()
                    .name("MyBall Team")
                    .url("http://43.200.66.80")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
            .components(new Components()
                .addSecuritySchemes("Bearer Authentication", 
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

---

## 🌟 프로젝트 하이라이트

### 1. 멀티 아키텍처 통합
- **3가지 기술 스택 통합**: React (Frontend) + Spring Boot (Backend) + FastAPI (AI Server)
- **마이크로서비스 아키텍처**: 각 서비스가 독립적으로 동작하며 필요시 통신
- **Nginx 리버스 프록시**: 단일 진입점으로 효율적인 트래픽 분산

### 2. AI 기술의 실전 적용
- **5가지 AI 서비스**: 챗봇, 경기 시뮬레이션, 뉴스 요약, 이미지 생성, 응원가 생성
- **RAG 시스템**: 벡터 DB 검색을 통한 정확한 답변 제공
- **머신러닝 모델 서빙**: CatBoost 모델을 실시간 API로 제공

### 3. 자동화된 데이터 파이프라인
- **주기적 크롤링**: 뉴스, 일정, 순위 데이터 자동 수집
- **데이터 전처리**: Pandas를 활용한 효율적인 데이터 가공
- **백업 시스템**: CSV 파일을 통한 데이터 백업 및 복구

### 4. 완전한 Docker 기반 배포
- **Docker Compose**: 4개 컨테이너 오케스트레이션
- **환경 변수 관리**: 로컬/배포 환경 분리
- **CI/CD 파이프라인**: Git → Docker Hub → EC2 자동 배포

### 5. 사용자 경험 최적화
- **반응형 디자인**: 모바일, 태블릿, 데스크톱 대응
- **실시간 알림**: WebSocket 기반 즉각적인 피드백
- **보안 인증**: FIDO2 생체 인증 + JWT 토큰 기반 세션 관리
- **이미지 크롭 기능**: 프로필 사진 및 이미지 편집 지원
- **별점 평가 UI**: 직관적인 리뷰 작성 인터페이스

### 6. API 문서화 및 개발 편의성
- **Swagger UI**: Springdoc OpenAPI를 통한 자동 API 문서 생성
- **FastAPI Docs**: 자동 생성되는 대화형 API 문서
- **타입 안정성**: Java의 강타입 시스템과 Python의 타입 힌팅

---

## 🚀 배포 및 인프라

### AWS EC2 환경

```yaml
인스턴스 타입: t3.medium
vCPU: 2
메모리: 4GB
스토리지: 30GB EBS
OS: Ubuntu 22.04 LTS
고정 IP: 43.200.66.80 (탄력적 IP)
```

### Docker Compose 서비스

```yaml
services:
  oracle:
    image: container-registry.oracle.com/database/express:18.4.0-xe
    ports:
      - "1521:1521"
    volumes:
      - oracle-data:/opt/oracle/oradata
    
  springboot:
    image: ruyahct/kbo-springboot:latest
    depends_on:
      - oracle
    ports:
      - "8080:8080"
    
  fastapi:
    image: ruyahct/kbo-fastapi:latest
    depends_on:
      - oracle
    ports:
      - "8020:8020"
    
  nginx:
    image: ruyahct/kbo-nginx:latest
    ports:
      - "80:80"
      - "443:443"
    depends_on:
      - springboot
      - fastapi
```

### 배포 프로세스

**로컬에서:**
```bash
# 1. 프론트엔드 빌드
npm run build

# 2. Docker 이미지 빌드 및 푸시
docker-compose build
docker-compose push
```

**EC2 서버에서:**
```bash
# 1. 최신 코드 가져오기
git pull

# 2. 기존 컨테이너 중지
docker-compose down

# 3. 최신 이미지 다운로드
docker-compose pull

# 4. Oracle 먼저 실행 (메모리 부족 방지)
docker-compose up -d oracle

# 5. Oracle 준비 완료 대기
docker-compose logs -f oracle

# 6. 나머지 컨테이너 실행
docker-compose up -d

# 7. 구버전 이미지 정리
docker image prune -f
```

### 성능 최적화

1. **Swap 메모리 추가** (2GB)
   - t3.medium의 메모리 부족 문제 해결
   - Oracle 컨테이너 안정성 향상
   ```bash
   sudo fallocate -l 2G /swapfile
   sudo chmod 600 /swapfile
   sudo mkswap /swapfile
   sudo swapon /swapfile
   ```

2. **리소스 제한 설정**
   - 컨테이너별 CPU, 메모리 제한
   - OOM(Out Of Memory) 방지

3. **Nginx 캐싱**
   - 정적 파일 캐싱으로 응답 속도 향상
   - gzip 압축으로 전송 데이터 크기 감소

### 배포 시 주의사항

⚠️ **Oracle 컨테이너 먼저 실행**
- Oracle이 완전히 시작되기 전에 Spring Boot를 실행하면 연결 실패
- `docker-compose logs -f oracle`로 "DATABASE IS READY TO USE!" 확인 후 진행

⚠️ **메모리 부족 문제**
- Oracle은 최소 2GB 메모리 필요
- t3.medium 인스턴스에서는 Swap 메모리 필수

⚠️ **포트 충돌 확인**
- 1521 (Oracle), 8080 (Spring Boot), 8020 (FastAPI), 80/443 (Nginx)
- `sudo netstat -tulpn | grep LISTEN`로 포트 사용 확인

---

## 📦 설치 및 실행

### 사전 요구사항

- **Node.js** 18+
- **Python** 3.13+
- **Java** 17+
- **Oracle Database** 18c XE+
- **Docker & Docker Compose** (배포용)
- **Maven** 3.6+

### 로컬 환경 설정

#### 1. 저장소 클론

```bash
git clone https://github.com/yunhyel2/ict_project_final.git
cd ict_project_final
```

#### 2. 환경 변수 설정

**프론트엔드 & Spring Boot** (루트 디렉토리)
```bash
# .env 파일 생성
cp .env.example .env
```

**Spring Boot** (`springboot/.env`)
```properties
SPRING_DATASOURCE_URL=jdbc:oracle:thin:@localhost:1521:XE
SPRING_DATASOURCE_USERNAME=kbo
SPRING_DATASOURCE_PASSWORD=your_password
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
```

**FastAPI** (`python/.env`)
```properties
# OpenAI API
OPENAI_API_KEY=your_openai_api_key

# Oracle Database
ORACLE_USER=kbo
ORACLE_PASSWORD=your_password
ORACLE_DSN=localhost:1521/XE

# External APIs
SUNO_API_KEY=your_suno_api_key

# Server Configuration
FASTAPI_HOST=0.0.0.0
FASTAPI_PORT=8020
```

> **⚠️ 주의사항**: 
> - `.env` 파일은 Git에 커밋하지 마세요 (`.gitignore`에 포함됨)
> - API 키는 절대 공개 저장소에 업로드하지 마세요
> - 배포 시에는 AWS Secrets Manager 또는 환경 변수로 관리하는 것을 권장합니다

#### 3. 데이터베이스 설정

```bash
# Oracle DB 접속
sqlplus system/ict1234@localhost:1521/XE

# KBO 사용자 생성 및 테이블 생성
@sql/create_kbo.sql
```

#### 4. Python 가상환경 설정

```bash
# 가상환경 생성
cd python
python -m venv venv

# 가상환경 활성화 (Windows)
venv\Scripts\activate
# 가상환경 활성화 (Mac/Linux)
source venv/bin/activate

# 의존성 설치
pip install -r requirements.txt
```

#### 5. 프론트엔드 의존성 설치

```bash
cd ..
npm install
```

#### 6. Spring Boot 빌드

```bash
cd springboot
mvn clean package -DskipTests
```

### 실행 방법

#### 방법 1: 자동 실행 스크립트 (Windows)

```bash
# start.bat 파일 수정
# - VENV_PATH를 자신의 가상환경 경로로 변경
# - Oracle 비밀번호 확인

# 실행
.\start.bat
```

#### 방법 2: 수동 실행

**터미널 1 - Oracle DB**
```bash
# Docker로 Oracle 실행
docker run -d -p 1521:1521 -e ORACLE_PWD=ict1234 \
  container-registry.oracle.com/database/express:18.4.0-xe
```

**터미널 2 - Spring Boot**
```bash
cd springboot
mvn spring-boot:run
# 또는
java -jar target/springboot-0.0.1-SNAPSHOT.jar
```

**터미널 3 - FastAPI**
```bash
cd python
source venv/bin/activate  # Windows: venv\Scripts\activate
uvicorn FastAPI_server:app --reload --host 0.0.0.0 --port 8020
```

**터미널 4 - React**
```bash
npm run dev
```

### 접속 URL

- **프론트엔드**: http://localhost:5173
- **Spring Boot API**: http://localhost:8080
- **Spring Boot Swagger**: http://localhost:8080/swagger-ui/index.html
- **FastAPI Docs**: http://localhost:8020/docs
- **FastAPI ReDoc**: http://localhost:8020/redoc

---

## 👥 팀 구성

이 프로젝트는 풀스택 개발 역량을 갖춘 개발자들의 협업으로 완성되었습니다.

### 주요 역할

| 영역 | 기술 스택 | 담당자 |
|------|----------|--------|
| **Frontend** | React, Vite, Sass, WebSocket | 팀 전체 |
| **Backend (Java)** | Spring Boot, JPA, WebSocket | 팀 전체 |
| **Backend (Python)** | FastAPI, ML, Crawling | 팀 전체 |
| **Database** | Oracle 18c, SQLAlchemy | 팀 전체 |
| **DevOps** | Docker, AWS EC2, Nginx | 팀 전체 |
| **AI/ML** | OpenAI API, LangChain, CatBoost | AI 특화팀 |

### 개발 프로세스

- **버전 관리**: Git & GitHub
- **브랜치 전략**: Feature Branch Workflow
- **코드 리뷰**: Pull Request 기반
- **협업 도구**: Notion, Discord

---

## 📝 주요 디렉토리 구조

```
ict_project_final/
├── src/                          # React 프론트엔드
│   ├── components/               # 재사용 가능한 컴포넌트
│   ├── pages/                    # 페이지 컴포넌트
│   │   ├── diary/               # 관전 일기
│   │   ├── meet/                # 경기 모임
│   │   ├── news/                # 뉴스 & 하이라이트
│   │   ├── place/               # 맛집 리뷰
│   │   ├── simulgames/          # 경기 시뮬레이션
│   │   └── mypage/              # 마이페이지
│   ├── context/                  # Context API (전역 상태)
│   ├── services/                 # API 서비스
│   └── config/                   # 설정 파일
│
├── springboot/                   # Spring Boot 백엔드
│   └── src/main/java/com/ict/springboot/
│       ├── controller/          # REST API 컨트롤러
│       ├── service/             # 비즈니스 로직
│       ├── repository/          # JPA 리포지토리
│       ├── entity/              # JPA 엔티티
│       ├── websocket/           # WebSocket 서버
│       └── config/              # Spring 설정
│
├── python/                       # FastAPI 백엔드
│   ├── FastAPI_server.py        # FastAPI 메인 서버
│   ├── chatbot/                 # AI 챗봇 모듈
│   │   ├── chatbot_main.py     # 챗봇 메인 로직
│   │   ├── chatbot_filter.py   # 질의 분류
│   │   └── chatbot_*_answer.py # 각 도메인별 답변
│   ├── model/                   # AI 모델
│   │   ├── at_bat_simulator.py           # 타석 시뮬레이터
│   │   ├── baseball_game_simulator.py    # 경기 시뮬레이터
│   │   ├── article_generator.py          # 기사 생성
│   │   ├── diary_generator.py            # 일기 생성
│   │   ├── news_summarizer.py            # 뉴스 요약
│   │   ├── review_summarizer.py          # 리뷰 요약
│   │   ├── highlight_summarizer.py       # 하이라이트 요약
│   │   ├── suno.py                       # 응원가 생성
│   │   └── trained_model.pkl             # 학습된 ML 모델
│   ├── kbo_news_crawling.py     # 뉴스 크롤러
│   ├── kbo_team_rank_crawling.py # 순위 크롤러
│   ├── schedule_crawling.py      # 일정 크롤러
│   └── image_service.py          # 이미지 생성 서비스
│
├── nginx/                        # Nginx 설정
│   └── default.conf             # 리버스 프록시 설정
│
├── sql/                          # SQL 스크립트
│   ├── create_kbo.sql           # DB 초기화 스크립트
│   └── players_all_2025.tsv     # 선수 데이터
│
├── docker-compose.yml            # Docker Compose 설정
├── Dockerfile.fastapi            # FastAPI 이미지
├── Dockerfile.springboot         # Spring Boot 이미지
├── Dockerfile.nginx              # Nginx 이미지
├── init-db.sh                    # DB 초기화 스크립트
├── start.bat                     # 로컬 실행 스크립트 (Windows)
└── package.json                  # Node.js 의존성
```

---

## 🔒 보안 고려사항

- **환경 변수 관리**: `.env` 파일을 통한 민감 정보 분리 (spring-dotenv 사용)
- **API 키 보안**: Git에 업로드되지 않도록 `.gitignore` 설정
- **JWT 토큰 인증**: 
  - 세션리스 인증으로 서버 부하 감소
  - Refresh Token을 통한 보안 강화
  - JJWT 라이브러리 (0.12.6) 사용
- **CORS 정책**: 허용된 도메인만 API 접근 가능
- **비밀번호 암호화**: 
  - BCrypt 알고리즘 사용 (Spring Security Crypto)
  - Salt 자동 생성으로 레인보우 테이블 공격 방어
- **FIDO2 인증**: Passwordless 생체 인증 (지문, 얼굴 인식)
- **이메일 인증**: 회원가입 및 비밀번호 찾기 시 이메일 인증 필수
- **HTTPS**: SSL/TLS 인증서 적용 (Nginx)
- **SQL Injection 방어**: JPA Prepared Statement 자동 사용

---

## 🐛 알려진 이슈 및 제한사항

### 1. Oracle 메모리 사용량
**문제**: t3.medium (4GB RAM) 환경에서 Oracle이 약 2-3GB 메모리 사용

**해결 방법**:
```bash
# Swap 메모리 2GB 추가
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

### 2. 크롤링 안정성
**문제**: KBO 공식 웹사이트 구조 변경 시 Selenium 크롤러 오작동 가능

**해결 방법**:
- CSS Selector 또는 XPath 업데이트
- 백업 데이터(CSV) 기반 복구 시스템 활용
- 에러 로깅 및 알림 시스템 구축

### 3. AI API 비용
**문제**: OpenAI API 사용량에 따른 비용 발생 (GPT-4o, DALL-E 3)

**해결 방법**:
- GPT-4o-mini 모델 우선 사용 (비용 10배 절감)
- 동일 요청에 대한 응답 캐싱
- 사용량 모니터링 및 제한 설정

### 4. 동시 접속자 제한
**문제**: t3.medium 인스턴스의 리소스 제약

**대응 방안**:
- 트래픽 증가 시 t3.large로 스케일업
- Auto Scaling Group 설정 검토
- CloudFront CDN 도입

---

## 📈 향후 개선 계획

### 단기 계획 (1-3개월)
- [ ] **Redis 캐싱 도입**: 자주 조회되는 데이터 캐싱으로 성능 향상
- [ ] **테스트 커버리지 향상**: JUnit, Pytest를 활용한 단위/통합 테스트
- [ ] **로그 모니터링**: ELK Stack 또는 CloudWatch 연동
- [ ] **알림 시스템 강화**: 이메일 알림 템플릿 다양화

### 중기 계획 (3-6개월)
- [ ] **모바일 앱 개발**: React Native로 iOS/Android 앱 출시
- [ ] **추천 시스템**: 협업 필터링 기반 맞춤 콘텐츠 추천
- [ ] **실시간 경기 데이터**: KBO 공식 API 연동 검토
- [ ] **소셜 기능 확장**: 팔로우, 좋아요, 공유 기능
- [ ] **CDN 도입**: 이미지 및 정적 파일 배포 최적화

### 장기 계획 (6개월 이상)
- [ ] **다국어 지원**: i18n을 통한 영어, 일본어 인터페이스
- [ ] **Kubernetes 마이그레이션**: 컨테이너 오케스트레이션 고도화
- [ ] **GraphQL API**: RESTful API와 병행하여 유연한 데이터 조회
- [ ] **블록체인 NFT**: 특별한 순간을 NFT로 발행하는 기능

---

## 📄 라이센스

이 프로젝트는 교육 목적으로 제작되었습니다.

---

## 📞 문의 및 링크

프로젝트에 대한 문의사항이나 피드백은 GitHub Issues를 통해 남겨주세요.

### 🔗 주요 링크
- **Live Demo**: [http://43.200.66.80](http://43.200.66.80)
- **Passwordless 관리자**: [https://52.78.234.231:8143](https://52.78.234.231:8143) (admin/admin)
- **GitHub Repository**: [ict_project_final](https://github.com/yunhyel2/ict_project_final)
- **API 문서**: 
  - Spring Boot Swagger: http://43.200.66.80:8080/swagger-ui/index.html
  - FastAPI Docs: http://43.200.66.80:8020/docs

---

<div align="center">

**Made with ❤️ by MyBall Team**

⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!

### 📊 프로젝트 통계
- **총 코드 라인 수**: 약 20,000+ 라인
- **API 엔드포인트**: 100+ 개
- **React 컴포넌트**: 50+ 개
- **데이터베이스 테이블**: 30+ 개
- **AI 모델 피처**: 27개 (타석 예측)
- **Docker 이미지**: 4개 (Oracle, Spring Boot, FastAPI, Nginx)

</div>

