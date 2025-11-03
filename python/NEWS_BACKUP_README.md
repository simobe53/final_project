# KBO 뉴스 백업 & 복원 가이드

## 📌 개요

개발 중에는 `application.yaml`의 `ddl-auto: create` 설정으로 인해 Spring Boot 재시작 시 테이블이 초기화됩니다.
뉴스 데이터를 매번 다시 크롤링하지 않도록 CSV 백업 기능을 추가했습니다.

---

## 🔄 워크플로우

### 1. 뉴스 크롤링 (CSV + DB 저장)

```bash
my_venv\Scripts\python.exe python\kbo_news_crawling.py --now
```

**결과:**
- ✅ DB에 90건 저장
- ✅ `python/news_backup/kbo_news_YYYYMMDD_HHMMSS.csv` (타임스탬프 백업)
- ✅ `python/news_backup/kbo_news_latest.csv` (최신 버전)

---

### 2. Spring Boot 재시작 시 (테이블 초기화됨)

```bash
.\start.bat
```

**문제:** `ddl-auto: create`로 인해 `KBO_NEWS` 테이블이 DROP되고 재생성됨 → 데이터 손실

---

### 3. CSV에서 DB 복원

```bash
my_venv\Scripts\python.exe python\restore_news_from_csv.py
```

**결과:**
- ✅ `kbo_news_latest.csv`에서 데이터를 읽어서 DB에 복원
- ✅ 90건의 뉴스가 다시 DB에 저장됨

**특정 백업 파일에서 복원:**
```bash
my_venv\Scripts\python.exe python\restore_news_from_csv.py python/news_backup/kbo_news_20231003_120000.csv
```

---

## 🚀 권장 사용 방법

### 방법 1: 자동화 (start.bat 수정)

`start.bat` 파일에 다음을 추가하면 서버 시작 전 자동으로 뉴스를 복원할 수 있습니다:

```batch
@REM Spring Boot 시작 전에 뉴스 복원
cd /d "%~dp0\python"
call %VENV_PATH%
python restore_news_from_csv.py
cd ..
```

### 방법 2: 수동 복원

1. `start.bat` 실행 → Spring Boot 시작 → 테이블 초기화됨
2. 별도 터미널에서 복원 스크립트 실행:
   ```bash
   my_venv\Scripts\python.exe python\restore_news_from_csv.py
   ```
3. 브라우저 새로고침 → 뉴스 표시됨

---

## 📂 백업 파일 위치

```
python/news_backup/
  ├── kbo_news_latest.csv          (항상 최신 버전)
  ├── kbo_news_20231003_120000.csv (타임스탬프 백업)
  ├── kbo_news_20231003_130000.csv
  └── ...
```

---

## ⚙️ 운영 환경 설정

운영 환경에서는 `application.yaml`을 다음과 같이 변경:

```yaml
jpa:
  hibernate:
    ddl-auto: update  # 또는 validate
```

이렇게 하면 테이블이 초기화되지 않으므로 CSV 복원이 필요 없습니다.

---

## 🔍 문제 해결

### 뉴스가 안 보여요!

1. **DB에 데이터가 있는지 확인:**
   ```bash
   my_venv\Scripts\python.exe -c "import oracledb; oracledb.init_oracle_client(); conn = oracledb.connect('KBO/KBO@localhost:1521/XEPDB1'); cur = conn.cursor(); cur.execute('SELECT COUNT(*) FROM KBO_NEWS'); print(f'뉴스 개수: {cur.fetchone()[0]}건')"
   ```

2. **데이터가 0건이면 복원:**
   ```bash
   my_venv\Scripts\python.exe python\restore_news_from_csv.py
   ```

3. **CSV 백업이 없으면 크롤링:**
   ```bash
   my_venv\Scripts\python.exe python\kbo_news_crawling.py --now
   ```

---

## 💡 팁

- 크롤링은 시간이 오래 걸리므로(약 3-5분) 백업 파일을 잘 보관하세요
- `kbo_news_latest.csv`는 항상 최신 버전으로 유지됩니다
- 타임스탬프 백업 파일은 여러 버전을 보관할 때 유용합니다
- Git에는 CSV 파일을 커밋하지 마세요 (용량이 큽니다)

---

## 📝 .gitignore 추가 권장

```
python/news_backup/
```

