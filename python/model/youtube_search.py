# -*- coding: utf-8 -*-
"""
YouTube 검색 모듈
"""
import os
import requests
from typing import Optional, List, Dict


class YouTubeSearcher:
    def __init__(self, api_key: str):
        """YouTube Data API v3 초기화"""
        self.api_key = api_key
        self.base_url = "https://www.googleapis.com/youtube/v3"

    def search_music(
        self,
        query: str,
        max_results: int = 5
    ) -> List[Dict]:
        """
        YouTube에서 음악 검색 (자동으로 'ai' 추가 및 필터링)

        Args:
            query: 검색어 (예: "하입보이" → "하입보이 ai"로 자동 변환)
            max_results: 최대 검색 결과 수 (1-50)

        Returns:
            검색 결과 리스트 (ver, 리믹스, remix, cover, ai, 버전 중 하나 포함된 것만)
        """
        # 자동으로 'ai' 추가
        search_query = f"{query} ai"

        # 필터 키워드 (대소문자 구분 없이)
        filter_keywords = ["ver", "리믹스", "remix", "cover", "ai", "버전", "커버"]

        params = {
            "part": "snippet",
            "q": search_query,
            "type": "video",
            "videoCategoryId": "10",  # 음악 카테고리
            "maxResults": max_results * 3,  # 필터링 후 충분한 결과를 위해 3배 요청
            "key": self.api_key,
            "order": "relevance",  # 관련성 순
        }

        try:
            response = requests.get(
                f"{self.base_url}/search",
                params=params,
                timeout=10
            )
            response.raise_for_status()
            data = response.json()

            results = []
            for item in data.get("items", []):
                video_id = item["id"]["videoId"]
                snippet = item["snippet"]
                title = snippet["title"]

                # 제목에 필터 키워드가 하나라도 포함되어 있는지 확인 (대소문자 무시)
                title_lower = title.lower()
                if any(keyword.lower() in title_lower for keyword in filter_keywords):
                    results.append({
                        "video_id": video_id,
                        "title": title,
                        "channel": snippet["channelTitle"],
                        "description": snippet["description"],
                        "thumbnail": snippet["thumbnails"]["default"]["url"],
                        "watch_url": f"https://www.youtube.com/watch?v={video_id}"
                    })

                    # 원하는 개수만큼 찾으면 중단
                    if len(results) >= max_results:
                        break

            return results

        except requests.exceptions.RequestException as e:
            print(f"❌ YouTube 검색 오류: {e}")
            raise Exception(f"YouTube 검색 실패: {str(e)}")

    def search_first_music(
        self,
        query: str
    ) -> Optional[Dict]:
        """
        첫 번째 검색 결과 반환 (가장 관련성 높은 음악)

        Args:
            query: 검색어

        Returns:
            첫 번째 검색 결과 또는 None
        """
        results = self.search_music(query, max_results=1)
        return results[0] if results else None

    def get_video_info(self, video_id: str) -> Dict:
        """
        특정 영상의 상세 정보 조회

        Args:
            video_id: YouTube 영상 ID

        Returns:
            영상 정보
        """
        params = {
            "part": "snippet,contentDetails,statistics",
            "id": video_id,
            "key": self.api_key
        }

        try:
            response = requests.get(
                f"{self.base_url}/videos",
                params=params,
                timeout=10
            )
            response.raise_for_status()
            data = response.json()

            if not data.get("items"):
                raise Exception(f"영상을 찾을 수 없습니다: {video_id}")

            item = data["items"][0]
            snippet = item["snippet"]
            statistics = item.get("statistics", {})

            return {
                "video_id": video_id,
                "title": snippet["title"],
                "channel": snippet["channelTitle"],
                "description": snippet["description"],
                "view_count": int(statistics.get("viewCount", 0)),
                "like_count": int(statistics.get("likeCount", 0)),
                "watch_url": f"https://www.youtube.com/watch?v={video_id}"
            }

        except requests.exceptions.RequestException as e:
            print(f"❌ 영상 정보 조회 오류: {e}")
            raise Exception(f"영상 정보 조회 실패: {str(e)}")


# 전역 인스턴스 (FastAPI에서 사용)
def get_youtube_searcher() -> YouTubeSearcher:
    """YouTube 검색 인스턴스 반환"""
    api_key = os.getenv("YOUTUBE_API_KEY")
    if not api_key:
        raise Exception("YOUTUBE_API_KEY 환경변수가 설정되지 않았습니다")
    return YouTubeSearcher(api_key)


if __name__ == "__main__":
    # 테스트
    from dotenv import load_dotenv
    load_dotenv()

    searcher = get_youtube_searcher()

    # 검색 테스트
    print("🔍 음악 검색:")
    results = searcher.search_music("하입보이", max_results=5)
    for i, r in enumerate(results, 1):
        print(f"{i}. {r['title']} - {r['channel']}")
        print(f"   URL: {r['watch_url']}")
