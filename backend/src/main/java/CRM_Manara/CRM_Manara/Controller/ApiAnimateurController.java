package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.PresenceStatus;
import CRM_Manara.CRM_Manara.dto.ActionResponseDto;
import CRM_Manara.CRM_Manara.dto.AnimateurNotificationDto;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.dto.InscriptionDto;
import CRM_Manara.CRM_Manara.dto.PresenceUpdateRequestDto;
import CRM_Manara.CRM_Manara.service.AnimateurNotificationService;
import CRM_Manara.CRM_Manara.service.AnimateurService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/animateur")
public class ApiAnimateurController {

    private final AnimateurService animateurService;
    private final AnimateurNotificationService animateurNotificationService;
    private final ApiDtoMapper apiDtoMapper;

    public ApiAnimateurController(AnimateurService animateurService,
                                  AnimateurNotificationService animateurNotificationService,
                                  ApiDtoMapper apiDtoMapper) {
        this.animateurService = animateurService;
        this.animateurNotificationService = animateurNotificationService;
        this.apiDtoMapper = apiDtoMapper;
    }

    @GetMapping("/animations")
    public List<?> animations(Authentication authentication) {
        Animateur animateur = requireAnimateur(authentication);
        return animateurService.getAnimationsForAnimateur(animateur.getId()).stream()
                .map(apiDtoMapper::toAnimationDto)
                .toList();
    }

    @GetMapping("/notifications")
    public List<AnimateurNotificationDto> notifications(Authentication authentication) {
        Animateur animateur = requireAnimateur(authentication);
        return animateurNotificationService.getNotificationsForAnimateur(animateur.getId(), 100).stream()
                .map(apiDtoMapper::toAnimateurNotificationDto)
                .toList();
    }

    @RequestMapping(value = "/notifications/read-all", method = {RequestMethod.POST, RequestMethod.PUT})
    public ActionResponseDto markAllNotificationsAsRead(Authentication authentication) {
        Animateur animateur = requireAnimateur(authentication);
        animateurNotificationService.markAllAsReadForAnimateur(animateur.getId());
        return new ActionResponseDto(true, "Notifications marquees comme lues.", null);
    }

    @RequestMapping(value = "/notifications/{id}/read", method = {RequestMethod.POST, RequestMethod.PUT})
    public ActionResponseDto markNotificationAsRead(@PathVariable Long id, Authentication authentication) {
        Animateur animateur = requireAnimateur(authentication);
        animateurNotificationService.markAsRead(animateur.getId(), id);
        return new ActionResponseDto(true, "Notification marquee comme lue.", id);
    }

    @GetMapping("/animations/{id}/inscriptions")
    public List<InscriptionDto> animationInscriptions(@PathVariable Long id, Authentication authentication) {
        String email = requireEmail(authentication);
        return animateurService.getInscriptionsForAnimation(id, email).stream()
                .sorted(Comparator.comparing(inscription -> inscription.getEnfant().getPrenom(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(apiDtoMapper::toInscriptionDto)
                .toList();
    }

    @GetMapping("/inscriptions")
    public List<InscriptionDto> inscriptions(@RequestParam(required = false) Long animationId,
                                             @RequestParam(required = false, defaultValue = "") String search,
                                             Authentication authentication) {
        Animateur animateur = requireAnimateur(authentication);
        String normalizedSearch = normalize(search);
        return animateurService.getInscriptionsForAnimateur(animateur.getId()).stream()
                .filter(inscription -> animationId == null
                        || inscription.getAnimation().getId().equals(animationId))
                .filter(inscription -> normalizedSearch.isBlank()
                        || normalize(inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom()).contains(normalizedSearch)
                        || (inscription.getEnfant().getParent() != null
                        && normalize(inscription.getEnfant().getParent().getPrenom() + " " + inscription.getEnfant().getParent().getNom()).contains(normalizedSearch)))
                .sorted(Comparator.comparing(inscription -> inscription.getEnfant().getPrenom(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(apiDtoMapper::toInscriptionDto)
                .toList();
    }

    @PostMapping("/inscriptions/{id}/presence")
    public ActionResponseDto updatePresence(@PathVariable Long id,
                                            @RequestBody PresenceUpdateRequestDto request,
                                            Authentication authentication) {
        String email = requireEmail(authentication);
        if (request == null || request.presenceStatus() == null || request.presenceStatus().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le statut de présence est requis");
        }
        try {
            PresenceStatus presenceStatus = PresenceStatus.valueOf(request.presenceStatus().trim().toUpperCase());
            animateurService.updatePresence(id, email, presenceStatus, request.incidentNote());
            return new ActionResponseDto(true, "Présence mise à jour.", id);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private Animateur requireAnimateur(Authentication authentication) {
        return animateurService.getAnimateurByEmail(requireEmail(authentication));
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        return authentication.getName();
    }

    private String normalize(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
    }
}
