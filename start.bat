@echo off
chcp 65001

REM 1️⃣ 가상환경 경로 설정 (수정하지마시오 무조건 됨)
if "%VENV_PATH%"=="" set VENV_PATH=%~dp0\my_venv\Scripts\activate

REM 2️⃣ 가상환경 활성화
call "%VENV_PATH%"

@REM 환경 확인
@REM java -version
@REM if %errorlevel% neq 0 (
@REM     echo Java 17 이상을 설치해주세요.
@REM     pause
@REM     exit /b 1
@REM )
@REM echo.

@REM node --version
@REM if %errorlevel% neq 0 (
@REM     echo Node.js를 설치해주세요.
@REM     pause
@REM     exit /b 1
@REM )
@REM echo.

@REM python --version
@REM if %errorlevel% neq 0 (
@REM     echo Python을 설치해주세요.
@REM     pause
@REM     exit /b 1
@REM )
@REM echo.

echo Oracle 사용자 KBO 생성 중...
echo exit | sqlplus -S system/ict1234@localhost:1521/XEPDB1 @sql/create_kbo.sql > nul 2>&1

cd /d "%~dp0"
call npm install
if %errorlevel% neq 0 (
    echo React 의존성 설치 실패
    pause
    exit /b 1
)
echo.

cd /d "%~dp0\python"
call %VENV_PATH%
call pip install -r requirements.txt
if %errorlevel% neq 0 (
    echo Python 의존성 설치 실패
    pause
    exit /b 1
)
echo.

REM 로컬 개발 환경용 디렉토리 생성 (배포 환경에서는 볼륨 사용)
if not exist "static_audio" mkdir static_audio

cd ..

@REM 뉴스 데이터 복원 (Spring Boot 시작 전)
echo.
echo 뉴스 데이터 복원 중...
cd /d "%~dp0\python"
call %VENV_PATH%
python restore_news_from_csv.py > nul 2>&1
if %errorlevel% equ 0 (
    echo 뉴스 복원 완료!
) else (
    echo 뉴스 백업 파일 없음 (최초 실행 시 크롤링 필요)
)
cd /d "%~dp0"
echo.

@REM 1)서버 따로따로 cmd 실행 방식
start "Spring Boot Server" cmd /k "cd /d "%~dp0\springboot" && mvnw.cmd clean compile spring-boot:run"


echo Spring Boot 서버 시작 대기 중...
:wait_springboot
netstat -an | findstr ":8080" | findstr "LISTENING" > nul 2>&1
if %errorlevel% neq 0 (
    timeout /t 3 /nobreak > nul
    goto wait_springboot
)
echo Spring Boot 서버가 시작되었습니다!
timeout /t 5 /nobreak > nul

start "React Dev Server" cmd /k "cd /d "%~dp0" && npm run dev"
timeout /t 5 /nobreak > nul

start "FastAPI Server" cmd.exe /k "cd /d "%~dp0\python" && %VENV_PATH% && python FastAPI_server.py"
timeout /t 3 /nobreak > nul

@REM 2)백그라운드 실행 방식
@REM echo Spring Boot 서버 시작 중... (포트: 8080)
@REM cd /d "%~dp0\myball_springboot"
@REM start /B mvnw.cmd clean compile spring-boot:run
@REM cd /d "%~dp0"

@REM echo Spring Boot 서버 시작 대기 중...
@REM :wait_springboot
@REM netstat -an | findstr ":8080" | findstr "LISTENING" > nul 2>&1
@REM if %errorlevel% neq 0 (
@REM     timeout /t 3 /nobreak > nul
@REM     goto wait_springboot
@REM )
@REM echo Spring Boot 서버가 시작되었습니다!
@REM timeout /t 5 /nobreak > nul

@REM echo React 개발 서버 시작 중... (포트: 5173)
@REM cd /d "%~dp0"
@REM start /B npm run dev
@REM timeout /t 5 /nobreak > nul

@REM echo FastAPI 서버 시작 중... (포트: 8020)
@REM cd /d "%~dp0\myball_FastAPI"
@REM call %VENV_PATH%
@REM start /B python FastAPI_server.py
@REM cd /d "%~dp0"
@REM timeout /t 3 /nobreak > nul

echo.
echo ========================================
echo    모든 서비스가 시작되었습니다!
echo ========================================
echo.
echo React 프론트엔드: http://localhost:5173
echo Spring Boot API: http://localhost:8080
echo FastAPI 서버: http://localhost:8020
echo.
echo API 문서:
echo - Spring Boot: http://localhost:8080/swagger-ui.html
echo - FastAPI: http://localhost:8020/docs
echo.
