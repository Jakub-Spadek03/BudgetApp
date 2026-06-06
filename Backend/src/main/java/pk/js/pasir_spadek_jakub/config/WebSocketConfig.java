package pk.js.pasir_spadek_jakub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GroupWebSocketHandler groupWebSocketHandler;

    public WebSocketConfig(GroupWebSocketHandler groupWebSocketHandler) {
        this.groupWebSocketHandler = groupWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(groupWebSocketHandler, "/ws/group-notifications")
                .setAllowedOriginPatterns("*");
    }
}