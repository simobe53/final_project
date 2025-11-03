# CI/CD 자동 배포 가이드

> **Docker Hub 토큰:** `dckr_pat_CLgHauTuqHSE9u37DyaSai3Hsrk`

---

## 📊 프로젝트 구조

```
myball/ (프론트엔드 + 배포 설정)
├── nginx/default.conf
├── .env (배포용)
├── docker-compose.yml (배포용)
├── Dockerfile.nginx
└── .github/workflows/deploy.yml ✅

myball_springboot/ (백엔드)
├── Dockerfile ✅
├── .env (로컬용)
└── .github/workflows/deploy.yml ✅

myball_FastAPI/ (AI/ML)
├── Dockerfile
├── .env (로컬용)
└── .github/workflows/deploy.yml ✅
```

---

## 🎯 소유자(yunhyel2)가 할 일

### ⏰ 예상 시간: 20분

---

## STEP 1: GitHub Personal Access Token 생성 (5분)

1. https://github.com/settings/tokens 접속
2. **Generate new token (classic)** 클릭
3. 설정:
   ```
   Note: CI/CD for myball
   Expiration: No expiration

   Select scopes (딱 2개만 체크!):
   ☑️ repo (클릭하면 하위 항목 자동 체크됨)
   ☑️ workflow

   ❌ 나머지는 전부 체크 해제!
   ```
4. **Generate token** → 토큰 복사 (ghp_xxxxx...)

⚠️ **한 번만 표시됩니다! 메모장에 저장하세요.**

---

## STEP 2: EC2 SSH 키 복사 (2분)

### myball-key.pem 파일이란?
EC2 서버에 SSH 접속할 때 사용하는 개인키 파일입니다.
이 파일의 **내용**을 GitHub Secrets에 등록해야 합니다.

### 방법 1: 명령어로 보기 (추천)

#### Windows
```cmd
# 파일 위치를 모른다면 먼저 검색
dir myball-key.pem /s

# 파일을 찾았다면 내용 보기
type D:\경로\myball-key.pem
```

#### Mac/Linux
```bash
# 파일 찾기
find ~ -name "myball-key.pem"

# 내용 보기
cat ~/Downloads/myball-key.pem
```

### 방법 2: 메모장으로 보기 (더 쉬움)

```
1. myball-key.pem 파일 찾기
2. 마우스 우클릭 → 연결 프로그램 → 메모장
3. 전체 선택 (Ctrl+A)
4. 복사 (Ctrl+C)
```

### 복사할 내용 예시

```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA...
(여러 줄의 암호화된 텍스트)
...
-----END RSA PRIVATE KEY-----
```

⚠️ **BEGIN부터 END까지 전체 내용을 복사해야 합니다!**

---

## STEP 3: GitHub Secrets 등록 (13분)

### 3-1. myball 레포 (6개)

https://github.com/yunhyel2/myball/settings/secrets/actions

| Name | Value |
|------|-------|
| DOCKERHUB_USERNAME | `ruyahct` |
| DOCKERHUB_TOKEN | `dckr_pat_CLgHauTuqHSE9u37DyaSai3Hsrk` |
| REPO_ACCESS_TOKEN | STEP 1에서 생성한 토큰 |
| EC2_HOST | `43.200.66.80` |
| EC2_USER | `ubuntu` |
| EC2_SSH_KEY | STEP 2에서 복사한 SSH 키 전체 |

---

### 3-2. myball_springboot 레포 (6개)

https://github.com/yunhyel2/myball_springboot/settings/secrets/actions

**동일하게 6개 등록** (위와 같은 값)

---

### 3-3. myball_FastAPI 레포 (5개)

https://github.com/yunhyel2/myball_FastAPI/settings/secrets/actions

**REPO_ACCESS_TOKEN 제외하고 5개 등록**

> 💡 **왜 제외?** FastAPI는 독립적으로 실행되므로 다른 레포와 통신할 필요가 없습니다.

---

## ✅ 완료!

### 자동 배포가 활성화되었습니다! 🎉

**⚠️ 중요: 각 레포의 브랜치가 다릅니다!**
- **myball**: `master` 브랜치에 push
- **myball_springboot**: `main` 브랜치에 push
- **myball_FastAPI**: `main` 브랜치에 push

---

## 📊 배포 흐름

### 프론트엔드 수정 시
```
git push origin master  ← myball은 master!
→ myball Actions (빌드)
→ myball_springboot Actions (자동 트리거)
→ Docker Hub 업로드
→ EC2 배포
```

### 백엔드 수정 시
```
git push origin main  ← springboot은 main!
→ myball_springboot Actions (빌드)
→ Docker Hub 업로드
→ EC2 배포
```

### FastAPI 수정 시
```
git push origin main  ← FastAPI도 main!
→ myball_FastAPI Actions (빌드)
→ Docker Hub 업로드
→ EC2 배포
```

---

## 🔍 배포 상태 확인

### GitHub Actions
- https://github.com/yunhyel2/myball/actions
- https://github.com/yunhyel2/myball_springboot/actions
- https://github.com/yunhyel2/myball_FastAPI/actions

### EC2 서버
```bash
ssh -i myball-key.pem ubuntu@43.200.66.80
cd myball
docker-compose ps
docker-compose logs -f springboot
```

### Docker Hub
- https://hub.docker.com/r/ruyahct/kbo-springboot
- https://hub.docker.com/r/ruyahct/kbo-fastapi
- https://hub.docker.com/r/ruyahct/kbo-nginx

---

## 🐛 문제 해결

### 1. "Error: Invalid token"
→ Secrets에 토큰이 정확히 입력되었는지 확인

### 2. "ssh: Connection timed out"
→ AWS EC2 보안그룹에서 SSH (22번) 포트 확인

### 3. "Repository dispatch event not triggered"
→ REPO_ACCESS_TOKEN 권한 확인 (repo + workflow)

### 4. Actions 실행 안됨
→ 레포 Settings → Actions → "Allow all actions" 확인

---


배포 상태:
https://github.com/yunhyel2/myball/actions
```

---

## 📋 체크리스트

- [ ] GitHub Personal Access Token 생성
- [ ] EC2 SSH 키 복사
- [ ] myball Secrets 6개 등록
- [ ] myball_springboot Secrets 6개 등록
- [ ] myball_FastAPI Secrets 5개 등록
- [ ] GitHub Actions 실행 확인

---

**설정 완료! 🚀**
