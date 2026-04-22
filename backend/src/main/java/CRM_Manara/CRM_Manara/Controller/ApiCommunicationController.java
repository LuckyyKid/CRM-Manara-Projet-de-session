package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.ChatConversationDetailDto;
import CRM_Manara.CRM_Manara.dto.ChatConversationSummaryDto;
import CRM_Manara.CRM_Manara.dto.ChatMessageDto;
import CRM_Manara.CRM_Manara.dto.ChatParticipantDto;
import CRM_Manara.CRM_Manara.dto.AppointmentSlotCreateDto;
import CRM_Manara.CRM_Manara.dto.AppointmentSlotDto;
import CRM_Manara.CRM_Manara.dto.BookingDto;
import CRM_Manara.CRM_Manara.dto.BookingRequestDto;
import CRM_Manara.CRM_Manara.dto.SendChatMessageRequestDto;
import CRM_Manara.CRM_Manara.dto.SidebarCountsDto;
import CRM_Manara.CRM_Manara.service.AppointmentSlotService;
import CRM_Manara.CRM_Manara.service.ChatService;
import CRM_Manara.CRM_Manara.service.SidebarCountsService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/communication")
public class ApiCommunicationController {

    private final ChatService chatService;
    private final SidebarCountsService sidebarCountsService;
    private final AppointmentSlotService appointmentSlotService;

    public ApiCommunicationController(ChatService chatService,
                                      SidebarCountsService sidebarCountsService,
                                      AppointmentSlotService appointmentSlotService) {
        this.chatService = chatService;
        this.sidebarCountsService = sidebarCountsService;
        this.appointmentSlotService = appointmentSlotService;
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

    @GetMapping("/appointments/my-slots")
    public List<AppointmentSlotDto> myAppointmentSlots(Authentication authentication) {
        return appointmentSlotService.listOwnSlots(requireEmail(authentication));
    }

    @PostMapping("/appointments/my-slots")
    public AppointmentSlotDto createAppointmentSlot(@RequestBody AppointmentSlotCreateDto request,
                                                    Authentication authentication) {
        return appointmentSlotService.createOwnSlot(requireEmail(authentication), request);
    }

    @PutMapping("/appointments/my-slots/{slotId}")
    public AppointmentSlotDto updateAppointmentSlot(@PathVariable Long slotId,
                                                    @RequestBody AppointmentSlotCreateDto request,
                                                    Authentication authentication) {
        return appointmentSlotService.updateOwnSlot(requireEmail(authentication), slotId, request);
    }

    @PutMapping("/appointments/my-slots/{slotId}/reschedule")
    public AppointmentSlotDto rescheduleBookedAppointmentSlot(@PathVariable Long slotId,
                                                              @RequestBody AppointmentSlotCreateDto request,
                                                              Authentication authentication) {
        return appointmentSlotService.rescheduleOwnBookedSlot(requireEmail(authentication), slotId, request);
    }

    @DeleteMapping("/appointments/my-slots/{slotId}")
    public void deleteAppointmentSlot(@PathVariable Long slotId, Authentication authentication) {
        appointmentSlotService.deleteOwnSlot(requireEmail(authentication), slotId);
    }

    @GetMapping("/appointments/animateur/{animateurUserId}/slots")
    public List<AppointmentSlotDto> animateurAvailableSlots(@PathVariable Long animateurUserId,
                                                            Authentication authentication) {
        return appointmentSlotService.listAvailableSlotsForAnimateur(requireEmail(authentication), animateurUserId);
    }

    @PostMapping("/appointments/slots/{slotId}/reserve")
    public AppointmentSlotDto reserveAppointmentSlot(@PathVariable Long slotId, Authentication authentication) {
        return appointmentSlotService.reserveSlot(requireEmail(authentication), slotId);
    }

    @GetMapping("/availability/{animateurId}")
    public List<AppointmentSlotDto> availability(@PathVariable Long animateurId, Authentication authentication) {
        return appointmentSlotService.listAvailabilityForViewer(requireEmail(authentication), animateurId);
    }

    @PostMapping("/availability")
    public AppointmentSlotDto createAvailability(@RequestBody AppointmentSlotCreateDto request,
                                                 Authentication authentication) {
        return appointmentSlotService.createOwnSlot(requireEmail(authentication), request);
    }

    @PutMapping("/availability/{id}")
    public AppointmentSlotDto updateAvailability(@PathVariable Long id,
                                                 @RequestBody AppointmentSlotCreateDto request,
                                                 Authentication authentication) {
        return appointmentSlotService.updateOwnSlot(requireEmail(authentication), id, request);
    }

    @DeleteMapping("/availability/{id}")
    public void deleteAvailability(@PathVariable Long id, Authentication authentication) {
        appointmentSlotService.deleteOwnSlot(requireEmail(authentication), id);
    }

    @PostMapping("/booking")
    public AppointmentSlotDto createBooking(@RequestBody BookingRequestDto request, Authentication authentication) {
        return appointmentSlotService.reserveSlot(requireEmail(authentication), request);
    }

    @GetMapping("/booking/animateur/{id}")
    public List<BookingDto> animateurBookings(@PathVariable Long id, Authentication authentication) {
        return appointmentSlotService.listBookingsForAnimateur(requireEmail(authentication), id);
    }

    @GetMapping("/booking/parent/{id}")
    public List<BookingDto> parentBookings(@PathVariable Long id, Authentication authentication) {
        return appointmentSlotService.listBookingsForParent(requireEmail(authentication), id);
    }

    @DeleteMapping("/booking/{id}")
    public BookingDto cancelBooking(@PathVariable Long id, Authentication authentication) {
        return appointmentSlotService.cancelBooking(requireEmail(authentication), id);
    }

    @PostMapping("/booking/{id}/reschedule")
    public BookingDto rescheduleBooking(@PathVariable Long id,
                                        @RequestBody BookingRequestDto request,
                                        Authentication authentication) {
        return appointmentSlotService.rescheduleBooking(requireEmail(authentication), id, request);
    }

    @PutMapping("/booking/reschedule/{id}")
    public BookingDto rescheduleBookingPut(@PathVariable Long id,
                                           @RequestBody BookingRequestDto request,
                                           Authentication authentication) {
        return appointmentSlotService.rescheduleBooking(requireEmail(authentication), id, request);
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }
        return authentication.getName();
    }
}
