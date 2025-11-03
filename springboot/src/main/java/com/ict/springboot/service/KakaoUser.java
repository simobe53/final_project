package com.ict.springboot.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*
 
 
 JSON의 {}는 클래스로
 
{
	{}
 	
} 안쪽 중괄호는 내부 정적 클래스로
JSON의 키는 클래스의 필드로
응답받은 JSON 예

{
	key1:1004,
	key2:"~",
	key3:{
		key3_1:"~"
	
	}
	key4_key5:{
		key45:1004
	}

}
를
자바 클래스로 정의
@Getter
@Setter
public class 임의의클래스명{
	private int key1;
	private String key2;
	private Key3 key3;
	private Key4Key5 key4_key5;//클래스명은 _가 빠진 파스칼 형식
	
	@Getter
	@Setter	
	public static class Key3{
		private String key3_1;
	}
	@Getter
	@Setter
	public static class Key4Key5{
		private int key45;
	}
}
그리고 외부클래스와 내부클래스에 게터와 세터를 만든다 
 
 
 
 카카오로 부터 받은 사용자 정보 JSON 응답 
  {
		"id": 358479854753,
		"connected_at": "2025-07-02T00:12:44Z",
		"properties":{
			"nickname": "김길동",
			"profile_image": "http://k.kakaocdn.net/dn/~",
			"thumbnail_image": "http://k.kakaocdn.net/dn/~"
		},
		"kakao_account":{
				"profile_nickname_needs_agreement": false,
				"profile_image_needs_agreement": false,
				"profile":{
					nickname": "김길동",
					"thumbnail_image_url": "~",
					"profile_image_url": "~",
					"is_default_image": false,
					"is_default_nickname": false
				}
		}
  
  
  }

 */
//https://www.jsonschema2pojo.org/
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KakaoUser {
    private Long id;//카카오의 고유 아이디
    private String connected_at;
    private Properties properties;
    private KakaoAccount kakao_account;

    @Getter @Setter
    public static class Properties {
        private String nickname;
        private String profile_image;
        private String thumbnail_image;
    }

    @Getter @Setter
    public static class KakaoAccount {
        private Boolean profile_nickname_needs_agreement;
        private Boolean profile_image_needs_agreement;
        private Profile profile;
    }

    @Getter @Setter
    public static class Profile {
        private String nickname;
        private String thumbnail_image_url;
        private String profile_image_url;
        private Boolean is_default_image;
        private Boolean is_default_nickname;
    }
}
