package com.ict.springboot.model;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.expiration}") // 24시간 (밀리초)
	private long expirationTime;

	private SecretKey getSecretKey() {
        byte[] secret = Base64.getEncoder().encodeToString(secretKey.getBytes()).getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(secret);
	}

	/**
	 * JWT토큰을 생성해서 반환하는 메소드
	 * @param username 사용자 아이디
	 * @param payloads 추가로 사용자 정보를 저장하기 위한 Claims
	 * @param expirationTime 토큰 만료 시간(15분에서 몇 시간이 적당).단위는 천분의 1초
	 * @return
	 */
	public String createToken(String username,Map<String,Object> payloads,long expirationTime) {
		//JWT 토큰의 만료 시간 설정
		long currentTimeMillis = System.currentTimeMillis();//토큰의 생성시간
		expirationTime = currentTimeMillis+expirationTime;//토큰의 만료시간

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		System.out.println("JWTokens:만료시간"+dateFormat.format(new Date(expirationTime)));
		//Header 부분 설정
		Map<String,Object> headers= new HashMap<>();
		headers.put("typ", "JWT");
		headers.put("alg", "HS256");


		//Claims객체 생성후 JwtBuilder의 claims(claims)로 해도 된다
		/*Claims claims = (Claims) Jwts.claims().setSubject(username);
		claims.putAll(payloads);
		claims.put("role", "ADMIN");*/

		JwtBuilder builder = Jwts.builder()
				.header().add(headers)// Headers 설정
				.and()
				.claims(payloads)// Claims 설정(기타 페이로드)
				.subject(username)//"sub"키로 사용자 ID 설정
				.issuedAt(new Date())//"iat"키로 생성 시간을 설정
				.expiration(new Date(expirationTime))//만료 시간 설정(필수로 설정하자.왜냐하면 토큰(문자열이라)은 세션처럼 제어가 안된다)
				.signWith(getSecretKey(), Jwts.SIG.HS256);//비밀 키로 JWT를 서명

		//JWT 생성
		String jwt = builder.compact();
		return jwt;
	}///////

    /**
	 * 발급한 토큰의 payloads부분을 반환하는 메소드
	 * @param token  발급토큰
	 * @return 토큰의 payloads부분 반환
	 */
	public Claims getTokenPayloads(String token){
		Claims claims = Jwts.claims().build();

		try {
			//아래 코드 실행시 유효하지 않은 토큰이면 예외 발생한다
			claims= Jwts.parser()
					 .verifyWith(getSecretKey()).build()//서명한 비밀키로 검증
					 .parseSignedClaims(token)//parseClaimsJws메소드는 만기일자 체크

					 .getPayload();

		}
		catch(Exception e) {
			//가져올때는 claims.get("invalid")
			claims.put("invalid", "유효하지 않는 토큰");//변조됬거나 만료된 토큰
		}
		return claims;
	}/////////////////////////
	/**
	 * 유효한 토큰인지 검증하는 메소드
	 * @param token  발급토큰
	 * @return 유효한 토큰이면 true,만료가 됬거나 변조된 토큰인 경우 false반환
	 */
	public boolean verifyToken(String token) {

		//블랙리스트 검사 로직 추가
		if (isTokenInvalidated(token)) {
	        System.out.println("이미 무효화된 토큰입니다 (로그아웃됨)");
	        return false;
	    }

		try {
			//JWT토큰 파싱 및 검증
			Jws<Claims> claims= Jwts.parser().verifyWith(getSecretKey()).build().parseSignedClaims(token);

			System.out.println("만기일자:"+claims.getPayload().getExpiration());
		}
		catch(JwtException | IllegalArgumentException e) {
			System.out.println("유효하지 않은 토큰입니다:"+e.getMessage());
			return false;
		}
		return true;

	}////////////////////////////////


	/**
	 * 요청헤더에 포함된 쿠키의 토큰값 반환하는 메소드
	 * 쿠키명:JWT-TOKEN
	 * @param request HttpServletRequest객체
	 * @param cookieName web.xml에 등록한 컨텍스트 초기화 파라미터 값(파라미터명는 "TOKEN-COOKIE-NAME")
	 * @return 발급받은 토큰 값
	 */

	public String getTokenInCookie(HttpServletRequest request,String cookieName) {
		Cookie[] cookies = request.getCookies();
		String token = "";
		if(cookies !=null){
			for(Cookie cookie:cookies){
				if(cookie.getName().equals(cookieName)){
					token = cookie.getValue();
				}
			}
		}
		return token;
	}///
	/*
	 * <<<로그아웃 처리용>>>
	 * JWT는 서버 측에서 직접 무효화가 불가능한 구조이기 때문에
	 * 로그아웃 시에는 토큰을 블랙리스트에 등록하여 처리한다
	 * 일반적으로 파일, 메모리, 또는 DB를 활용한 저장소에 토큰과 만료시간을 저장한다
	 *
	 * JWT는 서버가 상태를 가지지 않는(stateless) 인증 방식이기 때문에
	 * 클라이언트(브라우저)가 가지고 있는 토큰(JWT)이 유효하기만 하면 서버는
	 * 인증된 사용자로 간주한다
	 * 예:
	 * 사용자가 로그아웃.jsp에서 쿠키를 삭제 → 브라우저에서 토큰이 삭제됨 → 그 브라우저에서는
	 * 재접속 시 인증되지 않음 → OK
	 * 그러나 누군가 해당 토큰을 복사해 두었다면 그 사람은 여전히 JWT를 이용해 서버에
	 * 접근 가능하다(보안 문제)
	 * ※그래서 블랙리스트도 사용해야 한다
	 *
	 */
	/**
	 * 토큰의 만료 시간 (밀리초) 반환
	 * @param token 만료시간을 확인하기 위한 토큰
	 * @return 만료시간(밀리초)
	 */
	public long getExpiration(String token) {
	    try {
	        return Jwts.parser()
	                   .verifyWith(getSecretKey())
	                   .build()
	                   .parseSignedClaims(token)
	                   .getPayload()
	                   .getExpiration()
	                   .getTime();
	    } catch (Exception e) {
	        return 0;
	    }
	}
	// 로그아웃된 토큰(블랙리스트) 저장용 - 메모리 기반
	private static final Set<String> tokenBlacklist = new HashSet<>();

	/**
	 * 토큰을 블랙리스트에 등록 (로그아웃 처리)
	 * @param token 블랙리스트 처리할 토큰
	 *
	 */
	public void invalidateToken(String token) {
	    tokenBlacklist.add(token);
	}

	/**
	 * 해당 토큰이 블랙리스트에 등록되어 있는지 확인
	 * @param token 블랙리스트에 등록되었는지 확인할 토큰
	 * @return 블랙리스트에 등록된 경우 true반환,아니면 false반환
	 */
	public boolean isTokenInvalidated(String token) {
	    return tokenBlacklist.contains(token);
	}

}
