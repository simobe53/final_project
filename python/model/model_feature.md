<<피쳐엔지니어링 필요>>

# 타자 지표 : 접두어 b_  ->  batters 파일
df['b_double_rate'] = df['b_2B'] / df['b_ePA'].replace(0, np.nan)      # 3루타 확률
df['b_triple_rate'] = df['b_3B'] / df['b_ePA'].replace(0, np.nan)      # 3루타 확률
df['b_hr_rate'] = df['b_HR'] / df['b_ePA'].replace(0, np.nan)          # 홈런 확률
df['b_hp_rate'] = df['b_HP'] / df['b_ePA'].replace(0, np.nan)          # 몸에맞는볼 확률
df['b_gdp_rate'] = df['b_GDP'] / df['b_ePA'].replace(0, np.nan)        # 병살 확률
df['b_sf_rate'] = df['b_SF'] / df['b_ePA'].replace(0, np.nan)          # 희생플라이 확률
df['b_rbi_rate'] = df['b_RBI'] / df['b_ePA'].replace(0, np.nan)        # 타점 확률
df['b_run_rate'] = df['b_SB'] / df['b_ePA'].replace(0, np.nan)          # 도루 확률
df['b_so_rate'] = df['b_SO'] / df['b_ePA'].replace(0, np.nan)          # 삼진 확률

# 투수 지표 : 접두어 p_  ->  pitchers 파일
df['p_2b_per_inning'] = df['p_2B'] / df['p_IP'].replace(0, np.nan)     # 이닝당 피2루타
df['p_3b_per_inning'] = df['p_3B'] / df['p_IP'].replace(0, np.nan)     # 이닝당 피3루타
df['p_hr_per_inning'] = df['p_HR'] / df['p_IP'].replace(0, np.nan)     # 이닝당 피홈런
df['p_hp_per_inning'] = df['p_HP'] / df['p_IP'].replace(0, np.nan)     # 이닝당 몸에맞는볼
df['p_so_per_inning'] = df['p_SO'] / df['p_IP'].replace(0, np.nan)     # 이닝당 삼진
df['p_roe_per_inning'] = df['p_ROE'] / df['p_IP'].replace(0, np.nan)   # 이닝당 실책
    

<<<모델이 필요로 하는 피쳐>>>
#범주형 피쳐 (5개):
inning - 이닝 - # all_inning = ['1회','2회','3회','4회','5회','6회','7회','8회','9회']
Prev_Outs - # all_outs = ['무사', '1사', '2사']
Prev_Runners - # all_runners = ['주자 없음', '1루', '2루', '3루', '1,2루', '1,3루', '2,3루', '만루']

location - home/away # pitchers 파일에서 home이면 p_home away면 p_away   

hand - 투타 상성  #batters 파일, pitchers 파일 에서 조합 
(ex -  batters 파일에서 right pitchers 파일에서 left이면 br_pl)  


#수치형 피처 (22개):
기본 통계:
b_AVG - 타자 타율
b_OBP - 타자 출루율
b_SLG - 타자 장타율
b_OPS - 타자 OPS
p_ERA - 투수 ERA
p_FIP - 투수 FIP
p_WHIP - 투수 WHIP


PF - 구장 팩터 # 97~ 104의 값

피처 엔지니어링으로 생성된 비율들:
b_triple_rate - 타자 3루타 확률
b_hr_rate - 타자 홈런 확률
b_hp_rate - 타자 몸에맞는볼 확률
b_gdp_rate - 타자 병살 확률
b_sf_rate - 타자 희생플라이 확률
b_rbi_rate - 타자 타점 확률
b_run_rate - 타자 도루 확률
b_so_rate - 타자 삼진 확률
p_2b_per_inning - 투수 이닝당 피2루타
p_3b_per_inning - 투수 이닝당 피3루타
p_hr_per_inning - 투수 이닝당 피홈런
p_hp_per_inning - 투수 이닝당 몸에맞는볼
p_so_per_inning - 투수 이닝당 삼진
p_roe_per_inning - 투수 이닝당 실책


# 헷갈리면 훈련에 쓰인데이터 참고.
data_train_verify.tsv

