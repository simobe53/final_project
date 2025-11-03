# -*- coding: utf-8 -*-
from fastapi import FastAPI, HTTPException, Path, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import uvicorn

# FastAPI 애플리케이션 생성
app = FastAPI(
    title="FastAPI 초보 예제",
    description="자주 쓰이는 FastAPI 패턴들",
    version="1.0.0"
)

# CORS 설정 (프론트엔드와 통신을 위해)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Pydantic 모델 정의
class User(BaseModel):
    id: Optional[int] = None
    name: str
    email: str
    age: int

class UserCreate(BaseModel):
    name: str
    email: str
    age: int

# 임시 데이터 저장소
users_db = [
    {"id": 1, "name": "김철수", "email": "kim@example.com", "age": 25},
    {"id": 2, "name": "이영희", "email": "lee@example.com", "age": 30}
]

# 1. 기본 GET 엔드포인트
@app.get("/")
def read_root():
    return {"message": "FastAPI 초보 예제에 오신 것을 환영합니다!"}

# 2. 경로 매개변수 (Path Parameters)
@app.get("/users/{user_id}")
def get_user(user_id: int = Path(..., description="사용자 ID")):
    """특정 사용자 조회"""
    for user in users_db:
        if user["id"] == user_id:
            return user
    raise HTTPException(status_code=404, detail="사용자를 찾을 수 없습니다")

# 3. 쿼리 매개변수 (Query Parameters)
@app.get("/users/")
def get_users(
    skip: int = Query(0, description="건너뛸 개수"),
    limit: int = Query(10, description="가져올 개수"),
    search: Optional[str] = Query(None, description="검색어")
):
    """사용자 목록 조회 (쿼리 매개변수 사용)"""
    result = users_db[skip:skip + limit]
    
    if search:
        result = [user for user in result if search.lower() in user["name"].lower()]
    
    return {"users": result, "total": len(users_db)}

# 4. POST 요청 (Request Body)
@app.post("/users/", response_model=User)
def create_user(user: UserCreate):
    """새 사용자 생성"""
    new_user = {
        "id": len(users_db) + 1,
        "name": user.name,
        "email": user.email,
        "age": user.age
    }
    users_db.append(new_user)
    return new_user

# 5. PUT 요청 (업데이트)
@app.put("/users/{user_id}")
def update_user(user_id: int, user: UserCreate):
    """사용자 정보 수정"""
    for i, existing_user in enumerate(users_db):
        if existing_user["id"] == user_id:
            users_db[i] = {
                "id": user_id,
                "name": user.name,
                "email": user.email,
                "age": user.age
            }
            return {"message": "사용자 정보가 수정되었습니다", "user": users_db[i]}
    
    raise HTTPException(status_code=404, detail="사용자를 찾을 수 없습니다")

# 6. DELETE 요청
@app.delete("/users/{user_id}")
def delete_user(user_id: int):
    """사용자 삭제"""
    for i, user in enumerate(users_db):
        if user["id"] == user_id:
            deleted_user = users_db.pop(i)
            return {"message": "사용자가 삭제되었습니다", "deleted_user": deleted_user}
    
    raise HTTPException(status_code=404, detail="사용자를 찾을 수 없습니다")

# 7. 에러 처리 예제
@app.get("/error-example")
def error_example():
    """에러 처리 예제"""
    raise HTTPException(
        status_code=400,
        detail="이것은 에러 처리 예제입니다"
    )

# 8. 헬스 체크
@app.get("/health")
def health_check():
    return {"status": "healthy", "message": "서버가 정상 작동 중입니다"}

# 서버 실행
if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000)
