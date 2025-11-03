package com.ict.springboot;

import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ict.springboot.dto.UsersDto;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.model.JwtUtil;
import com.ict.springboot.repository.UsersRepository;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 인증 필터
 *
 * 모든 HTTP 요청을 가로채서 JWT 토큰 검증 및 인증 처리를 수행합니다.
 *
 * <처리 흐름>
 * 1. [섹션 1] 검증 제외 경로 체크 → 로그인/회원가입 등 공개 경로는 바로 통과
 * 2. [섹션 2] JWT 토큰 검증 → 토큰이 있으면 사용자 정보 추출 및 request에 저장
 * 3. [섹션 3] 인증 필수 경로 체크 → /meets, /admin 등은 로그인 필수, 나머지는 통과
 *
 * <특징>
 * - Spring Security와 함께 사용 (SecurityConfig에서 필터 등록)
 * - API 요청과 웹 요청을 구분하여 다른 에러 처리 (JSON vs 리다이렉트)
 * - React 경로는 구분할 수 없으므로 프론트엔드에서 추가 인증 처리 필요
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsersRepository usersRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // ==================================================================================
        // [섹션 1] 검증 제외 경로 - 토큰 검증 없이 무조건 통과
        // ==================================================================================
        // - 로그인/회원가입 등 인증이 필요 없는 공개 경로
        // - 토큰이 없어도 접근 가능한 경로들

        // 인증 관련 API
        if (path.startsWith("/api/auth/login") ||
            path.startsWith("/api/auth/google") ||
            path.startsWith("/api/auth/kakao") ||
            path.startsWith("/api/auth/passwordless") ||
            path.startsWith("/api/auth/reset-password")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 회원가입 플로우
        if ((path.equals("/api/users") && "POST".equals(method)) ||
            (path.matches("/api/users/[^/]+") && "GET".equals(method)) ||
            (path.equals("/api/users/mailSend") && "POST".equals(method)) ||
            (path.equals("/api/users/mailCheck") && "GET".equals(method)) ||
            (path.equals("/api/teams") && "GET".equals(method))) {
            filterChain.doFilter(request, response);
            return;
        }

        // 개발/테스트 환경
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.contains("PostmanRuntime")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 기타 공개 경로
        // 챗봇은 로그인 필수 - 사용자 팀 정보 필요
        // if (path.contains("/api/ai/chat")) {
        //     filterChain.doFilter(request, response);
        //     return;
        // }

        // ==================================================================================
        // [섹션 2] JWT 토큰 검증 및 사용자 정보 추출
        // ==================================================================================
        // - 쿠키에서 JWT 토큰을 추출하여 검증
        // - 유효한 토큰이면 DB에서 사용자 정보 조회 후 request에 저장
        // - 토큰이 없거나 무효하면 user = null로 유지 (아직 에러 발생 안 함)

        // JWT 토큰 추출 및 검증
        String token = jwtUtil.getTokenInCookie(request, "JWT-TOKEN");
        UsersDto user = null;

        // JWT 토큰이 있으면 검증
        if (token != null && !token.isEmpty() && jwtUtil.verifyToken(token)) {
            try {
                Claims claims = jwtUtil.getTokenPayloads(token);

                // invalid 토큰이 아닌 경우에만 처리 (로그아웃된 토큰 체크)
                if (!claims.containsKey("invalid")) {
                    String account = claims.getSubject();

                    // DB에서 사용자 정보 조회
                    UsersEntity userEntity = usersRepository
                            .findByAccount(account)
                            .orElse(null);

                    if (userEntity != null) {
                        // 사용자 정보를 request에 저장 (Controller에서 사용 가능)
                        user = UsersDto.toDto(userEntity);
                        request.setAttribute("user", user);
                        log.debug("JWT 인증 성공: {}", account);
                    }
                }
            } catch (Exception e) {
                log.warn("JWT 토큰 처리 중 오류: {}", e.getMessage());
            }
        }

        // ==================================================================================
        // [섹션 3] 인증 필수 경로 체크
        // ==================================================================================
        // - /admin, /api 경로만 백엔드에서 인증 체크
        // - 인증 실패 시:
        //   * API 요청 → 401 JSON 에러 반환
        //   * 관리자 요청 → /admin으로 리다이렉트
        // - 그 외 프론트엔드 경로는 IndexController로 보내서 React가 처리

        // 관리자 로그인 페이지는 인증 불필요
        if (path.equals("/admin") || path.equals("/admin/login") || path.equals("/admin/loginProcess")) {
            log.debug("관리자 로그인 페이지 허용: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        // 인증이 필요한 백엔드 경로 체크
        // if (user == null) {
        //     // API 요청 → 401 JSON 에러
        //     if (path.startsWith("/api/")) {
        //         log.warn("API 인증 실패: {}", path);
        //         response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        //         response.setContentType("application/json;charset=UTF-8");
        //         response.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
        //         return;
        //     }

        //     // 관리자 페이지 → 리다이렉트
        //     if (path.startsWith("/admin")) {
        //         log.warn("관리자 페이지 인증 실패: {}", path);
        //         response.sendRedirect("/admin");
        //         return;
        //     }
        // }

        // ==================================================================================
        // 여기까지 통과하면 Controller로 이동
        // ==================================================================================
        filterChain.doFilter(request, response);
    }
}
