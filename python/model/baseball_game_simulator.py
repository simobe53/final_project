# -*- coding: utf-8 -*-
"""
실제 야구게임 시뮬레이션 시스템
2025년 데이터 기반 완전한 야구 경기 시뮬레이션
"""

import pandas as pd
import numpy as np
import pickle
from collections import defaultdict
import warnings
warnings.filterwarnings('ignore')

class BaseballGameSimulator:
    """야구게임 시뮬레이션 메인 클래스"""
    
    def __init__(self):
        """초기화"""
        self.load_model()
        self.load_stadium_data()
        
    
    def load_model(self):
        """훈련된 AI 모델 로드"""
        print("AI 모델 로딩 중...")
        
        try:
            import os
            # 현재 파일의 디렉토리 경로를 기준으로 모델 파일 경로 설정
            current_dir = os.path.dirname(os.path.abspath(__file__))
            model_path = os.path.join(current_dir, 'trained_model.pkl')
            model_info_path = os.path.join(current_dir, 'model_info.pkl')
            
            with open(model_path, 'rb') as f:
                self.model = pickle.load(f)
            
            with open(model_info_path, 'rb') as f:
                self.model_info = pickle.load(f)
            
            print(f"모델 로드 완료 - {len(self.model_info['classes'])}가지 결과 예측 가능")
        except Exception as e:
            print(f"모델 로딩 실패: {e}")
            raise
    
    
    
    
    
    
    
    
    def create_lineup(self, team_name, pitcher, batters):
        """라인업 구조 생성 {pitcher:{}, batters:[]} 형식"""
        lineup = {
            'team_name': team_name,
            'pitcher': pitcher,
            'batters': batters
        }
        return lineup
    
    def display_lineup(self, lineup, team_type):
        """라인업 표시"""
        print(f"\n{team_type} 라인업: {lineup['team_name']}")
        print("=" * 80)
        
        # 투수 정보
        pitcher = lineup['pitcher']
        hand_kr = "우투" if pitcher['hand'] == 'right' else "좌투"
        print(f"선발투수: {pitcher['name']} ({hand_kr}, {pitcher['age']}세)")
        print(f"   ERA {pitcher['era']:.2f}, WHIP {pitcher['whip']:.2f}, FIP {pitcher['fip']:.2f}, WAR {pitcher['war']:.2f}")
        
        # 타자 정보
        print(f"\n타순:")
        for i, batter in enumerate(lineup['batters'], 1):
            hand_kr = "우타" if batter['hand'] == 'right' else "좌타"
            print(f"  {i}번: {batter['name']} ({batter['position']}, {hand_kr})")
            print(f"      타율 {batter['avg']:.3f}, OPS {batter['ops']:.3f}, 홈런 {batter['hr']}개, 타점 {batter['rbi']}개")
    
    def load_simulation_data(self):
        """시뮬레이션 데이터 JSON 파일 로드"""
        print("시뮬레이션 데이터 로딩 중...")
        
        try:
            import json
            with open('simulation_result.json', 'r', encoding='utf-8') as f:
                self.simulation_data = json.load(f)
            
            print(f"시뮬레이션 데이터 로드 완료")
            print(f"   홈팀: {self.simulation_data['team_info']['homeTeam']}")
            print(f"   어웨이팀: {self.simulation_data['team_info']['awayTeam']}")
            
        except Exception as e:
            print(f"시뮬레이션 데이터 로딩 실패: {e}")
            raise
    
    def set_simulation_data(self, simulation_data):
        """외부에서 받은 시뮬레이션 데이터 설정"""
        print("외부 시뮬레이션 데이터 설정 중...")
        
        try:
            self.simulation_data = simulation_data
            print(f"시뮬레이션 데이터 설정 완료")
            print(f"   홈팀: {self.simulation_data['team_info']['homeTeam']}")
            print(f"   어웨이팀: {self.simulation_data['team_info']['awayTeam']}")
            
        except Exception as e:
            print(f"시뮬레이션 데이터 설정 실패: {e}")
            raise
    
    def load_stadium_data(self):
        """구장 데이터 로드"""
        try:
            import os
            import pandas as pd
            
            # 현재 파일의 디렉토리 경로를 기준으로 구장 파일 경로 설정
            current_dir = os.path.dirname(os.path.abspath(__file__))
            stadium_path = os.path.join(current_dir, 'stadium_info.tsv')
            
            # 구장 정보 로드
            stadium_df = pd.read_csv(stadium_path, sep='\t', encoding='utf-8')
            
            # 팀별 구장 정보 딕셔너리 생성
            self.team_stadiums = {}
            for _, row in stadium_df.iterrows():
                team_code = row['team_code']
                stadium_name = row['stadium_name']
                self.team_stadiums[team_code] = stadium_name
            
            print(f"구장 데이터 로드 완료 - {len(self.team_stadiums)}개 팀")
            
        except Exception as e:
            print(f"구장 데이터 로딩 실패: {e}")
            # 기본 구장 정보 설정
            self.team_stadiums = {
                'LG': '잠실야구장',
                'KT': '수원KT위즈파크',
                'SS': '인천SSG랜더스필드',
                'NC': '창원NC파크',
                'DO': '대구삼성라이온즈파크',
                'KI': '광주기아챔피언스필드',
                'WO': '고척스카이돔',
                'HT': '대전한화생명이글스파크',
                'SK': '인천SSG랜더스필드',
                'OB': '잠실야구장'
            }
    
    def get_stadium_factor(self, team_name):
        """팀별 구장 팩터 반환"""
        # 기본 구장 팩터 (100 = 중립)
        stadium_factors = {
            'LG': 98,   # 잠실 (타자 친화적)
            'KT': 102,  # 수원 (투수 친화적)
            'SS': 100,  # 인천 (중립)
            'NC': 99,   # 창원 (약간 투수 친화적)
            'DO': 101,  # 대구 (약간 투수 친화적)
            'KI': 97,   # 광주 (타자 친화적)
            'WO': 100,  # 고척 (중립)
            'HT': 103,  # 대전 (투수 친화적)
            'SK': 100,  # 인천 (중립)
            'OB': 98    # 잠실 (타자 친화적)
        }
        
        return stadium_factors.get(team_name, 100)
    
    def convert_json_batter(self, json_batter):
        """JSON 타자 데이터를 기존 형식으로 변환"""
        # batting_stats가 없는 경우 기본값 사용
        if 'batting_stats' not in json_batter:
            return {
                'p_no': json_batter.get('p_no', 0),
                'name': json_batter.get('player_name', 'Unknown'),
                'age': 30,
                'position': 'IF',
                'avg': 0.250,
                'obp': 0.320,
                'slg': 0.400,
                'ops': 0.720,
                'hr': 10,
                'rbi': 50,
                'sb': 5,
                'hand': json_batter.get('hand', 'right'),
                'war': 0.0,
                'games': 100,
                'runs': 50,
                'hits': 100
            }
        
        batting_stats = json_batter['batting_stats']
        return {
            'p_no': json_batter['p_no'],
            'name': json_batter['player_name'],
            'age': 30,  # JSON에 나이 정보가 없으므로 기본값
            'position': 'IF',  # JSON에 포지션 정보가 없으므로 기본값
            'avg': batting_stats.get('b_AVG', 0.250),
            'obp': batting_stats.get('b_OBP', 0.320),
            'slg': batting_stats.get('b_SLG', 0.400),
            'ops': batting_stats.get('b_OPS', 0.720),
            'hr': int(batting_stats.get('b_HR', 10)),
            'rbi': int(batting_stats.get('b_RBI', 50)),
            'sb': int(batting_stats.get('b_SB', 5)),
            'hand': json_batter['hand'],
            'war': 0.0,  # JSON에 WAR 정보가 없으므로 기본값
            'games': 100,  # JSON에 경기 수 정보가 없으므로 기본값
            'runs': int(batting_stats.get('b_R', 50)),
            'hits': int(batting_stats.get('b_H', 100))
        }
    
    def convert_json_pitcher(self, json_pitcher):
        """JSON 투수 데이터를 기존 형식으로 변환"""
        pitching_stats = json_pitcher['pitching_stats']
        return {
            'p_no': json_pitcher['p_no'],
            'name': json_pitcher['player_name'],
            'age': 30,  # JSON에 나이 정보가 없으므로 기본값
            'era': pitching_stats['p_ERA'],
            'whip': pitching_stats['p_WHIP'],
            'wins': int(pitching_stats['p_W']),
            'losses': int(pitching_stats['p_L']),
            'games_started': 10,  # JSON에 선발 정보가 없으므로 기본값
            'innings': pitching_stats['p_IP'],
            'strikeouts': int(pitching_stats['p_SO']),
            'hand': json_pitcher['hand'],
            'war': 0.0,  # JSON에 WAR 정보가 없으므로 기본값
            'fip': pitching_stats['p_FIP']
        }
    
    def setup_game(self):
        """게임 설정 - JSON 파일의 라인업 사용"""
        print("\n야구게임 시뮬레이션 설정")
        print("=" * 60)
        
        # 시뮬레이션 데이터 로드
        self.load_simulation_data()
        
        return self._setup_lineups()
    
    def setup_game_from_data(self, simulation_data):
        """외부 데이터로 게임 설정"""
        print("\n야구게임 시뮬레이션 설정 (외부 데이터)")
        print("=" * 60)
        
        # 외부 시뮬레이션 데이터 설정
        self.set_simulation_data(simulation_data)
        
        return self._setup_lineups()
    
    def _setup_lineups(self):
        """라인업 설정 공통 로직"""
        
        # 팀 정보
        home_team = self.simulation_data['team_info']['homeTeam']
        away_team = self.simulation_data['team_info']['awayTeam']
        
        print(f"홈팀: {home_team}")
        print(f"어웨이팀: {away_team}")
        
        # 홈팀 라인업 구성
        print(f"\n{home_team} 라인업 구성 중...")
        home_data = self.simulation_data['player_stats']['homeLineup']
        
        # 홈팀 투수 변환
        home_pitcher = self.convert_json_pitcher(home_data['pitcher'])
        
        # 홈팀 타자들 변환 (batting1~batting9 순서대로)
        home_batters = []
        for i in range(1, 10):  # batting1부터 batting9까지
            batting_key = f'batting{i}'
            if batting_key in home_data:
                home_batters.append(self.convert_json_batter(home_data[batting_key]))
        
        if not home_batters:
            print(f"{home_team} 타자 데이터가 부족합니다.")
            return None, None
        
        home_lineup = self.create_lineup(home_team, home_pitcher, home_batters)
        
        # 어웨이팀 라인업 구성
        print(f"{away_team} 라인업 구성 중...")
        away_data = self.simulation_data['player_stats']['awayLineup']
        
        # 어웨이팀 투수 변환
        away_pitcher = self.convert_json_pitcher(away_data['pitcher'])
        
        # 어웨이팀 타자들 변환 (batting1~batting9 순서대로)
        away_batters = []
        for i in range(1, 10):  # batting1부터 batting9까지
            batting_key = f'batting{i}'
            if batting_key in away_data:
                away_batters.append(self.convert_json_batter(away_data[batting_key]))
        
        if not away_batters:
            print(f"{away_team} 타자 데이터가 부족합니다.")
            return None, None
        
        away_lineup = self.create_lineup(away_team, away_pitcher, away_batters)
        
        # 최종 라인업 표시
        print("\n" + "="*80)
        print("최종 라인업")
        print("="*80)
        
        self.display_lineup(home_lineup, "홈팀")
        self.display_lineup(away_lineup, "어웨이팀")
        
        return home_lineup, away_lineup
    
    def run_simulation_from_data(self, simulation_data):
        """❌ 더 이상 사용하지 않음 - 실시간 타석별 시뮬레이션으로 대체됨"""
        return {
            "error": "deprecated",
            "message": "전체 게임 시뮬레이션은 더 이상 사용되지 않습니다.",
            "recommendation": "predict_at_bat_result() 메서드를 사용해주세요."
        }
    
    def feature_engineering(self, df):
        """피처 엔지니어링 적용"""
        # 타자 지표 생성
        df['b_double_rate'] = df['b_2B'] / df['b_ePA'].replace(0, np.nan)
        df['b_triple_rate'] = df['b_3B'] / df['b_ePA'].replace(0, np.nan)
        df['b_hr_rate'] = df['b_HR'] / df['b_ePA'].replace(0, np.nan)
        df['b_hp_rate'] = df['b_HP'] / df['b_ePA'].replace(0, np.nan)
        df['b_gdp_rate'] = df['b_GDP'] / df['b_ePA'].replace(0, np.nan)
        df['b_sf_rate'] = df['b_SF'] / df['b_ePA'].replace(0, np.nan)
        df['b_rbi_rate'] = df['b_RBI'] / df['b_ePA'].replace(0, np.nan)
        df['b_run_rate'] = df['b_SB'] / df['b_ePA'].replace(0, np.nan)
        df['b_so_rate'] = df['b_SO'] / df['b_ePA'].replace(0, np.nan)
        
        # 투수 지표 생성
        df['p_2b_per_inning'] = df['p_2B'] / df['p_IP'].replace(0, np.nan)
        df['p_3b_per_inning'] = df['p_3B'] / df['p_IP'].replace(0, np.nan)
        df['p_hr_per_inning'] = df['p_HR'] / df['p_IP'].replace(0, np.nan)
        df['p_hp_per_inning'] = df['p_HP'] / df['p_IP'].replace(0, np.nan)
        df['p_so_per_inning'] = df['p_SO'] / df['p_IP'].replace(0, np.nan)
        df['p_roe_per_inning'] = df['p_ROE'] / df['p_IP'].replace(0, np.nan)
        
        return df
    
    
    def create_prediction_input(self, inning, outs, runners, batter_info, pitcher_info, location='p_home', stadium_factor=100):
        """AI 모델 예측을 위한 입력 데이터 생성"""
        
        # JSON 데이터에서 직접 통계 사용
        batter_stats = batter_info
        pitcher_stats = pitcher_info
        
        # 투타 상성 결정
        batter_hand = batter_stats['hand']
        pitcher_hand = pitcher_stats['hand']
        
        if batter_hand == 'right' and pitcher_hand == 'left':
            hand = 'br_pl'
        elif batter_hand == 'left' and pitcher_hand == 'right':
            hand = 'bl_pr'
        elif batter_hand == 'right' and pitcher_hand == 'right':
            hand = 'br_pr'
        else:  # left-left
            hand = 'bl_pl'
        
        # 주자 상황 텍스트 변환
        if not runners:
            runners_text = "주자 없음"
        elif len(runners) == 1:
            runners_text = runners[0]
        elif len(runners) == 2:
            if "1루" in runners and "2루" in runners:
                runners_text = "1,2루"
            elif "1루" in runners and "3루" in runners:
                runners_text = "1,3루"
            elif "2루" in runners and "3루" in runners:
                runners_text = "2,3루"
        elif len(runners) == 3:
            runners_text = "만루"
        else:
            runners_text = "주자 없음"
        
        # 아웃 카운트 텍스트 변환
        outs_text = f"{outs}사" if outs > 0 else "무사"
        
        # 예측 입력 데이터 생성 (JSON 데이터 구조에 맞게)
        input_data = {
            'inning': f"{inning}회",
            'Prev_Outs': outs_text,
            'Prev_Runners': runners_text,
            'location': location,
            'hand': hand,
            
            # 타자 기본 통계 (JSON 구조)
            'b_ePA': batter_stats.get('batting_stats', {}).get('b_ePA', 400),
            'b_AB': 400,  # 기본값
            'b_H': batter_stats.get('batting_stats', {}).get('b_H', 100),
            'b_2B': batter_stats.get('batting_stats', {}).get('b_2B', 20),
            'b_3B': batter_stats.get('batting_stats', {}).get('b_3B', 2),
            'b_HR': batter_stats.get('batting_stats', {}).get('b_HR', 15),
            'b_RBI': batter_stats.get('batting_stats', {}).get('b_RBI', 60),
            'b_SB': batter_stats.get('batting_stats', {}).get('b_SB', 5),
            'b_BB': batter_stats.get('batting_stats', {}).get('b_BB', 50),
            'b_HP': batter_stats.get('batting_stats', {}).get('b_HP', 3),
            'b_SO': batter_stats.get('batting_stats', {}).get('b_SO', 80),
            'b_GDP': batter_stats.get('batting_stats', {}).get('b_GDP', 8),
            'b_SF': batter_stats.get('batting_stats', {}).get('b_SF', 5),
            'b_AVG': batter_stats.get('batting_stats', {}).get('b_AVG', 0.250),
            'b_OBP': batter_stats.get('batting_stats', {}).get('b_OBP', 0.320),
            'b_SLG': batter_stats.get('batting_stats', {}).get('b_SLG', 0.400),
            'b_OPS': batter_stats.get('batting_stats', {}).get('b_OPS', 0.720),
            
            # 투수 기본 통계 (JSON 구조)
            'p_IP': pitcher_stats.get('pitching_stats', {}).get('p_IP', 100),
            'p_ER': 30,  # 기본값
            'p_H': pitcher_stats.get('pitching_stats', {}).get('p_H', 80),
            'p_2B': pitcher_stats.get('pitching_stats', {}).get('p_2B', 15),
            'p_3B': pitcher_stats.get('pitching_stats', {}).get('p_3B', 2),
            'p_HR': pitcher_stats.get('pitching_stats', {}).get('p_HR', 10),
            'p_BB': pitcher_stats.get('pitching_stats', {}).get('p_BB', 30),
            'p_HP': pitcher_stats.get('pitching_stats', {}).get('p_HP', 3),
            'p_SO': pitcher_stats.get('pitching_stats', {}).get('p_SO', 80),
            'p_ROE': pitcher_stats.get('pitching_stats', {}).get('p_ROE', 5),
            'p_ERA': pitcher_stats.get('pitching_stats', {}).get('p_ERA', 3.50),
            'p_FIP': pitcher_stats.get('pitching_stats', {}).get('p_FIP', 3.50),
            'p_WHIP': pitcher_stats.get('pitching_stats', {}).get('p_WHIP', 1.20),
            
            # 구장 팩터 (홈팀 구장 기준)
            'PF': stadium_factor
        }
        
        return input_data
    
    def predict_at_bat_result(self, inning, outs, runners, batter_info, pitcher_info, location='p_home', home_team_name=None):
        """타석 결과 예측"""
        
        # 구장 팩터 기본값
        stadium_factor = 100
        
        # 예측 입력 데이터 생성
        input_data = self.create_prediction_input(inning, outs, runners, batter_info, pitcher_info, location, stadium_factor)
        
        if input_data is None:
            return None, None
        
        # DataFrame으로 변환
        input_df = pd.DataFrame([input_data])
        
        # 피처 엔지니어링 적용
        input_df = self.feature_engineering(input_df)
        
        # 모델이 요구하는 피처만 선택
        feature_names = self.model_info['feature_names']
        available_features = [col for col in feature_names if col in input_df.columns]
        X = input_df[available_features]
        
        # 범주형 변수 처리
        categorical_features = self.model_info['categorical_features']
        for col in categorical_features:
            if col in X.columns:
                X[col] = X[col].astype('category')
        
        # 예측 수행
        prediction = self.model.predict(X)
        probabilities = self.model.predict_proba(X)
        
        # 몬테카를로 시뮬레이션으로 결과 선택
        classes = self.model_info['classes']
        probs = probabilities[0]
        
        # 확률에 따라 랜덤하게 결과 선택 (몬테카를로 시뮬레이션)
        monte_carlo_result = np.random.choice(classes, p=probs)
        
        # 결과 반환
        result_probabilities = dict(zip(classes, probs))
        
        return monte_carlo_result, result_probabilities

class GameState:
    """게임 상태 관리 클래스"""
    
    def __init__(self):
        """게임 상태 초기화"""
        self.inning = 1
        self.half = "초"  # "초" 또는 "말"
        self.outs = 0
        self.runners = []  # 주자 위치: ["1루", "2루", "3루"] 형태
        self.base_runners = {}  # 베이스별 주자 정보: {"1루": p_no, "2루": p_no, "3루": p_no}
        self.home_score = 0
        self.away_score = 0
        self.current_batter_idx = 0  # 현재 타자 순번 (0-8)
        self.game_over = False
        self.winner = None
        
        # 경기 기록
        self.play_by_play = []
        self.inning_scores = {"home": [0]*9, "away": [0]*9}
        
    def get_inning_text(self):
        """현재 이닝 텍스트 반환"""
        return f"{self.inning}회{self.half}"
    
    def get_runners_text(self):
        """주자 상황 텍스트 반환"""
        if not self.runners:
            return "주자 없음"
        elif len(self.runners) == 1:
            return self.runners[0]
        elif len(self.runners) == 2:
            if "1루" in self.runners and "2루" in self.runners:
                return "1,2루"
            elif "1루" in self.runners and "3루" in self.runners:
                return "1,3루"
            elif "2루" in self.runners and "3루" in self.runners:
                return "2,3루"
        elif len(self.runners) == 3:
            return "만루"
        return "주자 없음"
    
    def get_outs_text(self):
        """아웃 카운트 텍스트 반환"""
        return f"{self.outs}사" if self.outs > 0 else "무사"
    
    def add_runner(self, base, p_no=None):
        """주자 추가"""
        if base not in self.runners:
            self.runners.append(base)
            self.runners.sort(key=lambda x: {"1루": 1, "2루": 2, "3루": 3}[x])
        if p_no is not None:
            self.base_runners[base] = p_no
    
    def remove_runner(self, base):
        """주자 제거"""
        if base in self.runners:
            self.runners.remove(base)
        if base in self.base_runners:
            del self.base_runners[base]
    
    def advance_runners(self, bases):
        """주자 진루 (bases만큼)"""
        new_runners = []
        new_base_runners = {}
        runs_scored = 0
        
        base_values = {"1루": 1, "2루": 2, "3루": 3}
        
        for runner in self.runners:
            current_base = base_values[runner]
            new_base = current_base + bases
            p_no = self.base_runners.get(runner)
            
            if new_base >= 4:  # 홈으로 득점
                runs_scored += 1
            elif new_base == 1:
                new_runners.append("1루")
                if p_no:
                    new_base_runners["1루"] = p_no
            elif new_base == 2:
                new_runners.append("2루")
                if p_no:
                    new_base_runners["2루"] = p_no
            elif new_base == 3:
                new_runners.append("3루")
                if p_no:
                    new_base_runners["3루"] = p_no
        
        self.runners = new_runners
        self.base_runners = new_base_runners
        return runs_scored
    
    def process_result(self, result, batter_p_no=None):
        """타석 결과 처리"""
        runs_scored = 0
        
        if result == "1루타":
            runs_scored = self.advance_runners(1)
            self.add_runner("1루", batter_p_no)
            
        elif result == "2루타":
            runs_scored = self.advance_runners(2)
            self.add_runner("2루", batter_p_no)
            
        elif result == "3루타":
            runs_scored = self.advance_runners(3)
            self.add_runner("3루", batter_p_no)
            
        elif result == "홈런":
            runs_scored = len(self.runners) + 1  # 주자들 + 타자
            self.runners = []
            self.base_runners = {}
            
        elif result == "4구":
            # 볼넷 - 1루 진출, 주자들은 밀려나는 경우만 진루
            if "1루" in self.runners:
                if "2루" in self.runners:
                    if "3루" in self.runners:
                        # 만루 - 1점 득점
                        runs_scored = 1
                    else:
                        # 1,2루 - 3루로 진루 (2루 주자 이동)
                        runner_p_no = self.base_runners.get("2루")
                        self.remove_runner("2루")
                        self.add_runner("3루", runner_p_no)
                else:
                    # 1루만 - 2루로 진루 (1루 주자 이동)
                    runner_p_no = self.base_runners.get("1루")
                    self.remove_runner("1루")
                    self.add_runner("2루", runner_p_no)
            self.add_runner("1루", batter_p_no)
            
        elif result == "사구":
            # 몸에 맞는 볼 - 4구와 동일
            if "1루" in self.runners:
                if "2루" in self.runners:
                    if "3루" in self.runners:
                        runs_scored = 1
                    else:
                        self.add_runner("3루")
                else:
                    self.remove_runner("1루")
                    self.add_runner("2루")
            self.add_runner("1루", batter_p_no)
            
        elif result in ["플라이 아웃", "땅볼 아웃", "직선타 아웃"]:
            self.outs += 1
            
        elif result == "삼진":
            self.outs += 1
            
        elif result == "병살타 아웃":
            self.outs += 2
            # 1루 주자 제거 (병살)
            if "1루" in self.runners:
                self.remove_runner("1루")
            
        elif result == "희생플라이 아웃":
            self.outs += 1
            # 3루 주자가 있으면 득점
            if "3루" in self.runners:
                runs_scored = 1
                self.remove_runner("3루")
                
        elif result == "내야 안타":
            runs_scored = self.advance_runners(1)
            self.add_runner("1루", batter_p_no)
            
        elif result == "실책 출루":
            self.add_runner("1루", batter_p_no)
            
        elif result == "선행주자아웃 출루":
            # 주자 하나 아웃, 타자는 1루 진출
            if self.runners:
                self.runners.pop()  # 가장 앞선 주자 아웃
            self.add_runner("1루", batter_p_no)
        
        # 득점 추가
        if self.half == "초":  # 어웨이팀 공격
            self.away_score += runs_scored
            self.inning_scores["away"][self.inning-1] += runs_scored
        else:  # 홈팀 공격
            self.home_score += runs_scored
            self.inning_scores["home"][self.inning-1] += runs_scored
        
        return runs_scored
    
    def next_batter(self):
        """다음 타자로 변경"""
        # 안전성 검사: current_batter_idx가 9 이상인 경우 리셋
        if self.current_batter_idx >= 9:
            print(f"❌ 타자 인덱스 오류: {self.current_batter_idx} >= 9")
            self.current_batter_idx = 0
        
        # 다음 타자로 이동 (0-8 범위에서 순환)
        self.current_batter_idx = (self.current_batter_idx + 1) % 9
    
    def change_half(self):
        """이닝 교체 (초->말, 말->다음이닝초)"""
        if self.half == "초":
            self.half = "말"
        else:
            self.half = "초"
            self.inning += 1
        
        # 이닝 교체시 초기화 (타순은 유지)
        self.outs = 0
        self.runners = []
        self.base_runners = {}
        # current_batter_idx는 유지 - 타순이 이어져야 함
    
    def check_game_end(self):
        """게임 종료 조건 확인 (연장 없음, 9회 종료 시 동점이면 무승부)"""
        # 9회초가 끝나고 9회말 시작 전 - 홈팀이 앞서고 있을 때만 게임 종료
        if self.inning == 9 and self.half == "말" and self.outs == 0:
            if self.home_score > self.away_score:
                self.game_over = True
                self.winner = "홈팀"
                print(f">>> 9회초 종료! 홈팀 승리로 9회말 없음! 최종 스코어: 어웨이 {self.away_score} - {self.home_score} 홈")
            # 어웨이팀이 앞서거나 동점이면 9회말 진행
            return

        # 10회초가 되려고 하면 (즉, 9회말이 끝남) 게임 종료
        if self.inning >= 10:
            self.game_over = True
            if self.home_score > self.away_score:
                self.winner = "홈팀"
            elif self.away_score > self.home_score:
                self.winner = "어웨이팀"
            else:
                self.winner = "무승부"
            print(f">>> 9회 종료! 최종 스코어: 어웨이 {self.away_score} - {self.home_score} 홈 ({self.winner})")
            return
    
    def display_status(self):
        """현재 게임 상황 표시"""
        print(f"\n" + "="*60)
        print(f"{self.get_inning_text()} {self.get_outs_text()} {self.get_runners_text()}")
        print(f"스코어: 어웨이 {self.away_score} - {self.home_score} 홈")
        
        # 주자 상황 시각화
        if self.runners:
            print(f"주자: {', '.join(self.runners)}")
        
        print("="*60)
    
    def add_play_record(self, batter_info, pitcher_info, result, runs_scored, prev_base_runners=None):
        """플레이 기록 추가"""
        # 이전 타석의 아웃 수 저장 (JSON 변환시 사용)
        prev_outs = 0
        if self.play_by_play:
            prev_outs = self.play_by_play[-1]['outs']
        
        # 현재 타석 이전의 베이스 정보 저장
        if prev_base_runners is None:
            prev_base_runners = {}
        
        record = {
            'inning': self.inning,
            'half': self.half,
            'outs': self.outs,
            'prev_outs': prev_outs,  # 이전 타석의 아웃 수 추가
            'runners': self.runners.copy(),
            'base_runners': self.base_runners.copy(),  # 현재 타석 이후의 베이스 정보
            'prev_base_runners': prev_base_runners,    # 현재 타석 이전의 베이스 정보
            'batter': batter_info,
            'pitcher': pitcher_info,
            'result': result,
            'runs_scored': runs_scored,
            'score_after': {'away': self.away_score, 'home': self.home_score}
        }
        self.play_by_play.append(record)

    def simulate_full_game(self, simulator, home_lineup, away_lineup):
        """❌ 더 이상 사용하지 않는 전체 게임 시뮬레이션 - 실시간 타석별 시뮬레이션으로 대체됨"""
        print("\n야구 경기 시뮬레이션 시작!")
        print("=" * 60)
        
        # 홈팀 구장 정보 표시
        home_team_name = home_lineup['team_name']
        stadium_name = simulator.team_stadiums.get(home_team_name, "알 수 없음")
        stadium_factor = simulator.get_stadium_factor(home_team_name)
        
        print(f"경기장: {stadium_name} (구장팩터: {stadium_factor})")
        print(f"홈팀: {home_team_name}")
        print(f"어웨이팀: {away_lineup['team_name']}")
        print("=" * 60)
        
        while not self.game_over:
            # 3아웃이면 이닝 교체
            if self.outs >= 3:
                print(f"\n" + "="*30)
                print(f"{self.get_inning_text()} 종료 - 3아웃 체인지!")
                print(f"현재 스코어: 어웨이 {self.away_score} - {self.home_score} 홈")
                print("="*30)
                
                # 이닝별 스코어보드 표시 (3회, 6회, 9회에만)
                if self.inning in [3, 6, 9]:
                    self.display_scoreboard(home_team_name, away_lineup['team_name'])
                
                self.change_half()
                
                # 게임 종료 조건 확인
                self.check_game_end()
                if self.game_over:
                    break
                
                print(f"\n" + ">"*30)
                print(f"{self.get_inning_text()} 시작!")
                print(">"*30)
            
            # 현재 공격팀과 수비팀 결정
            if self.half == "초":  # 어웨이팀 공격
                batting_lineup = away_lineup
                pitching_lineup = home_lineup
                location = 'p_home'  # 홈팀이 수비
            else:  # 홈팀 공격
                batting_lineup = home_lineup
                pitching_lineup = away_lineup
                location = 'p_away'  # 어웨이팀이 수비
            
            # 현재 타자와 투수 정보
            # 안전성 검사: current_batter_idx가 배열 범위를 벗어나지 않도록
            if self.current_batter_idx >= len(batting_lineup['batters']):
                print(f"❌ 타자 인덱스 오류: {self.current_batter_idx} >= {len(batting_lineup['batters'])}")
                self.current_batter_idx = 0  # 인덱스 리셋
            
            current_batter = batting_lineup['batters'][self.current_batter_idx]
            current_pitcher = pitching_lineup['pitcher']
            
            # 게임 상황 표시
            self.display_status()
            
            # 현재 타자 정보 상세 표시
            batter_hand = "우타" if current_batter['hand'] == 'right' else "좌타"
            pitcher_hand = "우투" if current_pitcher['hand'] == 'right' else "좌투"
            
            print(f"타자: {current_batter['name']} ({current_batter['position']}, {batter_hand})")
            print(f"   타율 {current_batter['avg']:.3f}, OPS {current_batter['ops']:.3f}, 홈런 {current_batter['hr']}개")
            
            print(f"투수: {current_pitcher['name']} ({pitcher_hand})")
            print(f"   ERA {current_pitcher['era']:.2f}, WHIP {current_pitcher['whip']:.2f}")
            
            # 투타 상성 표시
            if (current_batter['hand'] == 'right' and current_pitcher['hand'] == 'left') or \
               (current_batter['hand'] == 'left' and current_pitcher['hand'] == 'right'):
                print("투타 상성: 유리한 매치업!")
            else:
                print("투타 상성: 일반적인 매치업")
            
            # AI 모델로 타석 결과 예측 (홈팀 구장 정보 포함)
            predicted_result, probabilities = simulator.predict_at_bat_result(
                self.inning, self.outs, self.runners, 
                current_batter, current_pitcher, location, home_team_name
            )
            
            if predicted_result is None:
                print("예측 실패 - 기본 결과 적용")
                predicted_result = "플라이 아웃"
            
            # 이전 타석의 베이스 정보 저장 (현재 타석 이전의 베이스 정보)
            prev_base_runners = self.base_runners.copy()
            
            # 결과 처리 (베이스 주자 이동)
            runs_scored = self.process_result(predicted_result, current_batter['p_no'])
            
            # 결과 표시 (numpy array 문제 해결)
            result_emoji = {
                "홈런": "*", "3루타": "+", "2루타": "=", "1루타": "-",
                "4구": "W", "사구": "H", "삼진": "K", 
                "플라이 아웃": "F", "땅볼 아웃": "G", "병살타 아웃": "DP",
                "내야 안타": "I", "실책 출루": "E", "선행주자아웃 출루": "FC",
                "직선타 아웃": "L", "희생플라이 아웃": "SF"
            }
            
            # numpy array를 문자열로 변환
            if isinstance(predicted_result, (list, np.ndarray)):
                predicted_result = str(predicted_result[0]) if len(predicted_result) > 0 else "플라이 아웃"
            
            emoji = result_emoji.get(predicted_result, "B")
            print(f"\n" + "-"*60)
            print(f"{emoji} 타석 결과: {predicted_result}")
            
            if runs_scored > 0:
                print(f"{runs_scored}점 득점! 현재 스코어: 어웨이 {self.away_score} - {self.home_score} 홈")
            
            # 상위 5개 확률 표시
            sorted_probs = sorted(probabilities.items(), key=lambda x: x[1], reverse=True)
            print(f"\nAI 예측 확률 (몬테카를로 시뮬레이션):")
            for i, (result_name, prob) in enumerate(sorted_probs[:5], 1):
                if result_name == predicted_result:
                    print(f"> {i}. {result_name}: {prob:.1%} <- 선택됨!")
                else:
                    print(f"   {i}. {result_name}: {prob:.1%}")
            
            # 몬테카를로 시뮬레이션 설명
            highest_prob_result = sorted_probs[0][0]
            if predicted_result != highest_prob_result:
                print(f"가장 높은 확률: {highest_prob_result} ({sorted_probs[0][1]:.1%})")
                print(f"   하지만 몬테카를로 시뮬레이션으로 {predicted_result} 선택!")
            
            print("-"*60)
            
            # 플레이 기록 추가 (이전 베이스 정보 포함)
            self.add_play_record(current_batter, current_pitcher, predicted_result, runs_scored, prev_base_runners)
            
            # 다음 타자로 변경 (아웃이 아닌 경우에만)
            if predicted_result not in ["플라이 아웃", "땅볼 아웃", "직선타 아웃", "삼진", "병살타 아웃", "희생플라이 아웃"]:
                self.next_batter()
            else:
                self.next_batter()  # 아웃이어도 다음 타자로
            
            # 잠시 대기 (시뮬레이션 속도 조절)
            import time
            # time.sleep(1.0)  # 빠른 진행을 위해 주석 처리
            
            # 타석 완료 후 간단한 구분선
            if self.outs < 3:
                print("\n" + "."*40 + " 다음 타자 " + "."*40)
        
        # 게임 종료
        self.display_final_result(home_team_name, away_lineup['team_name'])
        
        # 경기 기록 저장 (구장 정보 포함)
        stadium_name = simulator.team_stadiums.get(home_team_name, "잠실")
        log_file = self.save_game_log(home_team_name=home_team_name, away_team_name=away_lineup['team_name'], stadium_name=stadium_name)
        print(f"경기 기록 저장: {log_file}")
        
        return self.play_by_play
    
    def display_scoreboard(self, home_team, away_team):
        """이닝별 스코어보드 표시"""
        print("\n이닝별 스코어보드")
        print("=" * 60)
        print(f"{'이닝':<6}", end="")
        for i in range(1, 10):
            print(f"{i:>4}", end="")
        print(f"{'R':>4}")  # Runs
        print("-" * 60)
        
        # 어웨이팀 (초)
        print(f"초 {away_team[:8]:<6}", end="")
        for i in range(9):
            print(f"{self.inning_scores['away'][i]:>4}", end="")
        print(f"{self.away_score:>4}")
        
        # 홈팀 (말)
        print(f"말 {home_team[:8]:<6}", end="")
        for i in range(9):
            print(f"{self.inning_scores['home'][i]:>4}", end="")
        print(f"{self.home_score:>4}")
        print("=" * 60)

    def display_final_result(self, home_team, away_team):
        """최종 결과 표시"""
        print("\n" + "="*40)
        print("=" + " "*15 + "경기 종료!" + " "*15 + "=")
        print("="*40)
        
        # 이닝별 스코어보드 표시
        self.display_scoreboard(home_team, away_team)
        
        print(f"\n최종 스코어:")
        print(f"   {away_team}: {self.away_score}점")
        print(f"   {home_team}: {self.home_score}점")
        
        if self.winner:
            if self.winner == "홈팀":
                print(f"\n승리: 홈팀 승리! ({self.home_score}-{self.away_score})")
            else:
                print(f"\n승리: 어웨이팀 승리! ({self.away_score}-{self.home_score})")
        else:
            print(f"\n무승부 ({self.away_score}-{self.home_score})")
        
        # 상세한 경기 요약
        total_hits = sum(1 for record in self.play_by_play 
                        if record['result'] in ['1루타', '2루타', '3루타', '홈런', '내야 안타'])
        total_strikeouts = sum(1 for record in self.play_by_play 
                              if record['result'] == '삼진')
        total_walks = sum(1 for record in self.play_by_play 
                         if record['result'] in ['4구', '사구'])
        total_homeruns = sum(1 for record in self.play_by_play 
                            if record['result'] == '홈런')
        
        print(f"\n경기 통계:")
        print(f"   총 타석: {len(self.play_by_play)}개")
        print(f"   총 안타: {total_hits}개")
        print(f"   총 홈런: {total_homeruns}개")
        print(f"   총 삼진: {total_strikeouts}개")
        print(f"   총 볼넷: {total_walks}개")
        print(f"   총 이닝: {self.inning}이닝")
        
        print("\n" + "="*40)
    
    def save_game_log(self, filename=None, home_team_name=None, away_team_name=None, stadium_name="잠실"):
        """경기 기록 저장 - 요청된 JSON 구조로 변환"""
        if filename is None:
            from datetime import datetime
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            filename = f"game_log_{timestamp}.json"
        
        import json
        
        # 이닝별로 데이터 구조화
        structured_data = self.convert_to_structured_format(home_team_name, away_team_name, stadium_name)
        
        with open(filename, 'w', encoding='utf-8') as f:
            json.dump(structured_data, f, ensure_ascii=False, indent=2)
        
        print(f"경기 기록 저장: {filename}")
        return filename
    
    def convert_to_structured_format(self, home_team_name=None, away_team_name=None, stadium_name="잠실"):
        """play_by_play 데이터를 요청된 JSON 구조로 변환"""
        
        # 이닝별로 데이터 그룹화
        innings_data = {}
        
        for record in self.play_by_play:
            inning = record['inning']
            half = record['half']
            
            if inning not in innings_data:
                innings_data[inning] = {'top': {'events': []}, 'bottom': {'events': []}}
            
            # 이전 상황 계산 (이전 점수와 베이스 상황)
            prev_score = {'home': 0, 'away': 0}
            prev_base = {'1루': None, '2루': None, '3루': None}
            prev_outs = 0
            
            # 현재 기록의 인덱스 찾기
            current_index = self.play_by_play.index(record)
            
            # 이전 기록에서 상황 찾기
            if current_index > 0:
                prev_record = self.play_by_play[current_index - 1]
                prev_score = prev_record['score_after']
                
                # 이닝이 바뀌면 아웃 수를 0으로 초기화
                if (prev_record['inning'] != inning or prev_record['half'] != half):
                    prev_outs = 0
                else:
                    # 같은 이닝/하프면 이전 타석의 현재 아웃 수 사용
                    prev_outs = prev_record.get('outs', 0)
                
                # 베이스 주자 정보 사용 (현재 타석 이전의 베이스 정보)
                # 이닝이 바뀌었으면 주자 정보 초기화
                if current_index > 0:
                    prev_record = self.play_by_play[current_index - 1]
                    # 이닝이 바뀌었으면 빈 베이스
                    if (prev_record['inning'] != inning or prev_record['half'] != half):
                        prev_base = {'1루': None, '2루': None, '3루': None}
                    else:
                        # 같은 이닝/하프면 이전 타석의 베이스 정보 사용
                        if 'base_runners' in prev_record:
                            prev_base = {
                                '1루': prev_record['base_runners'].get('1루'),
                                '2루': prev_record['base_runners'].get('2루'), 
                                '3루': prev_record['base_runners'].get('3루')
                            }
                        else:
                            prev_base = {'1루': None, '2루': None, '3루': None}
                else:
                    # 첫 번째 타석의 경우 빈 베이스
                    prev_base = {'1루': None, '2루': None, '3루': None}
            
            # 결과를 한국어에서 영어로 변환
            result_mapping = {
                '1루타': 'single',
                '2루타': 'double', 
                '3루타': 'triple',
                '홈런': 'home_run',
                '4구': 'walk',
                '사구': 'hit_by_pitch',
                '삼진': 'strikeout',
                '플라이 아웃': 'fly_out',
                '땅볼 아웃': 'ground_out',
                '직선타 아웃': 'line_out',
                '병살타 아웃': 'double_play',
                '희생플라이 아웃': 'sacrifice_fly',
                '내야 안타': 'infield_single',
                '실책 출루': 'error',
                '선행주자아웃 출루': 'fielders_choice'
            }
            
            result = result_mapping.get(record['result'], record['result'])
            
            # 베이스 상황을 p_no로 변환
            base_with_pno = prev_base
            
            # 이벤트 생성
            event = {
                'prev': {
                    'score': prev_score,
                    'base': base_with_pno,
                    'outs': prev_outs
                },
                'pitcher': record['pitcher']['p_no'],
                'batter': record['batter']['p_no'],
                'result': result
            }
            
            # 이닝의 해당 half에 추가
            if half == '초':
                innings_data[inning]['top']['events'].append(event)
            else:
                innings_data[inning]['bottom']['events'].append(event)
        
        # 최종 구조 생성
        structured_data = {
            'innings': []
        }
        
        # 이닝 순서대로 정렬해서 추가
        for inning_num in sorted(innings_data.keys()):
            inning_data = innings_data[inning_num]
            structured_data['innings'].append({
                'inning': inning_num,
                'top': inning_data['top'],
                'bottom': inning_data['bottom']
            })
        
        return structured_data

def main():
    """메인 실행 함수 - 완전한 야구게임 시뮬레이션"""
    print("야구게임 시뮬레이션 시스템 v3.0")
    print("=" * 60)
    print("AI 모델 기반 실제 야구 경기 시뮬레이션")
    print("   2025년 실제 선수 데이터 + trained_model.pkl 활용")
    print("=" * 60)
    
    try:
        # 시뮬레이터 초기화
        print("시스템 초기화 중...")
        simulator = BaseballGameSimulator()
        
        print("\n게임 설정을 시작합니다!")
        print("   (Ctrl+C를 눌러 언제든 종료할 수 있습니다)")
        
        # 게임 설정 (홈팀/어웨이팀 라인업 구성)
        home_lineup, away_lineup = simulator.setup_game()
        
        if home_lineup and away_lineup:
            print("\n라인업 구성 완료!")
            print("\n경기 시작!")
            
            # 게임 상태 초기화
            game_state = GameState()
            
            # 완전한 9이닝 시뮬레이션 실행
            play_by_play = game_state.simulate_full_game(simulator, home_lineup, away_lineup)
            
            print(f"\n시뮬레이션 완료!")
            print(f"   - 총 {len(play_by_play)}개 타석")
            print(f"   - 최종 스코어: 어웨이 {game_state.away_score} - {game_state.home_score} 홈")
        else:
            print("\n게임 설정이 취소되었습니다.")
        
    except KeyboardInterrupt:
        print("\n\n사용자가 게임을 종료했습니다.")
    except Exception as e:
        print(f"\n오류 발생: {e}")
        import traceback
        traceback.print_exc()
        
        print("\n문제 해결 방법:")
        print("1. 필요한 파일들이 모두 있는지 확인 (trained_model.pkl, model_info.pkl)")
        print("2. 데이터 파일들이 올바른지 확인 (batters_2025.tsv, pitchers_2025.tsv)")
        print("3. Python 패키지가 설치되어 있는지 확인 (pandas, numpy, catboost, scikit-learn)")

def test_prediction():
    """예측 시스템 단독 테스트 (몬테카를로 시뮬레이션)"""
    print("몬테카를로 시뮬레이션 예측 테스트")
    print("=" * 50)
    
    try:
        simulator = BaseballGameSimulator()
        
        # 테스트용 더미 데이터
        dummy_batter = {'p_no': 11334}  # KIA 타이거즈 선수
        dummy_pitcher = {'p_no': 14109}  # 키움 히어로즈 투수
        
        print(f"타자: {simulator.get_player_name(11334)}")
        print(f"투수: {simulator.get_player_name(14109)}")
        print(f"구장: 광주 (KIA 타이거즈 홈)")
        
        print("\n몬테카를로 시뮬레이션 5회 실행:")
        print("-" * 50)
        
        # 5번 실행해서 다양한 결과 확인
        results_count = {}
        for i in range(5):
            result, probs = simulator.predict_at_bat_result(
                1, 0, [], dummy_batter, dummy_pitcher, 'p_home', 'KIA 타이거즈'
            )
            
            results_count[result] = results_count.get(result, 0) + 1
            
            print(f"시행 {i+1}: {result}")
        
        print(f"\nAI 예측 확률:")
        sorted_probs = sorted(probs.items(), key=lambda x: x[1], reverse=True)
        for i, (res, prob) in enumerate(sorted_probs[:5], 1):
            actual_count = results_count.get(res, 0)
            print(f"   {i}. {res}: {prob:.1%} (실제 {actual_count}/5회)")
        
        print(f"\n몬테카를로 시뮬레이션 결과:")
        print(f"   - 가장 높은 확률: {sorted_probs[0][0]} ({sorted_probs[0][1]:.1%})")
        print(f"   - 실제 선택된 결과들: {list(results_count.keys())}")
        print(f"   - 확률대로 다양한 결과가 나타남을 확인!")
            
    except Exception as e:
        print(f"테스트 실패: {e}")

if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1 and sys.argv[1] == "test":
        test_prediction()
    else:
        main()
