package CRM_Manara.CRM_Manara.config;

import CRM_Manara.CRM_Manara.service.RealtimeService;
import java.security.Principal;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeService realtimeService;

    public RealtimeWebSocketHandler(RealtimeService realtimeService) {
        this.realtimeService = realtimeService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Principal principal = session.getPrincipal();
        if (principal == null || principal.getName() == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Utilisateur non authentifie"));
            return;
        }
        realtimeService.registerSession(principal.getName(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Principal principal = session.getPrincipal();
        if (principal != null && principal.getName() != null) {
            realtimeService.unregisterSession(principal.getName(), session);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Canal sortant uniquement. Les actions passent par l'API REST.
    }
}
