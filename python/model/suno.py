import requests
import time
import dotenv
import os
import asyncio

from urllib3 import response

class SunoAPI:
    def __init__(self, api_key, pending_tasks=None, task_errors=None):
        self.api_key = api_key
        self.base_url = 'https://api.sunoapi.org/api/v1'
        self.headers = {
            'Authorization': f'Bearer {api_key}',
            'Content-Type': 'application/json'
        }
        self.pending_tasks = pending_tasks  # 외부에서 주입받는 이벤트 저장소
        self.task_errors = task_errors  # 외부에서 주입받는 에러 저장소
    
    def generate_music(self, **options):
        response = requests.post(f'{self.base_url}/generate',
                               headers=self.headers, json=options)
        result = response.json()

        if result['code'] != 200:
            raise Exception(f"Generation failed: {result['msg']}")

        return result['data']['taskId']

    def upload_and_cover(self, **options):
        """Upload and Cover Audio API 호출"""
        response = requests.post(f'{self.base_url}/generate/upload-cover',
                               headers=self.headers, json=options)
        result = response.json()

        if result['code'] != 200:
            raise Exception(f"Upload and cover failed: {result['msg']}")

        return result['data']['taskId']

    async def wait_for_completion(self, task_id_lyrics=None, task_id_music=None, task_id_cover=None, max_wait_time=600):
        """
        하이브리드 방식: 폴링 + 콜백 알림
        - 기본적으로 15초마다 폴링하여 안정성 보장
        - 콜백이 오면 즉시 확인하여 응답 속도 개선
        """
        start_time = time.time()
        task_id = task_id_lyrics or task_id_music or task_id_cover

        # 이벤트 등록 (콜백이 올 때 알림받기 위함)
        if self.pending_tasks is not None and task_id:
            event = asyncio.Event()
            self.pending_tasks[task_id] = event
            print(f"🔔 Task {task_id} 이벤트 등록 완료")

        try:
            while time.time() - start_time < max_wait_time:
                # 콜백 알림 대기 또는 15초 타임아웃
                if self.pending_tasks is not None and task_id and task_id in self.pending_tasks:
                    try:
                        await asyncio.wait_for(self.pending_tasks[task_id].wait(), timeout=15.0)
                        print(f"⚡ 콜백 알림 수신 - 즉시 확인")
                        # 이벤트 리셋 (다음 폴링을 위해)
                        self.pending_tasks[task_id].clear()
                    except asyncio.TimeoutError:
                        # 15초 경과 - 정상적인 폴링
                        print(f"⏰ 15초 경과 - 폴링 확인")
                else:
                    # 콜백 미사용 시 일반 sleep
                    await asyncio.sleep(15)

                # 작업 상태 확인
                resp = self.get_task_status(task_id_lyrics, task_id_music, task_id_cover)

                # 디버깅: 응답 구조 출력
                print(f"📊 Task {task_id} 상태 응답:")
                print(f"   응답 키: {list(resp.keys())}")

                # 상태 확인 (status 또는 callbackType)
                status = resp.get('status') or resp.get('callbackType')
                print(f"   상태: {status}")

                # 완료 조건: status='SUCCESS' 또는 callbackType='complete'
                if status in ['SUCCESS', 'complete']:
                    print(f"✅ Task {task_id} 완료!")
                    # response 또는 전체 resp 반환
                    return resp.get('response', resp)
                elif status in ['FAILED', 'error']:
                    error_msg = resp.get('errorMessage') or resp.get('msg', 'Unknown error')
                    raise Exception(f"Generation failed: {error_msg}")

            raise Exception('Generation timeout')

        finally:
            # 작업 완료 후 이벤트 정리
            if self.pending_tasks is not None and task_id and task_id in self.pending_tasks:
                del self.pending_tasks[task_id]
                print(f"🗑️ Task {task_id} 이벤트 정리 완료")
            # 에러 정보 정리
            if self.task_errors is not None and task_id and task_id in self.task_errors:
                del self.task_errors[task_id]
                print(f"🗑️ Task {task_id} 에러 정보 정리 완료")
    
    def get_task_status(self, task_id_lyrics=None, task_id_music=None, task_id_cover=None):
        task_id = task_id_lyrics or task_id_music or task_id_cover

        # 에러 저장소에서 먼저 확인
        if self.task_errors is not None and task_id in self.task_errors:
            error_info = self.task_errors[task_id]
            print(f"⚠️ Task {task_id} 에러 정보 발견: {error_info['msg']}")
            return {
                'status': 'error',
                'callbackType': 'error',
                'msg': error_info['msg'],
                'errorMessage': error_info['msg']
            }

        if task_id_lyrics:
            response = requests.get(f'{self.base_url}/lyrics/record-info?taskId={task_id_lyrics}',
                                headers={'Authorization': f'Bearer {self.api_key}'})

        elif task_id_music:
            response = requests.get(f'{self.base_url}/generate/record-info?taskId={task_id_music}',
                              headers={'Authorization': f'Bearer {self.api_key}'})

        elif task_id_cover:
            response = requests.get(f'{self.base_url}/generate/record-info?taskId={task_id_cover}',
                              headers={'Authorization': f'Bearer {self.api_key}'})

        return response.json()['data']