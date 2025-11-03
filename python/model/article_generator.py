# -*- coding: utf-8 -*-
import os
import re
from typing import Dict, List, Any
from openai import OpenAI
from dotenv import load_dotenv
import oracledb

load_dotenv()
client = OpenAI()

ARTICLES_PER_TEAM = 3
DEFAULT_BIAS_LEVEL = 0.8

# --------------------------
# 보조 함수
# --------------------------
def relay(game: Dict[str, Any]) -> str:
    """경기 기록을 텍스트로 변환"""
    lines = []
    lines.append(f"홈팀: {game['home']['name']} (최종 {game.get('home', {}).get('score', '?')})")
    lines.append(f"원정팀: {game['away']['name']} (최종 {game.get('away', {}).get('score', '?')})")
    lines.append("")
    for inning in game.get('innings', []):
        lines.append(f"== {inning.get('inning', '')} ==")
        for p in inning.get('plays', []):
            lines.append(f"{inning.get('inning', '')} | outs={p.get('outs','')} | {p.get('bases','')} | "
                         f"점수: {p.get('score','')} | 타자: {p.get('batter','')} vs 투수: {p.get('pitcher','')} -> {p.get('result','')}")
        if inning.get('end'):
            lines.append(inning.get('end'))
        lines.append("")
    summary = game.get('summary', {})
    if summary:
        fs = summary.get('final_score', {})
        lines.append("--- 경기 요약 ---")
        lines.append(f"최종 점수: 원정팀 {fs.get('away')} - 홈팀 {fs.get('home')}")
        lines.append(f"결과: {fs.get('result')}")
    return "\n".join(lines)

def build_system_prompt(bias_level: float, tone: str) -> str:
    return (
        "당신은 스포츠 전문 기자 역할을 합니다. 제공된 경기 기록을 바탕으로 특정 팀 관점에서 기사를 작성하세요.\n"
        f"편향 강도: {bias_level:.2f}.\n톤: {tone}.\n"
        "- 명예훼손, 확인되지 않은 범죄/부정행위 금지\n"
        "- 실제 경기 기록(득점/교체/주요 장면) 우선\n"
        "- 기사 구조: 제목(한 줄), 리드(1-2문장), 본문(120~250단어), 마지막에 경기 요약\n"
    )

def build_user_prompt(game_text: str, team_name: str, tone: str, bias_level: float) -> str:
    return (
        f"다음은 경기 기록과 요약입니다:\n\n{game_text}\n\n"
        f"팀 '{team_name}' 관점에서 편파적 기사를 작성하세요.\n"
        f"톤: {tone}, 편향 강도: {bias_level:.2f}.\n"
        "기사 끝에는 별도의 경기 요약을 포함하지 마세요. 기사 본문만 작성하세요.\n"
    )

def safety_check(text: str) -> bool:
    forbid = ["죽여", "성전환", "살해", "강간", "테러", "혐오", "허위사실"]
    low = text.lower()
    return all(f not in low for f in forbid)

def build_summary_block(summary: Dict[str, Any]) -> str:
    """기사 끝에 경기 요약 블록 추가"""
    if not summary: return ""
    fs = summary.get('final_score', {})
    line_score = summary.get('line_score', {})
    stats = summary.get('stats', {})

    lines = []
    lines.append("최종 점수:")
    lines.append(f" 원정팀: {fs.get('away')}점")
    lines.append(f" 홈 팀: {fs.get('home')}점")
    lines.append("")
    lines.append(f"승부 결과: {fs.get('result')}")
    lines.append("")
    if line_score:
        away = line_score.get('away', [])
        home = line_score.get('home', [])
        header = " " + " ".join(str(i+1) for i in range(len(away))) + " R"
        lines.append("이닝별 득점:")
        lines.append(header)
        lines.append(" --------------------------------")
        lines.append("원정 " + " ".join(str(x) for x in away) + " " + str(sum(away)))
        lines.append("홈 " + " ".join(str(x) for x in home) + " " + str(sum(home)))
        lines.append("")
    if stats:
        lines.append("경기 통계:")
        for k,v in stats.items():
            lines.append(f" {k}: {v}")
    return "\n".join(lines)

# --------------------------
# 핵심 함수
# --------------------------
def build_game_summary(game: Dict[str, Any]) -> Dict[str, Any]:
    summary = {}
    innings = game.get("innings", [])
    home_line, away_line = [], []
    prev_home, prev_away = 0, 0

    for inn in innings:
        scores = [p.get("score", "0-0") for p in inn.get("plays", [])]
        if scores:
            last_score = scores[-1]
            away_s, home_s = map(int, last_score.split("-"))
        else:
            away_s, home_s = prev_away, prev_home

        away_line.append(away_s - prev_away)
        home_line.append(home_s - prev_home)

        prev_away, prev_home = away_s, home_s

    # 최종 점수는 line_score의 마지막 누적값 사용
    home_score = prev_home
    away_score = prev_away

    summary["final_score"] = {
        "home": home_score,
        "away": away_score,
        "result": "홈팀 승리" if home_score > away_score
                  else "원정팀 승리" if away_score > home_score
                  else "무승부"
    }
    summary["line_score"] = {"home": home_line, "away": away_line}
    summary["stats"] = {"총 타석 수": sum(len(inn.get("plays", [])) for inn in innings)}
    game["summary"] = summary
    return summary


def generate_articles(game: Dict[str, Any]) -> Dict[str, List[Dict[str, Any]]]:
    relay_data = relay(game)
    final_score = game.get('summary', {}).get('final_score', {})

    if final_score.get('home',0) > final_score.get('away',0):
        winner_key = 'home'
        loser_key = 'away'
    else:
        winner_key = 'away'
        loser_key = 'home'

    results = {winner_key: [], loser_key: []}

    # 승리팀 기사
    for i in range(ARTICLES_PER_TEAM):
        team_name = game[winner_key]['name']
        tone = "열광적, 극찬"
        bias_level = 1.0
        sys_prompt = build_system_prompt(bias_level, tone)
        user_prompt = build_user_prompt(relay_data, team_name, tone, bias_level)
        response = client.chat.completions.create(
            model=os.getenv("BIAS_MODEL", "gpt-4o-mini"),
            messages=[{"role":"system","content":sys_prompt}, {"role":"user","content":user_prompt}],
            temperature=0.9,
        )
        article_text = response.choices[0].message.content.strip()
        if not safety_check(article_text):
            article_text = "[생성 거부] 안전 규정 위반"

        # 제목 추출 (첫 줄)
        title = article_text.splitlines()[0] if article_text else "제목 없음"
        results[winner_key].append({
            "title": title,
            "content": article_text,
            "tone": tone,
            "bias_level": bias_level
        })

    # 패배팀 기사
    for i in range(ARTICLES_PER_TEAM):
        team_name = game[loser_key]['name']
        tone = "격려, 우호적"
        bias_level = 0.8
        sys_prompt = build_system_prompt(bias_level, tone)
        user_prompt = build_user_prompt(relay_data, team_name, tone, bias_level)
        response = client.chat.completions.create(
            model=os.getenv("BIAS_MODEL", "gpt-4o-mini"),
            messages=[{"role":"system","content":sys_prompt}, {"role":"user","content":user_prompt}],
            temperature=0.7,
        )
        article_text = response.choices[0].message.content.strip()
        if not safety_check(article_text):
            article_text = "[생성 거부] 안전 규정 위반"

        title = article_text.splitlines()[0] if article_text else "제목 없음"
        results[loser_key].append({
            "title": title,
            "content": article_text,
            "tone": tone,
            "bias_level": bias_level
        })

    return results

def get_next_id(cursor, sequence_name="SEQ_ARTICLE"):
    cursor.execute(f"SELECT {sequence_name}.NEXTVAL FROM dual")
    return cursor.fetchone()[0]

def save_articles(simulation_id: str, game_log: Dict[str, Any]) -> Dict[str, List[Dict[str, Any]]]:
    if not simulation_id or not game_log:
        raise ValueError("simulation_id와 game_log 필요")

    # 경기 summary 생성
    build_game_summary(game_log)

    # 기사 생성
    articles = generate_articles(game_log)

    # DB 연결
    conn = oracledb.connect(
        user=os.getenv("DB_USER"),
        password=os.getenv("DB_PASSWORD"),
        dsn=f"{os.getenv('DB_HOST')}:{os.getenv('DB_PORT')}/{os.getenv('DB_SERVICENAME')}"
    )
    cursor = conn.cursor()

    for team_key, article_list in articles.items():
        team_name = game_log[team_key]["name"]
        for idx, article in enumerate(article_list, start=1):
            clean_content = re.sub(r'[#()*]', '', article["content"])
            cursor.execute(
                """
                INSERT INTO ARTICLES (ID, SIMULATION_ID, TEAM_NAME, CONTENT)
                VALUES (SEQ_ARTICLE.NEXTVAL,:1, :2, :3)
                """,
                (simulation_id, team_name, clean_content)
            )

    # 5️⃣ 커밋 및 종료
    conn.commit()
    cursor.close()
    conn.close()

    print("\n------ 모든 기사 생성 및 DB 저장 완료 ------")
    return articles