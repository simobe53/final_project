# -*- coding: utf-8 -*-
"""
실시간 타석별 시뮬레이션 로직
FastAPI 서버에서 분리된 비즈니스 로직
"""

import numpy as np

class AtBatSimulator:
    """타석 시뮬레이션 핵심 로직"""

    @staticmethod
    def perform_complete_simulation(request, ai_result, batter_info, pitcher_info):
        """완전한 타석 시뮬레이션 수행"""
        try:
            # 게임 상황 추출
            inning = request.get("inning", 1)
            half = request.get("half", "초")
            outs = request.get("outs", 0)
            current_home_score = request.get("homeScore", 0)
            current_away_score = request.get("awayScore", 0)

            # 주자 상황 추출
            current_runners = []
            base_runners = {}
            if request.get("base1"):
                current_runners.append("1루")
                base_runners["1루"] = request.get("base1")
            if request.get("base2"):
                current_runners.append("2루")
                base_runners["2루"] = request.get("base2")
            if request.get("base3"):
                current_runners.append("3루")
                base_runners["3루"] = request.get("base3")

            # 결과 변환 (한국어 → 영어)
            english_result = AtBatSimulator.convert_result_to_english(ai_result)

            # 베이스 러닝 시뮬레이션
            new_base_runners, runs_scored = AtBatSimulator.simulate_base_running(
                current_runners, base_runners, ai_result,
                batter_info.get('p_no') if batter_info else None
            )

            # 베이스 러닝 로그
            if runs_scored > 0:
                print(f"   득점: {runs_scored}점")

            # 점수 업데이트
            new_home_score = current_home_score + runs_scored if half == "말" else current_home_score
            new_away_score = current_away_score + runs_scored if half == "초" else current_away_score

            # 9회 이후 말 공격 워크오프 승 체크 (득점 직후 홈팀이 앞서면 즉시 종료)
            if inning >= 9 and half == "말" and runs_scored > 0 and new_home_score > new_away_score:
                print(f"   🎉 워크오프 승리! {inning}회말 {new_home_score}-{new_away_score}로 홈팀 승리!")
                return AtBatSimulator.build_simulation_result(
                    english_result, ai_result, runs_scored,
                    inning, half, outs, new_home_score, new_away_score,
                    new_base_runners, False, True, "HOME",
                    pitcher_info, batter_info
                )

            # 아웃 카운트 및 이닝 진행
            # 병살타는 1루 주자가 있을 때만 2아웃, 없으면 1아웃
            if ai_result == "병살타 아웃":
                new_outs = outs + (2 if "1루" in current_runners else 1)
            elif ai_result == "선행주자아웃 출루":
                new_outs = outs + 1  # 선행주자 1명 아웃
            else:
                new_outs = outs + AtBatSimulator.get_out_count(ai_result)

            new_inning = inning
            new_half = half
            inning_changed = False

            if new_outs >= 3:
                new_outs = 0
                new_base_runners = {"base1": None, "base2": None, "base3": None}
                inning_changed = True
                print(f"   이닝 교체: {inning}회{half} 종료")

                if half == "초":
                    new_half = "말"
                else:
                    new_half = "초"
                    new_inning = inning + 1

            # 게임 종료 조건 체크
            game_ended, winner = AtBatSimulator.check_game_end_conditions(
                new_inning, new_half, new_home_score, new_away_score
            )

            # 게임 종료 시 표시용 이닝 조정
            # 말 이닝에서 3아웃으로 게임이 종료되는 경우 다음 이닝 초가 아닌 현재 이닝 말로 표시
            # 예: 9회말 3아웃 → 내부적으로 10회초로 전환 → 게임종료 → UI에는 9회말로 표시
            # 이렇게 하면 타석은 정상 생성되면서도 UI에 다음 이닝이 표시되지 않음
            display_inning = new_inning
            display_half = new_half

            if game_ended and inning_changed and new_half == "초":
                # 말 이닝에서 3아웃으로 게임 종료된 경우
                # UI 표시용으로 이전 이닝 말로 유지
                # 9회말 → 10회초, 10회말 → 11회초, 11회말 → 12회초, 12회말 → 13회초 등 모든 경우 처리
                display_inning = new_inning - 1
                display_half = "말"

            return AtBatSimulator.build_simulation_result(
                english_result, ai_result, runs_scored,
                display_inning, display_half, new_outs, new_home_score, new_away_score,
                new_base_runners, inning_changed, game_ended, winner,
                pitcher_info, batter_info
            )

        except Exception as e:
            return {"error": f"시뮬레이션 처리 오류: {str(e)}"}

    @staticmethod
    def convert_result_to_english(korean_result):
        """결과 변환 (한국어 → 영어)"""
        result_mapping = {
            '1루타': 'single', '2루타': 'double', '3루타': 'triple', '홈런': 'home_run',
            '4구': 'walk', '사구': 'hit_by_pitch', '삼진': 'strikeout',
            '플라이 아웃': 'fly_out', '땅볼 아웃': 'ground_out', '직선타 아웃': 'line_out',
            '병살타 아웃': 'double_play', '희생플라이 아웃': 'sacrifice_fly',
            '내야 안타': 'infield_single', '실책 출루': 'error', '선행주자아웃 출루': 'fielders_choice'
        }
        return result_mapping.get(korean_result, korean_result)

    @staticmethod
    def build_simulation_result(english_result, korean_result, runs_scored,
                                inning, half, outs, home_score, away_score,
                                base_runners, inning_changed, game_ended, winner,
                                pitcher_info, batter_info):
        """시뮬레이션 결과 구성"""
        return {
            "result": english_result,
            "result_korean": korean_result,
            "rbi": runs_scored,
            "new_game_state": {
                "inning": inning,
                "half": half,
                "outs": outs,
                "homeScore": home_score,
                "awayScore": away_score,
                "base1": base_runners.get("base1"),
                "base2": base_runners.get("base2"),
                "base3": base_runners.get("base3"),
                "inning_changed": inning_changed
            },
            "game_ended": game_ended,
            "winner": winner,
            "pitcher_p_no": pitcher_info.get('p_no') if pitcher_info else None,
            "batter_p_no": batter_info.get('p_no') if batter_info else None
        }

    @staticmethod
    def simulate_base_running(current_runners, base_runners, result, batter_p_no):
        """베이스 러닝 시뮬레이션 (야구 규칙 준수)"""
        new_base_runners = {"base1": None, "base2": None, "base3": None}
        runs_scored = 0

        # ========== 안타/장타 ==========
        if result == "홈런":
            # 모든 주자 + 타자 득점
            runs_scored = len(current_runners) + 1
            return new_base_runners, runs_scored

        elif result == "3루타":
            # 모든 주자 득점, 타자는 3루
            runs_scored = len(current_runners)
            new_base_runners["base3"] = batter_p_no

        elif result == "2루타":
            # 3루 주자: 득점
            # 2루 주자: 득점
            # 1루 주자: 3루 진루 (보수적: 홈까지 가지 않음)
            # 타자: 2루
            if "3루" in current_runners:
                runs_scored += 1
            if "2루" in current_runners:
                runs_scored += 1
            if "1루" in current_runners:
                new_base_runners["base3"] = base_runners.get("1루")
            new_base_runners["base2"] = batter_p_no

        elif result in ["1루타", "내야 안타"]:
            # 3루 주자: 득점
            # 2루 주자: 3루 진루 (보수적: 홈까지 가지 않음)
            # 1루 주자: 2루 진루
            # 타자: 1루
            if "3루" in current_runners:
                runs_scored += 1
            if "2루" in current_runners:
                new_base_runners["base3"] = base_runners.get("2루")
            if "1루" in current_runners:
                new_base_runners["base2"] = base_runners.get("1루")
            new_base_runners["base1"] = batter_p_no

        # ========== 볼넷/사구 (밀려나는 주자만 진루) ==========
        elif result in ["4구", "사구"]:
            # 1루가 비어있으면 타자만 1루 출루
            if "1루" not in current_runners:
                # 2루, 3루 주자는 그대로
                if "3루" in current_runners:
                    new_base_runners["base3"] = base_runners.get("3루")
                if "2루" in current_runners:
                    new_base_runners["base2"] = base_runners.get("2루")
                new_base_runners["base1"] = batter_p_no
            else:
                # 1루에 주자가 있으면 밀려남
                if "2루" not in current_runners:
                    # 1루만: 1루→2루, 타자→1루
                    new_base_runners["base2"] = base_runners.get("1루")
                    if "3루" in current_runners:
                        new_base_runners["base3"] = base_runners.get("3루")
                else:
                    # 1,2루 모두 차있음
                    if "3루" not in current_runners:
                        # 1,2루: 2루→3루, 1루→2루, 타자→1루
                        new_base_runners["base3"] = base_runners.get("2루")
                        new_base_runners["base2"] = base_runners.get("1루")
                    else:
                        # 만루: 3루→홈(득점), 2루→3루, 1루→2루, 타자→1루
                        runs_scored = 1
                        new_base_runners["base3"] = base_runners.get("2루")
                        new_base_runners["base2"] = base_runners.get("1루")
                new_base_runners["base1"] = batter_p_no

        # ========== 실책 출루 (타자 출루, 주자 상황에 따라 진루) ==========
        elif result == "실책 출루":
            # 타자는 1루 출루
            new_base_runners["base1"] = batter_p_no

            # 1루 주자가 있었으면 밀려서 진루
            if "1루" in current_runners:
                # 2루도 차있었으면 2루 주자도 밀림
                if "2루" in current_runners:
                    # 3루도 차있었으면 만루 → 실책이므로 3루 주자는 홈 가지 않고 그대로
                    if "3루" in current_runners:
                        # 만루 상황: 실책으로 베이스가 꽉 차므로 3루는 그대로, 2루는 3루 못감
                        # 보수적으로: 3루 유지, 2루→3루 불가, 1루→2루
                        new_base_runners["base3"] = base_runners.get("3루")
                        new_base_runners["base2"] = base_runners.get("2루")
                    else:
                        # 1,2루: 2루→3루, 1루→2루
                        new_base_runners["base3"] = base_runners.get("2루")
                        new_base_runners["base2"] = base_runners.get("1루")
                else:
                    # 1루만: 1루→2루
                    new_base_runners["base2"] = base_runners.get("1루")
                    if "3루" in current_runners:
                        new_base_runners["base3"] = base_runners.get("3루")
            else:
                # 1루가 비어있었으면 나머지 주자 그대로
                if "3루" in current_runners:
                    new_base_runners["base3"] = base_runners.get("3루")
                if "2루" in current_runners:
                    new_base_runners["base2"] = base_runners.get("2루")

        # ========== 선행주자아웃 출루 (야수선택 - 선행주자 아웃, 타자 출루) ==========
        elif result == "선행주자아웃 출루":
            # 타자는 1루 출루, 홈에서 가장 먼 주자가 아웃됨
            new_base_runners["base1"] = batter_p_no

            # 우선순위: 홈에서 가장 먼 주자가 아웃 (3루 > 2루 > 1루 순)
            if "3루" in current_runners:
                # 3루 주자 아웃, 1루/2루는 그대로
                if "2루" in current_runners:
                    new_base_runners["base2"] = base_runners.get("2루")
                # 1루 주자는 타자가 들어오므로 이미 없음
            elif "2루" in current_runners:
                # 2루 주자 아웃, 1루는 타자가 들어감
                pass
            elif "1루" in current_runners:
                # 1루 주자 아웃 (타자가 1루로 들어감)
                pass

        # ========== 희생플라이 (외야 플라이로 3루 주자 태그업 득점) ==========
        elif result == "희생플라이 아웃":
            # 3루 주자: 태그업 득점
            # 2루 주자: 그대로 (태그업으로 3루 못감)
            # 1루 주자: 그대로
            if "3루" in current_runners:
                runs_scored = 1
            if "2루" in current_runners:
                new_base_runners["base2"] = base_runners.get("2루")
            if "1루" in current_runners:
                new_base_runners["base1"] = base_runners.get("1루")

        # ========== 땅볼 아웃 (진루 상황 고려) ==========
        elif result == "땅볼 아웃":
            # 주자 상황별 진루 처리
            if not current_runners:
                # 빈 베이스: 타자만 아웃
                pass

            elif current_runners == ["1루"]:
                # 1루만: 1루 → 2루 (100%), 타자 아웃
                new_base_runners["base2"] = base_runners.get("1루")

            elif current_runners == ["2루"]:
                # 2루만: 잔루 30% / 2루→3루 70%, 타자 아웃
                if np.random.random() < 0.70:
                    new_base_runners["base3"] = base_runners.get("2루")
                else:
                    new_base_runners["base2"] = base_runners.get("2루")

            elif current_runners == ["3루"]:
                # 3루만: 잔루 100%, 타자 아웃
                new_base_runners["base3"] = base_runners.get("3루")

            elif set(current_runners) == {"1루", "2루"}:
                # 1,2루: 1루→2루, 2루→3루 (결과: 2,3루), 타자 아웃
                new_base_runners["base2"] = base_runners.get("1루")
                new_base_runners["base3"] = base_runners.get("2루")

            elif set(current_runners) == {"1루", "3루"}:
                # 1,3루: 1루→2루, 3루 그대로 (결과: 2,3루), 타자 아웃
                new_base_runners["base2"] = base_runners.get("1루")
                new_base_runners["base3"] = base_runners.get("3루")

            elif set(current_runners) == {"2루", "3루"}:
                # 2,3루: 모두 잔루, 타자 아웃
                new_base_runners["base2"] = base_runners.get("2루")
                new_base_runners["base3"] = base_runners.get("3루")

            elif set(current_runners) == {"1루", "2루", "3루"}:
                # 만루: 모두 잔루, 타자 아웃
                new_base_runners["base1"] = base_runners.get("1루")
                new_base_runners["base2"] = base_runners.get("2루")
                new_base_runners["base3"] = base_runners.get("3루")

        # ========== 일반 아웃 (주자 그대로) ==========
        elif result in ["삼진", "플라이 아웃", "직선타 아웃"]:
            # 주자들 그대로 유지
            if "3루" in current_runners:
                new_base_runners["base3"] = base_runners.get("3루")
            if "2루" in current_runners:
                new_base_runners["base2"] = base_runners.get("2루")
            if "1루" in current_runners:
                new_base_runners["base1"] = base_runners.get("1루")

        # ========== 병살타 (주자 + 타자 아웃 OR 타자만 아웃) ==========
        elif result == "병살타 아웃":
            # 주자가 있으면: 1루 주자 + 타자 아웃 (2아웃)
            # 주자가 없으면: 타자만 아웃 (1아웃) - 실제로는 병살타가 안 되지만 예외 처리
            if "1루" in current_runners:
                # 1루 주자 아웃, 타자도 아웃
                # 3루 주자: 플레이 중 득점 가능
                # 2루 주자: 3루 진루
                if "3루" in current_runners:
                    runs_scored = 1
                if "2루" in current_runners:
                    new_base_runners["base3"] = base_runners.get("2루")
            else:
                # 주자 없으면 타자만 아웃 (병살타 불가능 상황)
                # 2루, 3루 주자는 그대로
                if "3루" in current_runners:
                    new_base_runners["base3"] = base_runners.get("3루")
                if "2루" in current_runners:
                    new_base_runners["base2"] = base_runners.get("2루")

        return new_base_runners, runs_scored

    @staticmethod
    def get_out_count(result):
        """아웃 개수 반환"""
        if result == "병살타 아웃":
            return 2
        elif result in ["삼진", "플라이 아웃", "땅볼 아웃", "직선타 아웃", "희생플라이 아웃"]:
            return 1
        return 0

    @staticmethod
    def check_game_end_conditions(inning, half, home_score, away_score):
        """게임 종료 조건 체크 (12회까지 연장전, 12회 종료 시 동점이면 무승부)"""
        game_ended = False
        winner = None

        # 9회초가 끝나고 말로 넘어갈 때: 홈팀이 이기고 있으면 9회말 진행 없이 게임 종료
        if inning == 9 and half == "말" and home_score > away_score:
            game_ended = True
            winner = "HOME"
            print(f"게임 종료: 9회초 종료, 홈팀 승리 {home_score}-{away_score} (9회말 진행 없음)")
            return game_ended, winner

        # 9회말이 끝나고 10회로 넘어가려고 할 때 체크
        if inning == 10 and half == "초":
            # 동점이면 연장전 진행
            if home_score == away_score:
                print(f"동점 {home_score}-{away_score}, 10회 연장전 진행")
                return False, None
            # 승부가 났으면 게임 종료
            game_ended = True
            if home_score > away_score:
                winner = "HOME"
            else:
                winner = "AWAY"
            print(f"게임 종료: 9회 종료 {home_score}-{away_score} (승자: {winner})")
            return game_ended, winner

        # 10회말이 끝나고 11회로 넘어가려고 할 때 체크
        if inning == 11 and half == "초":
            # 동점이면 연장전 계속
            if home_score == away_score:
                print(f"동점 {home_score}-{away_score}, 11회 연장전 진행")
                return False, None
            # 승부가 났으면 게임 종료
            game_ended = True
            if home_score > away_score:
                winner = "HOME"
            else:
                winner = "AWAY"
            print(f"게임 종료: 10회 종료 {home_score}-{away_score} (승자: {winner})")
            return game_ended, winner

        # 11회말이 끝나고 12회로 넘어가려고 할 때 체크
        if inning == 12 and half == "초":
            # 동점이면 연장전 계속
            if home_score == away_score:
                print(f"동점 {home_score}-{away_score}, 12회 연장전 진행")
                return False, None
            # 승부가 났으면 게임 종료
            game_ended = True
            if home_score > away_score:
                winner = "HOME"
            else:
                winner = "AWAY"
            print(f"게임 종료: 11회 종료 {home_score}-{away_score} (승자: {winner})")
            return game_ended, winner

        # 12회말이 끝나서 13회가 되려고 하면 무조건 게임 종료
        if inning == 13 and half == "초":
            game_ended = True
            if home_score > away_score:
                winner = "HOME"
            elif away_score > home_score:
                winner = "AWAY"
            else:
                winner = "TIE"
            print(f"게임 종료: 12회 종료 {home_score}-{away_score} ({winner})")
            return game_ended, winner

        return game_ended, winner