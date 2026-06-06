package pk.js.pasir_spadek_jakub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pk.js.pasir_spadek_jakub.security.JwtUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GroupWebSocketHandler extends TextWebSocketHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public GroupWebSocketHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = getTokenFromSession(session);
        if (token != null && jwtUtil.validateToken(token)) {
            String email = jwtUtil.extractUsername(token);
            sessions.put(email, session);
            System.out.println("=== WebSocket połączony: " + email);
        } else {
            System.out.println("=== WebSocket odrzucony - brak tokena");
            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
        System.out.println("=== WebSocket rozłączony: " + session.getId());
    }

    public void sendToUser(String email, Object message) {
        WebSocketSession session = sessions.get(email);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
                System.out.println("=== Wysłano do: " + email);
            } catch (Exception e) {
                System.out.println("=== Błąd wysyłania do: " + email + " - " + e.getMessage());
            }
        } else {
            System.out.println("=== Brak sesji dla: " + email);
        }
    }

    private String getTokenFromSession(WebSocketSession session) {
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query != null && query.startsWith("token=")) {
            return query.substring(6);
        }
        return null;
    }
}