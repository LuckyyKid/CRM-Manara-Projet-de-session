package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller REST pour le chatbot
 * Gère les requêtes du widget de chat
 */
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    /**
     * Endpoint pour envoyer un message au chatbot
     * POST /api/chatbot/message
     */
    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> envoyerMessage(@RequestBody Map<String, String> request) {
        String messageUtilisateur = request.get("message");
        
        // Trouve la réponse appropriée avec suggestions
        ChatbotService.ReponseChat reponse = chatbotService.trouverReponse(messageUtilisateur);
        
        // Prépare la réponse JSON
        Map<String, Object> response = new HashMap<>();
        response.put("reponse", reponse.message);
        response.put("suggestions", reponse.suggestions);
        response.put("success", true);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour obtenir le message de bienvenue et les suggestions
     * GET /api/chatbot/init
     */
    @GetMapping("/init")
    public ResponseEntity<Map<String, Object>> initialiser() {
        Map<String, Object> response = new HashMap<>();
        response.put("messageBienvenue", chatbotService.getMessageBienvenue());
        response.put("suggestions", chatbotService.getSuggestions());
        response.put("success", true);
        
        return ResponseEntity.ok(response);
    }
}
