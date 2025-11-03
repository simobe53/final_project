package com.ict.springboot.websocket;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ict.springboot.dto.ChatDto;
import com.ict.springboot.dto.UsersDto;

//<<웹소켓 서버>>
@Component
public class WebSocketServer extends TextWebSocketHandler {

	//<<< 접속한 클라이언트를 저장하기 위한 속성(필드)>>>
	// 연결된 클라이언트 하나당 하나씩 WebSocketSession이 할당된다
	// 키는 웹소켓의 세션 아이디,값은 WebSocketSession객체
	private Map<Long, Map<Long, WebSocketSession>> simulationClients= new ConcurrentHashMap<>();
	Long userId;
	Long simulationId;
	UsersDto user = new UsersDto();
	ChatDto chat = new ChatDto();
	Map<Long, WebSocketSession> clients;
	
	@Value("${fastapi.server-url}")
    private String FASTAPI_SERVER_URL;

	// 유저들의 메시지 분석
	private static final Deque<String> recentMessages = new ArrayDeque<>();
    private static final int MESSAGE_SIZE = 10;
	private final RestTemplate restTemplate = new RestTemplate();


	//세션에 저장되어 있는 유저 정보를 가져온다.
    private UsersDto userSettting(@NonNull WebSocketSession session) {
        Object userObj = session.getAttributes().get("user");
        if (userObj == null) throw new IllegalStateException("세션에 유저 정보가 없습니다.");
        return (UsersDto) userObj;
    }

	public Map<Long, WebSocketSession> getClients(Long simulationId) {
		return simulationClients.get(simulationId);
	}

	public Long getSimulationId(@NonNull WebSocketSession session) {
		String query = session.getUri().getQuery(); 
		return Long.valueOf(query.substring(query.lastIndexOf("simulationId="), query.length()).replace("simulationId=", ""));
	}
	
	//클라이언트와 연결되었을 때 호출되는 콜백 메소드
	@Override
	public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
		
		user = userSettting(session);
		userId = user.getId();
		simulationId = getSimulationId(session);
		
		simulationClients.putIfAbsent(simulationId, new ConcurrentHashMap<>()); // 없을시 방생성
		Map<Long, WebSocketSession> clients = getClients(simulationId);

		
		//클라이언트에 같은 id값이 있다면 덮어써버린다.
		clients.put(userId, session);
		
		//입장 메시지를 출력하는 메소드 (출력하지않도록한다)
		// enterMessage(session);
	}

	//<<< 연결된 클라이언트로부터 메시지를 받을 때마다 자동 호출되는 콜백 메소드 >>>
	@Override
	protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
		// 현재 채팅방 세팅
		simulationId = getSimulationId(session);
		user = userSettting(session);

		// 1️⃣ 유저 메시지 저장 또는 브로드캐스트
		String payload = message.getPayload();
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree(payload);
		String type = json.has("type") ? json.get("type").asText() : "chat";
		if ("location".equals(type)) {
			handleLocationMessage(json, getClients(simulationId), user);
			return;
		}

		String chatMessage = json.has("content") ? json.get("content").asText() : "";
		if (!chatMessage.isBlank()) {
			addRecentMessage(chatMessage);
		}
        // 2️⃣ AI 감정 분석
        if (recentMessages.size() >= MESSAGE_SIZE && isToxic(recentMessages)) {
            // 3️⃣ 서버가 공지성 워닝 메시지 전송
            broadcastToAll("cleanBot", "대화가 과열되고 있습니다. 서로를 존중해주세요", Integer.MIN_VALUE, simulationId);
			recentMessages.clear();
        }

		//클라이언트에 메시지를 보내는 메소드
		createMessage(session, message);
	}

	//클라이언트와 통신장애가 발생하면 자동으로 호출되는 메소드
	@Override
	public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable e) throws Exception {
		user = userSettting(session);
		userId = user.getId();
		System.out.println(userId + "와 통신 장애 발생:"+e.getMessage());
	}


	//클라이언트와 연결이 끊어졌을 때 호출되는 콜백 메소드
	@Override
	public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
		
       	UsersDto user = userSettting(session);
        Long simulationId = getSimulationId(session);
		clients = getClients(simulationId);
		Map<Long, WebSocketSession> clients = getClients(simulationId);
		WebSocketSession clientSession = clients.get(user.getId());
		
		//nullPointerException 경고 방지를 위한 코드
		if(clientSession != null) {
			exitMessage(session);
			System.out.println(user.getName() + "님과의 연결이 끊어 졌습니다");
		}

		if (clients != null) {
			clients.remove(user.getId());
		}
	
	}

	private void addRecentMessage(String message) {
        if (recentMessages.size() >= MESSAGE_SIZE) {
            recentMessages.removeFirst(); // 가장 오래된 메시지 제거
        }
        recentMessages.addLast(message);
    }

	private boolean isToxic(Deque<String> messages) {
		// ✅ 간단한 예시: 최근 10개 중 비속어 포함 메시지가 7개 이상이면 toxic
		try {
			Map<String, Object> requestBody = Map.of("messages", messages);

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

			ResponseEntity<Map> response = restTemplate.postForEntity(
					FASTAPI_SERVER_URL+"/api/chat/isToxic", request, Map.class);

			return (Boolean) response.getBody().get("toxic");
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
    }

	/**
	 * 공지 메시지 브로드캐스트
	 * @param type 메시지 타입 (notice, aiBot, cleanBot)
	 * @param msg 메시지 내용
	 * @param teamId 공지를 받을 팀 id (0이면 전체)
	 * @param sId 시뮬레이션 id (0이면 전체)
	 */
	public void broadcastToAll(String type, String msg, int isHome, Long sId) throws IOException {
		broadcastToAll(type, msg, isHome, sId, null);
	}

	public void broadcastToAll(String type, String msg, int isHome, Long sId, java.util.List<String> audioUrl) throws IOException {
		Map<Long, WebSocketSession> clients = getClients(sId); // 현재 방의 세젼들

		for (Long clientId : clients.keySet()) {
			WebSocketSession clientSession = clients.get(clientId);
			if (clientSession == null || !clientSession.isOpen()) continue;

			UsersDto receiver = userSettting(clientSession);

			ChatDto notice = new ChatDto();
			notice.setType(type);
			notice.setMessage(msg);
			notice.setUserTeam(receiver.getTeam());
			notice.setIsHome(isHome);
			if (audioUrl != null && !audioUrl.isEmpty()) {
				notice.setAudioUrl(audioUrl);  // audioUrl 설정
			}

			clientSession.sendMessage(new TextMessage(notice.toJson()));
		}
	}

	//입장하는 경우에는 입장 메시지를 출력한다.
	public void enterMessage(@NonNull WebSocketSession session) throws IOException {
		
		user = userSettting(session);
		simulationId = getSimulationId(session);
		Map<Long, WebSocketSession> clients = getClients(simulationId);

		//유저 전원에게 메시지를 뿌리기 위한 for문
		for(Long clientId : clients.keySet()) {
			WebSocketSession clientSession = clients.get(clientId);
			
			//처음에 프론트 값 주입을 위해서 다른 값도 같이 보낸다.
			chat.setName(user.getName());
			chat.setId(user.getId());
			chat.setType("notice");
			chat.setUserTeam(user.getTeam());
			chat.setMessage(user.getName() + "님이 입장하셨습니다.");	
			clientSession.sendMessage(new TextMessage(chat.toJson()));
		}
	}

	//퇴장하는 경우에는 퇴장 메시지를 출력한다.
	public void exitMessage(@NonNull WebSocketSession session) throws IOException {
		
		user = userSettting(session);
		simulationId = getSimulationId(session);
		Map<Long, WebSocketSession> clients = getClients(simulationId);
		// 유저 전원에게 메시지를 뿌리기 위한 for문(나간 사람 제외) (출력하지않도록한다)
		// for(Long clientId : clients.keySet()) {
		// 	WebSocketSession clientSession = clients.get(clientId);
		// 	chat.setType("notice");
		// 	chat.setMessage(user.getName() + "님이 퇴장하셨습니다.");
		// 	if (clientSession != null && clientSession.isOpen() && clientId != userId) clientSession.sendMessage(new TextMessage(chat.toJson()));
		// }
		clients.remove(userId);
	}

	public void createMessage(@NonNull WebSocketSession session, TextMessage message) throws IOException {
		
		user = userSettting(session);
		simulationId = getSimulationId(session);
		Map<Long, WebSocketSession> clients = getClients(simulationId);
		
		String payload = message.getPayload(); // JSON 문자열
	    ObjectMapper mapper = new ObjectMapper();
	    JsonNode json = mapper.readTree(payload);
		
		// 메시지 타입 확인 (위치 공유 메시지인지 채팅 메시지인지)
		String type = json.has("type") ? json.get("type").asText() : "chat";
		
		// 위치 공유 메시지 처리
		if ("location".equals(type)) {
			handleLocationMessage(json, clients, user);
			return;
		}

		//유저 전원에게 채팅 메시지를 뿌리기 위한 for문
		for(Long clientId : clients.keySet()) {
			WebSocketSession clientSession = clients.get(clientId);
			
		    String content = json.get("content").asText(); // 프론트에서 받아온 메시지 내용
		    String align = json.get("align").asText(); // 유저 응원 진영
			
			chat.setId(user.getId());
			chat.setName(user.getName());
			chat.setUserTeam(user.getTeam());
			chat.setMessage(content);
			chat.setAlign(align);
			
			//메시지가 공백인지 확인
			if(content != "") {
				//응원팀이 홈팀이 아니면 정렬 right => 'send' 타입으로 보내기
				if ("right".equals(align)) {
					chat.setType("send");
					clientSession.sendMessage(new TextMessage(chat.toJson()));
				}
				//홈팀이면 왼쪽정렬 => 'receive' 타입으로 보내기
				else {
					chat.setType("receive");
					clientSession.sendMessage(new TextMessage(chat.toJson()));
				}
			}
		}
	}
	
	// 위치 공유 메시지 처리 메서드
	private void handleLocationMessage(JsonNode json, Map<Long, WebSocketSession> clients, UsersDto user) throws IOException {
		double latitude = json.get("latitude").asDouble();
		double longitude = json.get("longitude").asDouble();
		
		// 위치 정보를 JSON으로 구성
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> locationData = new java.util.HashMap<>();
		locationData.put("type", "location");
		locationData.put("userId", user.getId());
		locationData.put("userName", user.getName());
		locationData.put("profileImage", user.getProfileImage());
		locationData.put("latitude", latitude);
		locationData.put("longitude", longitude);
		locationData.put("timestamp", System.currentTimeMillis());
		
		String locationJson = mapper.writeValueAsString(locationData);
		
		// 같은 방의 모든 클라이언트에게 위치 정보 전송
		for(Long clientId : clients.keySet()) {
			WebSocketSession clientSession = clients.get(clientId);
			if (clientSession != null && clientSession.isOpen()) {
				clientSession.sendMessage(new TextMessage(locationJson));
			}
		}
	}
}