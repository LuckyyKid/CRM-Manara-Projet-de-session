package CRM_Manara.CRM_Manara.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimeWebSocketHandler realtimeWebSocketHandler;

    @Value("${app.frontend.base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Value("${app.cors.allowed-origins:http://localhost:4200,https://crm-manara-projet-de-session.vercel.app,https://*.netlify.app}")
    private String allowedOrigins;

    public WebSocketConfig(RealtimeWebSocketHandler realtimeWebSocketHandler) {
        this.realtimeWebSocketHandler = realtimeWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler, "/ws/realtime")
                .setAllowedOriginPatterns(resolveAllowedOriginPatterns().toArray(String[]::new));
    }

    private List<String> resolveAllowedOriginPatterns() {
        Set<String> patterns = new LinkedHashSet<>();
        addOrigins(patterns, allowedOrigins);
        addOrigins(patterns, frontendBaseUrl);
        return new ArrayList<>(patterns);
    }

    private void addOrigins(Set<String> patterns, String source) {
        if (source == null || source.isBlank()) {
            return;
        }

        for (String value : source.split(",")) {
            String normalized = value.trim();
            if (!normalized.isBlank()) {
                patterns.add(normalized);
            }
        }
    }
}
