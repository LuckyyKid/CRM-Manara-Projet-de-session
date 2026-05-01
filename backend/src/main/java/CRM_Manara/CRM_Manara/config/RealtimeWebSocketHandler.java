package CRM_Manara.CRM_Manara.config;

import CRM_Manara.CRM_Manara.service.JwtService;
import CRM_Manara.CRM_Manara.service.RealtimeService;
import CRM_Manara.CRM_Manara.service.userService;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeService realtimeService;
    private final JwtService jwtService;
    private final userService userService;
    private final ConcurrentHashMap<String, String> sessionEmails = new ConcurrentHashMap<>();

    public RealtimeWebSocketHandler(RealtimeService realtimeService,
                                    JwtService jwtService,
                                    userService userService) {
        this.realtimeService = realtimeService;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String email = resolveEmail(session);
        if (email == null || email.isBlank()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Utilisateur non authentifie"));
            return;
        }
        sessionEmails.put(session.getId(), email);
        realtimeService.registerSession(email, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String email = sessionEmails.remove(session.getId());
        if (email == null) {
            Principal principal = session.getPrincipal();
            email = principal == null ? null : principal.getName();
        }
        if (email != null && !email.isBlank()) {
            realtimeService.unregisterSession(email, session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Canal sortant uniquement. Les actions passent par l'API REST.
    }

    private String resolveEmail(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        if (principal != null && principal.getName() != null) {
            return principal.getName();
        }

        String token = extractToken(session);
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            String username = jwtService.extractUsername(token);
            UserDetails userDetails = userService.loadUserByUsername(username);
            if (jwtService.isTokenValid(token, userDetails)) {
                return username;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String extractToken(WebSocketSession session) {
        if (session.getUri() == null || session.getUri().getRawQuery() == null) {
            return null;
        }

        for (String parameter : session.getUri().getRawQuery().split("&")) {
            int separatorIndex = parameter.indexOf('=');
            if (separatorIndex <= 0) {
                continue;
            }
            String name = URLDecoder.decode(parameter.substring(0, separatorIndex), StandardCharsets.UTF_8);
            if (!"token".equals(name)) {
                continue;
            }
            return URLDecoder.decode(parameter.substring(separatorIndex + 1), StandardCharsets.UTF_8);
        }
        return null;
    }
}
