package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.ChatConversationDetailDto;
import CRM_Manara.CRM_Manara.dto.ChatConversationSummaryDto;
import CRM_Manara.CRM_Manara.dto.ChatMessageDto;
import CRM_Manara.CRM_Manara.dto.ChatParticipantDto;
import CRM_Manara.CRM_Manara.dto.SendChatMessageRequestDto;
import CRM_Manara.CRM_Manara.dto.SidebarCountsDto;
import CRM_Manara.CRM_Manara.service.ChatService;
import CRM_Manara.CRM_Manara.service.SidebarCountsService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/communication")
public class ApiCommunicationController {

    private final ChatService chatService;
    private final SidebarCountsService sidebarCountsService;

    public ApiCommunicationController(ChatService chatService, SidebarCountsService sidebarCountsService) {
        this.chatService = chatService;
        this.sidebarCountsService = sidebarCountsService;
    }

    @GetMapping("/contacts")
    public List<ChatParticipantDto> contacts(Authentication authentication) {
        try {
            return chatService.listAvailableContacts(requireEmail(authentication));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/conversations")
    public List<ChatConversationSummaryDto> conversations(Authentication authentication) {
        try {
            return chatService.listConversations(requireEmail(authentication));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/conversations/{id}")
    public ChatConversationDetailDto conversation(@PathVariable Long id, Authentication authentication) {
        try {
            return chatService.getConversation(id, requireEmail(authentication));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PostMapping("/conversations/{id}/read")
    public void markConversationAsRead(@PathVariable Long id, Authentication authentication) {
        try {
            chatService.markConversationAsRead(id, requireEmail(authentication));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PostMapping("/messages")
    public ChatMessageDto sendMessage(@RequestBody SendChatMessageRequestDto request, Authentication authentication) {
        try {
            return chatService.sendMessage(requireEmail(authentication), request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/sidebar-counts")
    public SidebarCountsDto sidebarCounts(Authentication authentication) {
        try {
            return sidebarCountsService.getCountsForEmail(requireEmail(authentication));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }
        return authentication.getName();
    }
}
