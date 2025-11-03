package com.ict.springboot.model;

import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.ict.springboot.service.KakaoUser;

import jakarta.servlet.http.HttpServletResponse;

public class KakaoLoginApi {
	
	//<<액세스 토큰 얻는 메소드>>	
	/**
	 * https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#request-token
	 * @param restTemplate HTTP 요청용 객체
	 * @param code 카카오에서 받은 인가 코드
	 * @param restApiKey REST API KEY
	 * @param redirectUri 카카오 개발자 센터의 어플리케이션에 등록한 액세스 토큰을 받을 URI
	 * @return 액세스 토큰 반환
	 
	 */
	public static String getAccessToken(RestTemplate restTemplate, String code,String restApiKey,String redirectUri) {
		//<<<Access Token 발급 요청>>>
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        /*
         * Map은 하나의 키에 하나의 값만 매핑 가능하다
         * MultiValueMap은 하나의 Key에 여러 개의 Value를 매핑가능하다
         * Map<K, List<V>> 와 동일한 구조다         
		   form-data 전송 시 동일 key에 여러 값 전달할 수 있다(key=val1&key=val2)
		   REST API 파라미터 구성 시 유용하다
		   LinkedMultiValueMap은 MultiValueMap의 구현체로 입력 순서를 보장한다
         */
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", restApiKey);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
       
        HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);

        @SuppressWarnings("rawtypes")
        ResponseEntity<Map> tokenResponse = restTemplate.exchange(
                "https://kauth.kakao.com/oauth/token",
                 HttpMethod.POST,
                 tokenRequest,
                 Map.class
        );

        String accessToken = (String) tokenResponse.getBody().get("access_token");
	    return accessToken;
	}
	//<<사용자 정보 반환하는 메소드>>
	/**
	 * https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#req-user-info
	 * @param restTemplate HTTP 요청용 객체
	 * @param accessToken 액세스 토큰
	 * @return 카카오에서 받은 회원정보를 저장한 KakaoUser객체
	 */
	public static KakaoUser getKakaoUser(RestTemplate restTemplate,String accessToken) {
		//<<< 사용자 정보 요청>>>
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);

        HttpEntity<Void> userInfoRequest = new HttpEntity<>(headers);

        ResponseEntity<KakaoUser> userResponse = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                userInfoRequest,               
                KakaoUser.class
        );
        
        return userResponse.getBody();
	}
	//<<서비스 로그아웃 메서드>>
	/**
	 * https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#logout
	 * @param restTemplate HTTP 요청용 객체
	 * @param accessToken 액세스 토큰
	   https://kapi.kakao.com/v1/user/logout 주소로 POST요청
	   카카오 내 토큰만 무효화하고 카카오 인증 서버의 사용자 세션은 유지
	   이후 로그인 시 계정 인증 없이 바로 Access Token 재발급 되어
	   즉시 로그인됨(카카오 정책상 정상)
	   RestTemplate으로 POST 가능
		 */
	
	public static void kakaoServiceLogout(String restApiKey,RestTemplate restTemplate,String accessToken) {
	    
	    try {	       
	    	
	        HttpHeaders headers = new HttpHeaders();
	        headers.set("Authorization", "Bearer " + accessToken);

	        HttpEntity<Void> logoutRequest = new HttpEntity<>(headers);

	        restTemplate.exchange(
	        		"https://kapi.kakao.com/v1/user/logout",
	                HttpMethod.POST,
	                logoutRequest,
	                String.class
	        );
	       
	    } 
	    catch (Exception e) {e.printStackTrace();}
	}
	//<2.카카오계정과 함께 로그아웃>
	/**
	 * @param response  라다이렉트룔 서블릿 API
	 * @param restApiKey REST API Key
	 * @param logout_redirect_url 계정 로그아웃후 리다이렉트로 이동할 앱의 URL 
	 
	   https://kauth.kakao.com/oauth/logout주소로 리다이렉트 로그아웃 요청 
	   토큰 만료 및 카카오 계정 자체의 로그인 세션 만료
	   logout_redirect_uri에 설정한 주소로 카카오에서 세션 로그아웃 후 리다이렉트 시킨다
	   클라이언트 브라우저로 리다이렉트만 가능하다
	   즉 RestTemplate으로 요청 불가능하다
	 
	 */
	public static void kakaoAccountLogout(HttpServletResponse response, String restApiKey,String logout_redirect_url) {
		String logoutUrl = "https://kauth.kakao.com/oauth/logout"
                + "?client_id=" + restApiKey
                + "&logout_redirect_uri="+logout_redirect_url;
		try {
			response.sendRedirect(logoutUrl);
		}
		catch (Exception e) {e.printStackTrace();}
		
	}
	/**
	 * 카카오 로그아웃 URL 생성
	 * @param restApiKey 카카오 REST API 키
	 * @param logoutRedirectUri 로그아웃 후 리다이렉트 URI
	 * @return 카카오 로그아웃 URL
	 */
	public static String getKakaoLogoutUrl(String restApiKey, String logoutRedirectUri) {
		return "https://kauth.kakao.com/oauth/logout?client_id=" + restApiKey + "&logout_redirect_uri=" + logoutRedirectUri;
	}
}
