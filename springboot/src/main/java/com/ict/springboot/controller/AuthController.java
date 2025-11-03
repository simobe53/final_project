package com.ict.springboot.controller;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.model.KakaoLoginApi;
import com.ict.springboot.service.AuthService;
import com.ict.springboot.service.KakaoUser;
import com.ict.springboot.model.JwtUtil;
import com.ict.springboot.service.PasswordlessService;
import com.ict.springboot.service.UsersService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

// Controller랑 RestController의 차이점 : ResponseBody를 따로 적을 필요없이 무조건 데이터만 반환한다.
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final PasswordlessService passwordlessService;
    private final UsersService usersService;

	// <<Passwordless 설정값 주입>>
	@Value("${passwordless.recommend}")
	private String passwordlessRecommend;

	@Value("${google.client-secret}")
	private String googleClientSecret;

	// <<카카오 로그인 관련 키 주입받기>>
	@Value("${kakao.rest_api_key}")
	private String restApiKey;
	@Value("${kakao.redirect_uri}")
	private String redirectUri;
	@Value("${kakao.logout_redirect_uri}")
	private String logoutRedirectUri;
	@Value("${google.client-id}")
	private String googleClientId;

	// <<프론트엔드 URL 주입>>
	@Value("${front.url}")
	private String frontUrl;

	private final RestTemplate restTemplate;
	// private final NotificationsService notiService;
	
	@PostMapping("verify-password")
	public ResponseEntity<?> verifyPassword(@RequestBody UsersDto testDto, HttpServletRequest request) {
		// JWT 토큰에서 사용자 정보 추출
		String token = jwtUtil.getTokenInCookie(request, "JWT-TOKEN");
		if (token == null || !jwtUtil.verifyToken(token)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
		}
		
		var claims = jwtUtil.getTokenPayloads(token);
		String loginAccount = claims.getSubject();
		
		if (!testDto.getAccount().equals(loginAccount)) {
			// 비밀번호 검증 시도하는 아이디와 로그인 아이디 불일치시 무조건 에러 반환 : 보안
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "정상적인 접근이 아닙니다."));
        }
		
		UsersDto loginUser = UsersDto.builder()
				.account(loginAccount)
				.password(testDto.getPassword())
				.build();
			
		UsersDto loginDto = authService.isUser(loginUser);
		if (loginDto == null) {
            // 검증 실패시 401 Unauthorized 상태 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "비밀번호가 틀립니다!"));
        }
        return ResponseEntity.ok(loginDto);
	}
	
	@GetMapping("login")
	public ResponseEntity<?> loginCheck(HttpServletRequest request) {
		// JWT 토큰에서 사용자 정보 추출
		String token = jwtUtil.getTokenInCookie(request, "JWT-TOKEN");
		if (token == null || !jwtUtil.verifyToken(token)) {
			return ResponseEntity.status(401).build();
		}
		
		var claims = jwtUtil.getTokenPayloads(token);
		String account = claims.getSubject();
		
		// 사용자 정보 조회
		UsersDto loginDto = UsersDto.builder().account(account).build();
		UsersDto sessionDto = authService.findLoginUser(loginDto);
		
		//로그인 상태
		if (sessionDto == null) return ResponseEntity.status(401).build();

		//읽지 않은 알림수 저장
		// sessionDto.setNotiCount(notiService.countNewNotify(session));

		//로그인 상태
	    return ResponseEntity.ok(sessionDto);
	}
	
    @PostMapping("login")
    public ResponseEntity<?> loginProcess(@RequestBody UsersDto user, HttpServletResponse response) {
        log.info("로그인 시도 - 사용자: {}", user.getAccount());

        // 1. 먼저 Passwordless 등록 여부 확인 (Recommend 모드일 때만)
        if ("1".equals(passwordlessRecommend)) {
            boolean isPasswordlessRegistered = passwordlessService.isPasswordlessRegistered(user.getAccount());

            if (isPasswordlessRegistered) {
                log.warn("Passwordless 등록된 사용자 일반 로그인 시도 차단 - 사용자: {}", user.getAccount());
                // Passwordless가 등록된 사용자는 일반 로그인 차단
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "message", "Passwordless가 등록된 사용자는 일반 로그인을 사용할 수 없습니다.",
                        "passwordlessRegistered", true
                ));
            }
        }

        // 2. 비밀번호 검증
        UsersDto sessionDto = authService.isUser(user);

        if (sessionDto == null) {
            log.warn("로그인 실패 - 회원 정보 없음: {}", user.getAccount());
            // 로그인이 실패하면 401 Unauthorized 상태 반환
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "회원 정보가 없습니다!"));
        }

        // JWT 토큰 생성 (세션처럼 DB 조회하므로 최소 정보만 저장)
        Map<String, Object> payloads = new HashMap<>();
        payloads.put("id", sessionDto.getId());
        payloads.put("name", sessionDto.getName());
		payloads.put("method", sessionDto.getMethod());
        payloads.put("role", sessionDto.getRole());
        // Team 정보는 저장하지 않음 - DB에서 조회하므로

        String token = jwtUtil.createToken(sessionDto.getAccount(), payloads, 86400000L); // 24시간

        // JWT 토큰을 쿠키로 설정
        Cookie tokenCookie = new Cookie("JWT-TOKEN", token);
        tokenCookie.setHttpOnly(true);
        tokenCookie.setSecure(false); // 개발환경에서는 false
        tokenCookie.setPath("/");
        tokenCookie.setMaxAge(24 * 60 * 60); // 24시간
        response.addCookie(tokenCookie);

        log.info("로그인 성공 - 사용자: {}, JWT 토큰 생성 완료", sessionDto.getAccount());

        // 로그인이 성공하면 사용자 정보만 반환 (토큰은 httpOnly 쿠키에 저장됨)
        return ResponseEntity.ok(sessionDto);
    }

    @GetMapping("logout")
    public ResponseEntity<?> logout(HttpServletResponse response, HttpServletRequest request) throws IOException {
		UsersDto loginUser = (UsersDto) request.getAttribute("user");

        // JWT 토큰을 블랙리스트에 추가
        String token = jwtUtil.getTokenInCookie(request, "JWT-TOKEN");
        if (token != null && !token.isEmpty()) {
            jwtUtil.invalidateToken(token);
        }
        // 쿠키 삭제
        Cookie tokenCookie = new Cookie("JWT-TOKEN", "");
        tokenCookie.setMaxAge(0);
        tokenCookie.setPath("/");
        response.addCookie(tokenCookie);

        // 카카오 로그아웃 URL을 반환 (리다이렉트 대신)
		System.out.println(loginUser.getMethod());
		if ("KAKAO".equals(loginUser.getMethod())) {
			String kakaoLogoutUrl = KakaoLoginApi.getKakaoLogoutUrl(restApiKey, logoutRedirectUri);
        	return ResponseEntity.ok(Map.of("logoutUrl", kakaoLogoutUrl));
		}
        return ResponseEntity.ok("로그아웃됨");
    }
    
    @GetMapping("kakao-url")
    public ResponseEntity<?> getKakaoLoginUrl() {
        String kakaoUrl = "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=" + restApiKey + "&redirect_uri=" + redirectUri;
        return ResponseEntity.ok(Map.of("url", kakaoUrl));
    }
    
    @GetMapping("kakaoLogin")
    //리다이렉트를 해주지 않으면 데이터만 보여지는 페이지가 남는다.
    public void KakaoLogin(@RequestParam String code, HttpServletResponse response) throws IOException {

    	//액세스 토큰을 생성하고 그걸 이용해서 카카오 유저 가져오기
		String accessToken = KakaoLoginApi.getAccessToken(restTemplate, code, restApiKey, redirectUri);
		KakaoUser kakaoUser = KakaoLoginApi.getKakaoUser(restTemplate, accessToken);
		long id = kakaoUser.getId();
		String nickname = kakaoUser.getProperties().getNickname();

		//카카오 로그인 정보로 dto 생성
		UsersDto kakaoDto = UsersDto.builder()
				.account(String.valueOf(id))
				.password("-")
				.name(nickname)
				.email("kakao_" + id + "@kakao.com") // 카카오 로그인용 이메일 설정
				.method("KAKAO")
				.profileImage(kakaoUser.getProperties().getThumbnail_image())
				.build();

		// 해당 카카오 고유 ID의 회원이 있는지 체크하고 없으면 회원 DB에 저장
		authService.saveUser(kakaoDto);

		// JWT 토큰 생성 (세션처럼 DB 조회하므로 최소 정보만 저장)
		Map<String, Object> payloads = new HashMap<>();
		payloads.put("id", kakaoDto.getId());
		payloads.put("name", kakaoDto.getName());
		payloads.put("method", kakaoDto.getMethod());
		payloads.put("role", kakaoDto.getRole());
		// Team 정보는 저장하지 않음 - DB에서 조회하므로

		String token = jwtUtil.createToken(kakaoDto.getAccount(), payloads, 86400000L); // 24시간

		Cookie tokenCookie = new Cookie("JWT-TOKEN", token);
		tokenCookie.setHttpOnly(true);
		tokenCookie.setSecure(false);
		tokenCookie.setPath("/");
		tokenCookie.setMaxAge(24 * 60 * 60);
		response.addCookie(tokenCookie);

		// React 프론트엔드로 리다이렉트
		response.sendRedirect(frontUrl + "/");
    }
	@PostMapping("google")
	public void googleLogin(@RequestParam String code, HttpServletResponse response) throws IOException {
		try {
			// 1️⃣ Google Token Endpoint 요청
			String tokenUrl = "https://oauth2.googleapis.com/token";

			MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
			params.add("code", code);
			params.add("client_id", googleClientId);
			params.add("client_secret", googleClientSecret);
			params.add("redirect_uri", frontUrl); // frontUrl 하나로 통일
			params.add("grant_type", "authorization_code");

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

			HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

			Map<String, Object> tokenResponse = restTemplate.postForObject(tokenUrl, request, Map.class);
			String idToken = (String) tokenResponse.get("id_token");

			// 2️⃣ ID Token 검증
			com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier verifier =
				new com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier.Builder(
					new com.google.api.client.http.javanet.NetHttpTransport(),
					com.google.api.client.json.gson.GsonFactory.getDefaultInstance()
				)
				.setAudience(Collections.singletonList(googleClientId))
				.build();

			com.google.api.client.googleapis.auth.oauth2.GoogleIdToken idTokenObj = verifier.verify(idToken);
			if (idTokenObj == null) {
				response.sendRedirect(frontUrl + "/login?error=invalid_token");
				return;
			}

			com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload payload = idTokenObj.getPayload();
			String googleId = payload.getSubject();
			String email = payload.getEmail();
			String name = (String) payload.get("name");
			String picture = (String) payload.get("picture");

			// 3️⃣ 사용자 DTO 생성 및 DB 저장
			UsersDto googleUser = UsersDto.builder()
					.account("google_" + googleId)
					.password("-")
					.name(name)
					.email(email)
					.method("GOOGLE")
					.profileImage(picture)
					.build();

			authService.saveUser(googleUser);

			// 4️⃣ JWT 토큰 생성
			Map<String, Object> jwtPayloads = new HashMap<>();
			jwtPayloads.put("id", googleUser.getId());
			jwtPayloads.put("name", googleUser.getName());
			jwtPayloads.put("method", googleUser.getMethod());
			jwtPayloads.put("role", googleUser.getRole());

			String token = jwtUtil.createToken(googleUser.getAccount(), jwtPayloads, 86400000L);

			// 5️⃣ 쿠키에 JWT 저장
			Cookie tokenCookie = new Cookie("JWT-TOKEN", token);
			tokenCookie.setHttpOnly(true);
			tokenCookie.setSecure(true); // 배포환경 HTTPS에서 true
			tokenCookie.setPath("/");
			tokenCookie.setMaxAge(24 * 60 * 60);
			response.addCookie(tokenCookie);

			// 6️⃣ 프론트 리다이렉트
			response.sendRedirect(frontUrl + "/");
		} catch (Exception e) {
			e.printStackTrace();
			response.sendRedirect(frontUrl + "/login?error=server_error");
		}
	}

	// ===== Passwordless 관련 엔드포인트 =====

	/**
	 * Passwordless 로그인 시작 (6자리 인증 번호 생성)
	 * PasswordlessX1280 방식: 일회용 토큰 요청 후 로그인 시작
	 */
	@PostMapping("passwordless/login")
	public ResponseEntity<?> startPasswordlessLogin(@RequestBody Map<String, String> request,
													 HttpServletRequest httpRequest) {
		String account = request.get("account");
		log.info("Starting Passwordless login for user: {}", account);

		// 1. 일회용 토큰 요청 (PasswordlessX1280 방식)
		Map<String, String> tokenResult = passwordlessService.getOneTimeToken(account);
		if (!"true".equals(tokenResult.get("success"))) {
			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", tokenResult.get("message"));
			return ResponseEntity.badRequest().body(response);
		}

		String oneTimeToken = tokenResult.get("oneTimeToken");

		// 2. 로그인 시작 (일회용 토큰 사용)
		Map<String, String> result = passwordlessService.startLogin(account, oneTimeToken, httpRequest);

		if ("true".equals(result.get("success"))) {
			return ResponseEntity.ok(Map.of(
					"success", true,
					"sessionId", result.get("sessionId"),
					"authNumber", result.get("authNumber"),
					"wsUrl", result.get("wsUrl"),
					"pushConnectorToken", result.get("pushConnectorToken"),
					"term", Integer.parseInt(result.get("term"))
			));
		} else {
			Map<String, Object> response = new HashMap<>();
			response.put("success", false);
			response.put("message", result.get("message"));

			// 이미 인증 요청이 있는 경우
			if ("ALREADY_REQUESTED".equals(result.get("code"))) {
				response.put("code", "ALREADY_REQUESTED");
			}

			if ("Passwordless가 등록되지 않은 사용자입니다.".equals(result.get("message"))) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
			}
			return ResponseEntity.badRequest().body(response);
		}
	}

	/**
	 * Passwordless 로그인 결과 확인 (폴링)
	 */
	@PostMapping("passwordless/result")
	public ResponseEntity<?> checkPasswordlessResult(@RequestBody Map<String, String> request,
													  HttpServletResponse httpResponse) {
		String sessionId = request.get("sessionId");
		String account = request.get("account");

		Map<String, Object> result = passwordlessService.checkLoginResult(sessionId, account);
		String status = (String) result.get("status");

		Map<String, Object> response = new HashMap<>();
		response.put("status", status);

		if ("approved".equals(status)) {
			String token = (String) result.get("token");

			// JWT 토큰을 쿠키로 설정 (일반 로그인과 동일)
			Cookie tokenCookie = new Cookie("JWT-TOKEN", token);
			tokenCookie.setHttpOnly(true);
			tokenCookie.setSecure(false); // 개발환경에서는 false
			tokenCookie.setPath("/");
			tokenCookie.setMaxAge(24 * 60 * 60); // 24시간
			httpResponse.addCookie(tokenCookie);

			response.put("user", result.get("user"));
		} else if ("error".equals(status)) {
			response.put("message", result.get("message"));
		}

		// 타임아웃인 경우 400 응답
		if ("timeout".equals(status)) {
			return ResponseEntity.badRequest().body(response);
		}

		return ResponseEntity.ok(response);
	}

	/**
	 * Passwordless 로그인 취소
	 */
	@PostMapping("passwordless/cancel")
	public ResponseEntity<?> cancelPasswordlessLogin(@RequestBody Map<String, String> request) {
		String sessionId = request.get("sessionId");
		log.info("Canceling Passwordless login for session: {}", sessionId);

		boolean success = passwordlessService.cancelLogin(sessionId);
		return ResponseEntity.ok(Map.of("success", success));
	}

	/**
	 * Passwordless 등록 상태 확인
	 */
	@GetMapping("passwordless/status")
	public ResponseEntity<?> checkPasswordlessStatus(@RequestParam String account) {
		boolean isRegistered = passwordlessService.isPasswordlessRegistered(account);
		return ResponseEntity.ok(Map.of("registered", isRegistered));
	}

	/**
	 * Passwordless 관리 - 비밀번호 검증 (세션만 사용)
	 */
	@PostMapping("passwordless/manage-check")
	public ResponseEntity<?> passwordlessManageCheck(@RequestBody Map<String, String> request,
													  HttpServletRequest httpRequest) {
		String account = request.get("account");
		String password = request.get("password");

		// 비밀번호 검증
		UsersDto loginUser = UsersDto.builder()
				.account(account)
				.password(password)
				.build();

		UsersDto loginDto = authService.isUser(loginUser);
		if (loginDto == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					"success", false,
					"message", "비밀번호가 올바르지 않습니다."
			));
		}

		// 세션에 검증 완료 표시 (5분 유효)
		jakarta.servlet.http.HttpSession session = httpRequest.getSession(true);
		session.setAttribute("PasswordlessVerified", true);
		session.setAttribute("PasswordlessTime", System.currentTimeMillis());
		session.setAttribute("PasswordlessAccount", account);

		log.info("Passwordless verification completed for user: {}", account);

		return ResponseEntity.ok(Map.of("success", true));
	}

	/**
	 * Passwordless 등록 시작 (QR 코드 생성) - 세션 검증만
	 */
	@PostMapping("passwordless/register")
	public ResponseEntity<?> startPasswordlessRegister(@RequestBody Map<String, String> request,
														HttpServletRequest httpRequest) {
		String account = request.get("account");

		// 세션 검증
		jakarta.servlet.http.HttpSession session = httpRequest.getSession(false);
		if (session == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					"success", false,
					"message", "세션이 만료되었습니다."
			));
		}

		Boolean verified = (Boolean) session.getAttribute("PasswordlessVerified");
		Long verifiedTime = (Long) session.getAttribute("PasswordlessTime");
		String sessionAccount = (String) session.getAttribute("PasswordlessAccount");

		if (verified == null || !verified || verifiedTime == null || sessionAccount == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					"success", false,
					"message", "비밀번호 검증이 필요합니다."
			));
		}

		// 계정 일치 검증
		if (!sessionAccount.equals(account)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					"success", false,
					"message", "계정 정보가 일치하지 않습니다."
			));
		}

		// 타임아웃 검증 (5분)
		long nowTime = System.currentTimeMillis();
		long gapTime = nowTime - verifiedTime;

		if (gapTime > 5 * 60 * 1000) { // 5분 초과
			// 세션 정리
			session.removeAttribute("PasswordlessVerified");
			session.removeAttribute("PasswordlessTime");
			session.removeAttribute("PasswordlessAccount");

			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
					"success", false,
					"message", "인증 시간이 만료되었습니다."
			));
		}

		log.info("Session validated for user: {}, elapsed time: {}ms", account, gapTime);

		// Passwordless 등록 시작
		Map<String, String> result = passwordlessService.startRegistration(account);

		if ("true".equals(result.get("success"))) {
			return ResponseEntity.ok(Map.of(
					"success", true,
					"sessionId", result.get("sessionId"),
					"qr", result.get("qr"),
					"registerKey", result.get("registerKey"),
					"serverUrl", result.get("serverUrl"),
					"wsUrl", result.get("wsUrl"),
					"pushConnectorToken", result.get("pushConnectorToken")
			));
		} else {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", result.get("message")
			));
		}
	}

	/**
	 * Passwordless 등록 상태 확인 (폴링)
	 */
	@PostMapping("passwordless/register/status")
	public ResponseEntity<?> checkPasswordlessRegisterStatus(@RequestBody Map<String, String> request) {
		String sessionId = request.get("sessionId");

		boolean success = passwordlessService.completeRegistration(sessionId);
		return ResponseEntity.ok(Map.of("success", success));
	}

	/**
	 * Passwordless 해지
	 */
	@PostMapping("passwordless/unregister")
	public ResponseEntity<?> unregisterPasswordless(@RequestBody Map<String, String> request) {
		String account = request.get("account");

		// Passwordless 회원은 비밀번호가 자동 변경되어 알 수 없으므로 비밀번호 검증 없이 해지
		Map<String, String> result = passwordlessService.unregister(account);

		if ("true".equals(result.get("success"))) {
			return ResponseEntity.ok(Map.of("success", true));
		} else {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", result.get("message")
			));
		}
	}

	/**
	 * 비밀번호 찾기 - 아이디, 이메일로 사용자 확인 후 8자리 임시 비밀번호 생성
	 */
	@PostMapping("reset-password")
	public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
		String account = request.get("account");
		String email = request.get("email");

		if (account == null || email == null) {
			return ResponseEntity.badRequest().body(Map.of(
					"success", false,
					"message", "아이디, 이메일을 모두 입력해주세요."
			));
		}

		Map<String, Object> result = usersService.resetPassword(account, email);

		if ((Boolean) result.get("success")) {
			return ResponseEntity.ok(result);
		} else {
			return ResponseEntity.badRequest().body(result);
		}
	}

}
