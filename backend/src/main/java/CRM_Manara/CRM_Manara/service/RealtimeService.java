package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.dto.RealtimeEnvelopeDto;
import CRM_Manara.CRM_Manara.dto.SidebarCountsDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Service
public class RealtimeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RealtimeService.class);

    private final ObjectMapper objectMapper;
    private final SidebarCountsService sidebarCountsService;
    private final ConcurrentHashMap<String, Set<WebSocketSession>> sessionsByEmail = new ConcurrentHashMap<>();

    public RealtimeService(ObjectMapper objectMapper, SidebarCountsService sidebarCountsService) {
        this.objectMapper = objectMapper;
        this.sidebarCountsService = sidebarCountsService;
    }

    public void registerSession(String email, WebSocketSession session) {
        sessionsByEmail.computeIfAbsent(email, ignored -> ConcurrentHashMap.newKeySet()).add(session);
        sendSidebarCounts(email);
    }

    public void unregisterSession(String email, WebSocketSession session) {
        Set<WebSocketSession> sessions = sessionsByEmail.get(email);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            sessionsByEmail.remove(email);
        }
    }

    public void sendSidebarCounts(String email) {
        try {
            SidebarCountsDto counts = sidebarCountsService.getCountsForEmail(email);
            sendToUser(email, "sidebar-counts", counts);
        } catch (IllegalArgumentException exception) {
            LOGGER.debug("Impossible d'envoyer les compteurs sidebar pour {}", email, exception);
        }
    }

    public void sendToUser(String email, String type, Object payload) {
        Set<WebSocketSession> sessions = sessionsByEmail.get(email);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(new RealtimeEnvelopeDto(type, payload));
        } catch (IOException exception) {
            LOGGER.warn("Impossible de serialiser l'evenement temps reel {}", type, exception);
            return;
        }

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                continue;
            }
            try {
                synchronized (session) {
                    session.sendMessage(message);
                }
            } catch (IOException exception) {
                LOGGER.debug("Envoi websocket impossible pour {}", email, exception);
            }
        }
    }
}
