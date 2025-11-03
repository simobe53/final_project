package com.ict.springboot.service;


import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict.springboot.entity.UsersEntity;
import com.ict.springboot.model.JwtUtil;
import com.ict.springboot.repository.UsersRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Passwordless X1280 비즈니스 로직 서비스
 * 등록, 인증, 해제 등의 핵심 기능 제공
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PasswordlessService {

    @Value("${passwordless.recommend}")
    private String passwordlessRecommend;

    @Value("${passwordless.server-id}")
    private String serverId;

    @Value("${passwordless.server-key}")
    private String serverKey;

    @Value("${passwordless.rest-check-url}")
    private String restCheckUrl;

    @Value("${passwordless.push-connector-url}")
    private String pushConnectorUrl;

    private final UsersRepository usersRepository;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // API 엔드포인트 URL 상수
    private static final String IS_AP_URL = "/ap/rest/auth/isAp";
    private static final String JOIN_AP_URL = "/ap/rest/auth/joinAp";
    private static final String GET_TOKEN_URL = "/ap/rest/auth/getTokenForOneTime";
    private static final String WITHDRAWAL_AP_URL = "/ap/rest/auth/withdrawalAp";
    private static final String GET_SP_URL = "/ap/rest/auth/getSp";
    private static final String RESULT_URL = "/ap/rest/auth/result";
    private static final String CANCEL_URL = "/ap/rest/auth/cancel";

    // 세션 관리를 위한 임시 저장소 (실제 운영에서는 Redis 등 사용 권장)
    private final Map<String, PasswordlessSession> sessions = new ConcurrentHashMap<>();

    /**
     * Passwordless 등록 여부 확인
     * @param account 사용자 계정
     * @return 등록 여부
     */
    public boolean isPasswordlessRegistered(String account) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("userId", account);

            String response = callApi(IS_AP_URL, params);
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode dataNode = jsonNode.get("data");

            if (dataNode != null && dataNode.has("exist")) {
                return dataNode.get("exist").asBoolean();
            }
        } catch (Exception e) {
            log.error("Failed to check Passwordless registration for user: " + account, e);
        }
        return false;
    }

    /**
     * Passwordless 등록 시작 (QR 코드 생성) - 토큰 포함 버전
     * @param account 사용자 계정
     * @param token PasswordlessToken (5분 유효)
     * @return QR 코드 데이터 및 세션 정보
     */
    public Map<String, String> startRegistrationWithToken(String account, String token) {
        log.info("========== Passwordless Registration Started (with Token) ==========");
        log.info("User: {}, Token: {}", account, token);

        Map<String, String> result = new HashMap<>();

        // 1. 사용자 확인
        log.debug("Step 1: Checking user exists in database");
        UsersEntity user = usersRepository.findByAccount(account).orElse(null);
        if (user == null) {
            log.error("User not found: {}", account);
            result.put("success", "false");
            result.put("message", "사용자를 찾을 수 없습니다.");
            return result;
        }
        log.debug("User found - ID: {}, Name: {}", user.getId(), user.getName());

        // 2. 등록 여부 확인
        log.debug("Step 2: Checking if Passwordless already registered via isAp API");
        try {
            Map<String, String> params = new HashMap<>();
            params.put("userId", account);

            String response = callApi(IS_AP_URL, params);
            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode dataNode = jsonNode.get("data");

            if (dataNode != null && dataNode.has("exist")) {
                boolean exist = dataNode.get("exist").asBoolean();
                log.info("isAp check result - exist: {}", exist);

                if (exist) {
                    log.warn("Passwordless already registered for user: {}", account);
                    result.put("success", "false");
                    result.put("message", "이미 Passwordless가 등록되어 있습니다.");
                    return result;
                }
            }
        } catch (Exception e) {
            log.error("Failed to check Passwordless registration status", e);
            result.put("success", "false");
            result.put("message", "등록 상태 확인 중 오류가 발생했습니다.");
            return result;
        }

        log.debug("User is not registered for Passwordless yet");

        // 3. QR 등록 요청 (PasswordlessX1280 방식)
        log.info("Step 3: Requesting QR registration from Passwordless server");
        try {
            Map<String, String> params = new HashMap<>();
            params.put("userId", account);
            log.debug("API Request - Endpoint: {}, Params: {}", JOIN_AP_URL, params);

            String response = callApi(JOIN_AP_URL, params);
            log.debug("API Response received: {}", response);

            JsonNode jsonNode = objectMapper.readTree(response);
            String responseCode = jsonNode.has("code") ? jsonNode.get("code").asText() : "unknown";
            log.debug("Response code: {}", responseCode);

            if (jsonNode.has("code") && jsonNode.get("code").asDouble() == 0.0) {
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode != null) {
                    // QR 코드 정보 추출
                    String qr = dataNode.has("qr") ? dataNode.get("qr").asText() : null;
                    String registerKey = dataNode.has("registerKey") ? dataNode.get("registerKey").asText() : null;
                    String serverUrl = restCheckUrl;
                    String pushConnectorToken = dataNode.has("pushConnectorToken") ? dataNode.get("pushConnectorToken").asText() : null;

                    log.info("QR code generated successfully");
                    log.debug("RegisterKey: {}", registerKey);
                    log.debug("ServerUrl: {}", serverUrl);
                    log.debug("PushConnectorToken: {}", pushConnectorToken != null ? "exists" : "null");
                    log.debug("QR data length: {}", qr != null ? qr.length() : 0);

                    // 세션 생성
                    String sessionId = System.currentTimeMillis() + "_sessionId";
                    PasswordlessSession session = new PasswordlessSession();
                    session.setSessionId(sessionId);
                    session.setUserId(account);
                    session.setType("REGISTER");
                    session.setStatus("PENDING");
                    session.setCreatedAt(System.currentTimeMillis());
                    sessions.put(sessionId, session);

                    log.info("Session created - SessionId: {}, Type: REGISTER, Status: PENDING", sessionId);
                    log.debug("Total active sessions: {}", sessions.size());

                    // 결과 반환
                    result.put("success", "true");
                    result.put("qr", qr);
                    result.put("registerKey", registerKey);
                    result.put("serverUrl", serverUrl);
                    result.put("sessionId", sessionId);
                    result.put("wsUrl", pushConnectorUrl);
                    result.put("pushConnectorToken", pushConnectorToken);

                    log.info("========== Registration QR Generated Successfully ==========");
                } else {
                    log.error("Response data node is null");
                    result.put("success", "false");
                    result.put("message", "QR 등록 응답 데이터가 없습니다.");
                }
            } else {
                String code = jsonNode.has("code") ? jsonNode.get("code").asText() : "unknown";
                String msg = jsonNode.has("msg") ? jsonNode.get("msg").asText() : "Unknown error";
                log.error("Passwordless server returned error - Code: {}, Message: {}", code, msg);
                result.put("success", "false");
                result.put("message", "[" + code + "] " + msg);
            }
        } catch (Exception e) {
            log.error("========== Registration Failed ==========");
            log.error("User: {}", account);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Stack trace:", e);
            result.put("success", "false");
            result.put("message", "Passwordless 등록 요청에 실패했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * Passwordless 등록 시작 (QR 코드 생성) - 기존 메서드 (하위 호환성)
     * @param account 사용자 계정
     * @return QR 코드 데이터 및 세션 정보
     *
     * 참고: 비밀번호 검증은 AuthController에서 먼저 수행됨 (authService.isUser())
     */
    public Map<String, String> startRegistration(String account) {
        log.info("========== Passwordless Registration Started ==========");
        log.info("User: {}", account);

        Map<String, String> result = new HashMap<>();

        // 1. 사용자 확인
        log.debug("Step 1: Checking user exists in database");
        UsersEntity user = usersRepository.findByAccount(account).orElse(null);
        if (user == null) {
            log.error("User not found: {}", account);
            result.put("success", "false");
            result.put("message", "사용자를 찾을 수 없습니다.");
            return result;
        }
        log.debug("User found - ID: {}, Name: {}", user.getId(), user.getName());

        // 2. 이미 등록된 경우 체크
        log.debug("Step 2: Checking if Passwordless already registered via isAp API");
        if (isPasswordlessRegistered(account)) {
            log.warn("Passwordless already registered for user: {}", account);
            result.put("success", "false");
            result.put("message", "이미 Passwordless가 등록되어 있습니다.");
            return result;
        }
        log.debug("User is not registered for Passwordless yet");

        // 3. QR 등록 요청 (PasswordlessX1280 방식)
        log.info("Step 3: Requesting QR registration from Passwordless server");
        try {
            Map<String, String> params = new HashMap<>();
            params.put("userId", account);
            log.debug("API Request - Endpoint: {}, Params: {}", JOIN_AP_URL, params);

            String response = callApi(JOIN_AP_URL, params);
            log.debug("API Response received: {}", response);

            JsonNode jsonNode = objectMapper.readTree(response);
            String responseCode = jsonNode.has("code") ? jsonNode.get("code").asText() : "unknown";
            log.debug("Response code: {}", responseCode);

            if (jsonNode.has("code") && jsonNode.get("code").asDouble() == 0.0) {
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode != null) {
                    // QR 코드 정보 추출
                    String qr = dataNode.has("qr") ? dataNode.get("qr").asText() : null;
                    String registerKey = dataNode.has("registerKey") ? dataNode.get("registerKey").asText() : null;
                    // serverUrl은 환경변수에서 가져온 값 사용 (서버 응답값은 프로토콜/포트 누락 가능)
                    String serverUrl = restCheckUrl;
                    String pushConnectorToken = dataNode.has("pushConnectorToken") ? dataNode.get("pushConnectorToken").asText() : null;

                    log.info("QR code generated successfully");
                    log.debug("RegisterKey: {}", registerKey);
                    log.debug("ServerUrl: {}", serverUrl);
                    log.debug("PushConnectorToken: {}", pushConnectorToken != null ? "exists" : "null");
                    log.debug("QR data length: {}", qr != null ? qr.length() : 0);

                    // 세션 생성
                    String sessionId = System.currentTimeMillis() + "_sessionId";
                    PasswordlessSession session = new PasswordlessSession();
                    session.setSessionId(sessionId);
                    session.setUserId(account);
                    session.setType("REGISTER");
                    session.setStatus("PENDING");
                    session.setCreatedAt(System.currentTimeMillis());
                    sessions.put(sessionId, session);

                    log.info("Session created - SessionId: {}, Type: REGISTER, Status: PENDING", sessionId);
                    log.debug("Total active sessions: {}", sessions.size());

                    // pushConnector로 푸시 요청 전송
                    // 주석처리: 프론트엔드에서 WebSocket으로 직접 연결하므로 불필요
                    // try {
                    //     sendPushNotification(account, registerKey, pushConnectorToken);
                    //     log.info("Push notification sent to mobile app");
                    // } catch (Exception e) {
                    //     log.error("Failed to send push notification: {}", e.getMessage());
                    // }

                    // 결과 반환
                    result.put("success", "true");
                    result.put("qr", qr);
                    result.put("registerKey", registerKey);
                    result.put("serverUrl", serverUrl);
                    result.put("sessionId", sessionId);
                    result.put("wsUrl", pushConnectorUrl);
                    result.put("pushConnectorToken", pushConnectorToken);

                    log.info("========== Registration QR Generated Successfully ==========");
                } else {
                    log.error("Response data node is null");
                    result.put("success", "false");
                    result.put("message", "QR 등록 응답 데이터가 없습니다.");
                }
            } else {
                String code = jsonNode.has("code") ? jsonNode.get("code").asText() : "unknown";
                String msg = jsonNode.has("msg") ? jsonNode.get("msg").asText() : "Unknown error";
                log.error("Passwordless server returned error - Code: {}, Message: {}", code, msg);
                result.put("success", "false");
                result.put("message", "[" + code + "] " + msg);
            }
        } catch (Exception e) {
            log.error("========== Registration Failed ==========");
            log.error("User: {}", account);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Stack trace:", e);
            result.put("success", "false");
            result.put("message", "Passwordless 등록 요청에 실패했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * Passwordless 등록 완료 처리
     * @param sessionId 세션 ID
     * @return 처리 결과
     */
    public boolean completeRegistration(String sessionId) {
        log.info("========== Completing Passwordless Registration (Reference Code Style) ==========");
        log.debug("SessionId: {}", sessionId);

        // 1. 세션 확인
        log.debug("Step 1: Checking session exists");
        PasswordlessSession session = sessions.get(sessionId);
        if (session == null) {
            log.error("Session not found: {}", sessionId);
            return false;
        }
        if (!"REGISTER".equals(session.getType())) {
            log.error("Invalid session type. Expected: REGISTER, Actual: {}", session.getType());
            return false;
        }
        log.debug("Session found - UserId: {}, Type: {}, Status: {}",
                  session.getUserId(), session.getType(), session.getStatus());

        // 2. 사용자 확인
        log.debug("Step 2: Checking user exists");
        UsersEntity user = usersRepository.findByAccount(session.getUserId()).orElse(null);
        if (user == null) {
            log.error("User not found: {}", session.getUserId());
            return false;
        }
        log.debug("User found - ID: {}, Name: {}", user.getId(), user.getName());

        // 3. Passwordless 등록 여부 확인
        log.info("Step 3: Verifying registration with isAp API (QRReg=T)");
        boolean isRegistered = false;
        try {
            Map<String, String> params = new HashMap<>();
            params.put("userId", session.getUserId());
            params.put("QRReg", "T");
            log.debug("API Request - Endpoint: {}, Params: {}", IS_AP_URL, params);

            String response = callApi(IS_AP_URL, params);
            log.debug("API Response: {}", response);

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode dataNode = jsonNode.get("data");

            if (dataNode != null && dataNode.has("exist")) {
                isRegistered = dataNode.get("exist").asBoolean();
                log.info("isAp check result - exist: {}", isRegistered);
            } else {
                log.warn("Response data node is null or missing 'exist' field");
            }
        } catch (Exception e) {
            log.error("Failed to verify registration status");
            log.error("User: {}", session.getUserId());
            log.error("Error: {}", e.getMessage(), e);
        }

        // 4. 등록 완료 시 비밀번호 변경
        if (isRegistered) {
            log.info("Step 4: QR Registration Complete - Changing password");

            // 비밀번호 자동 변경
            log.info("Changing password after QR registration");
            String newPassword = System.currentTimeMillis() + ":" + session.getUserId();
            user.setPassword(newPassword);
            usersRepository.save(user);
            log.info("Password changed for security - Format: timestamp:userId");

            // 세션 정리
            sessions.remove(sessionId);
            log.info("Session removed - Remaining sessions: {}", sessions.size());

            log.info("========== Registration Completed Successfully ==========");
            return true;
        } else {
            log.warn("Registration not confirmed by Passwordless server");
            log.warn("========== Registration Completion Failed ==========");
        }

        return false;
    }

    /**
     * Passwordless 등록 해제
     * @param account 사용자 계정
     * @return 처리 결과
     *
     * 참고: 비밀번호 검증은 AuthController에서 먼저 수행됨 (authService.isUser())
     */
    public Map<String, String> unregister(String account) {
        Map<String, String> result = new HashMap<>();

        // 1. 사용자 확인
        UsersEntity user = usersRepository.findByAccount(account).orElse(null);
        if (user == null) {
            result.put("success", "false");
            result.put("message", "사용자를 찾을 수 없습니다.");
            return result;
        }

        // 2. Passwordless 등록 여부 확인
        if (!isPasswordlessRegistered(account)) {
            result.put("success", "false");
            result.put("message", "Passwordless가 등록되어 있지 않습니다.");
            return result;
        }

        // 4. Passwordless 해제 요청 (PasswordlessX1280 방식)
        boolean success = false;
        try {
            Map<String, String> params = new HashMap<>();
            params.put("userId", account);

            String response = callApi(WITHDRAWAL_AP_URL, params);
            JsonNode jsonNode = objectMapper.readTree(response);

            success = jsonNode.has("code") && jsonNode.get("code").asDouble() == 0.0;
        } catch (Exception e) {
            log.error("Failed to unregister Passwordless for user: " + account, e);
        }

        if (success) {
            // Recommend 모드인 경우 새 비밀번호 설정 필요
            if ("1".equals(passwordlessRecommend)) {
                // 사용자가 새 비밀번호를 설정해야 함
                result.put("needNewPassword", "true");
            }

            result.put("success", "true");
            result.put("message", "Passwordless가 해제되었습니다.");
        } else {
            result.put("success", "false");
            result.put("message", "Passwordless 해제에 실패했습니다.");
        }

        return result;
    }

    /**
     * Passwordless 일회용 토큰 요청
     * @param account 사용자 계정
     * @return 일회용 토큰
     */
    public Map<String, String> getOneTimeToken(String account) {
        Map<String, String> result = new HashMap<>();

        // 1. 사용자 확인
        UsersEntity user = usersRepository.findByAccount(account).orElse(null);
        if (user == null) {
            result.put("success", "false");
            result.put("message", "사용자를 찾을 수 없습니다.");
            return result;
        }

        // 2. 일회용 토큰 요청 (PasswordlessX1280 방식)
        try {
            Map<String, String> params = new HashMap<>();
            params.put("userId", account);

            String response = callApi(GET_TOKEN_URL, params);
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("code") && jsonNode.get("code").asDouble() == 0.0) {
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode != null && dataNode.has("token")) {
                    String encryptedToken = dataNode.get("token").asText();
                    // AES 복호화
                    String oneTimeToken = decryptAES(encryptedToken, serverKey);

                    if (oneTimeToken != null) {
                        result.put("success", "true");
                        result.put("oneTimeToken", oneTimeToken);
                    } else {
                        result.put("success", "false");
                        result.put("message", "토큰 복호화에 실패했습니다.");
                    }
                } else {
                    result.put("success", "false");
                    result.put("message", "토큰 데이터가 없습니다.");
                }
            } else {
                String code = jsonNode.has("code") ? jsonNode.get("code").asText() : "unknown";
                String msg = jsonNode.has("msg") ? jsonNode.get("msg").asText() : "Unknown error";
                result.put("success", "false");
                result.put("message", "[" + code + "] " + msg);
            }
        } catch (Exception e) {
            log.error("Failed to get one-time token for user: " + account, e);
            result.put("success", "false");
            result.put("message", "일회용 토큰 요청에 실패했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * Passwordless 로그인 시작 (6자리 숫자 생성)
     * @param account 사용자 계정
     * @param oneTimeToken 일회용 토큰
     * @param request HTTP 요청
     * @return 인증 정보
     */
    public Map<String, String> startLogin(String account, String oneTimeToken, HttpServletRequest request) {
        log.info("========== Passwordless Login Started ==========");
        log.info("User: {}", account);

        Map<String, String> result = new HashMap<>();

        // 1. 사용자 확인
        log.debug("Step 1: Checking user exists");
        UsersEntity user = usersRepository.findByAccount(account).orElse(null);
        if (user == null) {
            log.error("User not found: {}", account);
            result.put("success", "false");
            result.put("message", "사용자를 찾을 수 없습니다.");
            return result;
        }
        log.debug("User found - ID: {}, Name: {}", user.getId(), user.getName());

        // 2. Passwordless 등록 여부 확인
        log.debug("Step 2: Checking Passwordless registration status via API");
        if (!isPasswordlessRegistered(account)) {
            log.warn("Passwordless not registered for user: {}", account);
            result.put("success", "false");
            result.put("message", "Passwordless가 등록되지 않은 사용자입니다.");
            return result;
        }
        log.debug("User is registered for Passwordless");

        // 3. 클라이언트 IP 추출
        String clientIp = getClientIp(request);
        log.debug("Step 3: Client IP: {}", clientIp);

        // 4. 세션 ID 생성
        String sessionId = System.currentTimeMillis() + "_sessionId";
        log.debug("Step 4: SessionId generated: {}", sessionId);

        // 5. 인증 요청 (PasswordlessX1280 방식)
        log.info("Step 5: Requesting authentication from Passwordless server");
        try {
            String random = UUID.randomUUID().toString();
            log.debug("Random UUID generated: {}", random);

            // getSp 호출 시 sessionId 포함 (서버가 이 ID로 세션 생성)
            Map<String, String> params = new HashMap<>();
            params.put("userId", account);
            params.put("token", oneTimeToken);
            params.put("clientIp", clientIp);
            params.put("sessionId", sessionId);  // 필수: 서버가 이 sessionId로 세션 생성
            params.put("random", random);
            params.put("password", "");
            log.debug("API Request - Endpoint: {}, Params: {}", GET_SP_URL, params);

            String response = callApi(GET_SP_URL, params);
            log.debug("API Response received: {}", response);

            JsonNode jsonNode = objectMapper.readTree(response);
            String responseCode = jsonNode.has("code") ? jsonNode.get("code").asText() : "unknown";
            log.debug("Response code: {}", responseCode);

            if (jsonNode.has("code") && jsonNode.get("code").asDouble() == 0.0) {
                // Passwordless 서버가 반환한 sessionId 추출 (중요!)
                String serverSessionId = jsonNode.has("sessionId") ? jsonNode.get("sessionId").asText() : null;
                if (serverSessionId != null) {
                    sessionId = serverSessionId;  // 서버가 생성한 sessionId 사용
                    log.info("Using sessionId from Passwordless server: {}", sessionId);
                } else {
                    log.warn("Passwordless server did not return sessionId, using client-generated: {}", sessionId);
                }

                JsonNode dataNode = jsonNode.get("data");
                if (dataNode != null) {
                    String authNum = dataNode.has("authNum") ? dataNode.get("authNum").asText() : null;
                    String servicePassword = dataNode.has("servicePassword") ? dataNode.get("servicePassword").asText() : authNum;
                    String pushConnectorToken = dataNode.has("pushConnectorToken") ? dataNode.get("pushConnectorToken").asText() : null;
                    Integer term = dataNode.has("term") ? dataNode.get("term").asInt() : 180;

                    log.info("Authentication number generated successfully");
                    log.debug("AuthNumber: {}", authNum);
                    log.debug("ServicePassword: {}", servicePassword);
                    log.debug("Term: {} seconds", term);
                    log.debug("PushConnectorToken: {}", pushConnectorToken != null ? "exists" : "null");
                    log.debug("Final SessionId: {}", sessionId);

                    // 세션 저장
                    PasswordlessSession session = new PasswordlessSession();
                    session.setSessionId(sessionId);
                    session.setUserId(account);
                    session.setType("LOGIN");
                    session.setStatus("PENDING");
                    session.setAuthNumber(servicePassword);
                    session.setCreatedAt(System.currentTimeMillis());
                    sessions.put(sessionId, session);

                    log.info("Session created - SessionId: {}, Type: LOGIN, Status: PENDING", sessionId);
                    log.debug("Total active sessions: {}", sessions.size());

                    // 결과 반환
                    result.put("success", "true");
                    result.put("sessionId", sessionId);
                    result.put("authNumber", servicePassword);
                    result.put("wsUrl", pushConnectorUrl);
                    result.put("pushConnectorToken", pushConnectorToken);
                    result.put("term", String.valueOf(term));

                    log.info("========== Login Authentication Request Sent ==========");
                } else {
                    log.error("Response data node is null");
                    result.put("success", "false");
                    result.put("message", "인증 응답 데이터가 없습니다.");
                }
            } else {
                String code = jsonNode.has("code") ? jsonNode.get("code").asText() : "unknown";
                String msg = jsonNode.has("msg") ? jsonNode.get("msg").asText() : "Unknown error";

                // 이미 인증 요청이 있는 경우 (200.6)
                if ("200.6".equals(code)) {
                    log.warn("Authentication already requested - Code: {}, Message: {}", code, msg);
                    result.put("success", "false");
                    result.put("code", "ALREADY_REQUESTED");
                    result.put("message", msg);
                } else {
                    log.error("Passwordless server returned error - Code: {}, Message: {}", code, msg);
                    result.put("success", "false");
                    result.put("message", "[" + code + "] " + msg);
                }
            }
        } catch (Exception e) {
            log.error("========== Login Request Failed ==========");
            log.error("User: {}", account);
            log.error("Error type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Stack trace:", e);
            result.put("success", "false");
            result.put("message", "인증 요청에 실패했습니다: " + e.getMessage());
        }

        return result;
    }

    /**
     * Passwordless 로그인 결과 확인 (폴링)
     * @param sessionId 세션 ID
     * @param account 사용자 계정
     * @return 인증 결과
     */
    public Map<String, Object> checkLoginResult(String sessionId, String account) {
        Map<String, Object> result = new HashMap<>();

        // 1. 세션 확인
        PasswordlessSession session = sessions.get(sessionId);
        if (session == null || !account.equals(session.getUserId())) {
            result.put("status", "error");
            result.put("message", "세션을 찾을 수 없습니다.");
            return result;
        }

        // 2. 타임아웃 체크 (3분)
        long elapsedTime = System.currentTimeMillis() - session.getCreatedAt();
        if (elapsedTime > 180000) { // 180초
            sessions.remove(sessionId);
            result.put("status", "timeout");
            return result;
        }

        // 3. Passwordless 서버에 결과 확인 (PasswordlessX1280 방식)
        String authStatus = "pending";
        try {
            Map<String, String> params = new HashMap<>();
            params.put("userId", account);
            params.put("sessionId", sessionId);

            String response = callApi(RESULT_URL, params);
            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.has("data")) {
                JsonNode dataNode = jsonNode.get("data");
                if (dataNode.has("auth")) {
                    String auth = dataNode.get("auth").asText();
                    if ("Y".equals(auth)) {
                        authStatus = "approved";
                    } else if ("N".equals(auth)) {
                        authStatus = "rejected";
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to check authentication result for sessionId: " + sessionId, e);
            authStatus = "error";
        }

        if ("approved".equals(authStatus)) {
            // 4. 사용자 정보 조회
            UsersEntity user = usersRepository.findByAccount(account).orElse(null);
            if (user == null) {
                result.put("status", "error");
                result.put("message", "사용자를 찾을 수 없습니다.");
                return result;
            }

            // 5. 로그인 성공 시 비밀번호 자동 변경
            log.info("Passwordless login success - Changing password for security");
            String newPassword = System.currentTimeMillis() + ":" + account;
            user.setPassword(newPassword);
            usersRepository.save(user);
            log.info("Password changed for user: {} - Format: timestamp:userId", account);

            // 6. JWT 토큰 생성
            Map<String, Object> payloads = new HashMap<>();
            payloads.put("id", user.getId());
            payloads.put("name", user.getName());
            payloads.put("method", user.getMethod());
            payloads.put("role", user.getRole());

            String token = jwtUtil.createToken(account, payloads, 86400000L); // 24시간

            // 7. 사용자 정보 설정
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("account", user.getAccount());
            userInfo.put("name", user.getName());
            userInfo.put("email", user.getEmail());
            userInfo.put("role", user.getRole());
            userInfo.put("team", user.getTeam());
            userInfo.put("gender", user.getGender());
            userInfo.put("profileImage", user.getProfileImage());
            userInfo.put("method", user.getMethod());

            result.put("status", "approved");
            result.put("token", token);
            result.put("user", userInfo);

            // 세션 정리
            sessions.remove(sessionId);
        } else if ("rejected".equals(authStatus)) {
            result.put("status", "rejected");
            sessions.remove(sessionId);
        } else {
            result.put("status", "pending");
        }

        return result;
    }

    /**
     * Passwordless 로그인 취소
     * @param sessionId 세션 ID
     * @return 성공 여부
     */
    public boolean cancelLogin(String sessionId) {
        PasswordlessSession session = sessions.get(sessionId);
        if (session == null) {
            return false;
        }

        boolean success = false;
        try {
            Map<String, String> params = new HashMap<>();
            params.put("userId", session.getUserId());
            params.put("sessionId", sessionId);

            String response = callApi(CANCEL_URL, params);
            JsonNode jsonNode = objectMapper.readTree(response);

            success = jsonNode.has("code") && jsonNode.get("code").asDouble() == 0.0;
        } catch (Exception e) {
            log.error("Failed to cancel authentication for sessionId: " + sessionId, e);
        }

        if (success) {
            sessions.remove(sessionId);
        }

        return success;
    }

    /**
     * REST API 호출 (공통)
     * @param endpoint API 엔드포인트
     * @param params 요청 파라미터
     * @return API 응답 (JSON 문자열)
     */
    private String callApi(String endpoint, Map<String, String> params) {
        String retVal = "";

        try {
            String requestURL = restCheckUrl + endpoint;
            URIBuilder b = new URIBuilder(requestURL);

            // 파라미터 추가
            for (Map.Entry<String, String> entry : params.entrySet()) {
                b.addParameter(entry.getKey(), entry.getValue());
            }

            java.net.URI uri = b.build();

            if (!endpoint.equals(RESULT_URL)) {
                log.info("Calling Passwordless API: {} with params: {}", endpoint, params);
                log.debug("Full URL: {}", uri.toString());
            }

            // Apache HttpClient 사용
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            org.apache.http.HttpResponse response;

            // POST 요청
            HttpPost httpPost = new HttpPost(uri);
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
            response = httpClient.execute(httpPost);

            HttpEntity entity = response.getEntity();
            retVal = EntityUtils.toString(entity);

            // 응답 로깅 (result API는 빈번하므로 제외)
            if (!endpoint.equals(RESULT_URL)) {
                log.info("API Response - Status: {}, Body: {}", response.getStatusLine().getStatusCode(), retVal);
            }

            httpClient.close();
        } catch (Exception e) {
            log.error("Passwordless API call failed - Endpoint: {}, Params: {}, Error: {}",
                      endpoint, params, e.getMessage());
            log.error("Stack trace:", e);
        }

        return retVal;
    }

    /**
     * AES 복호화 (One-Time Token 복호화용)
     * @param encrypted 암호화된 문자열
     * @param key 복호화 키
     * @return 복호화된 문자열
     */
    private String decryptAES(String encrypted, String key) {
        try {
            byte[] keyBytes = key.getBytes();
            byte[] ivBytes = keyBytes; // IV와 Key를 동일하게 사용

            SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));

            byte[] decodedBytes = Base64.getDecoder().decode(encrypted);
            byte[] decrypted = cipher.doFinal(decodedBytes);

            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            log.error("AES decryption failed", e);
            return null;
        }
    }

    /**
     * 클라이언트 IP 추출
     * @param request HTTP 요청
     * @return IP 주소
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // IPv6 localhost를 IPv4로 변환
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    /**
     * Passwordless 세션 정보 (내부 클래스)
     */
    private static class PasswordlessSession {
        private String sessionId;
        private String userId;
        private String type; // REGISTER or LOGIN
        private String status; // PENDING, APPROVED, REJECTED
        private String authNumber;
        private long createdAt;

        // Getters and Setters
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getAuthNumber() { return authNumber; }
        public void setAuthNumber(String authNumber) { this.authNumber = authNumber; }

        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    }
}