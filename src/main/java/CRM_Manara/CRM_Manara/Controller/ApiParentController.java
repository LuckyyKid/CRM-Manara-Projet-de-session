package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.dto.ActionResponseDto;
import CRM_Manara.CRM_Manara.dto.AnimationCapacityDto;
import CRM_Manara.CRM_Manara.dto.AnimationWithCapacityDto;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.dto.InscriptionDto;
import CRM_Manara.CRM_Manara.dto.InscriptionRequestDto;
import CRM_Manara.CRM_Manara.dto.ParentActivitiesResponseDto;
import CRM_Manara.CRM_Manara.dto.ParentActivityViewDto;
import CRM_Manara.CRM_Manara.dto.ParentNotificationDto;
import CRM_Manara.CRM_Manara.service.parentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/parent")
public class ApiParentController {

    private final parentService parentService;
    private final ApiDtoMapper apiDtoMapper;

    public ApiParentController(parentService parentService, ApiDtoMapper apiDtoMapper) {
        this.parentService = parentService;
        this.apiDtoMapper = apiDtoMapper;
    }

    @GetMapping("/enfants")
    public List<?> enfants(Authentication authentication) {
        String email = requireEmail(authentication);
        return parentService.getEnfantsForParent(email).stream()
                .map(apiDtoMapper::toEnfantDto)
                .toList();
    }

    @GetMapping("/inscriptions")
    public List<InscriptionDto> inscriptions(Authentication authentication) {
        String email = requireEmail(authentication);
        return parentService.getInscriptionsForParent(email).stream()
                .sorted(Comparator.comparing(Inscription::getId).reversed())
                .map(apiDtoMapper::toInscriptionDto)
                .toList();
    }

    @GetMapping("/notifications")
    public List<ParentNotificationDto> notifications(Authentication authentication) {
        String email = requireEmail(authentication);
        return parentService.getNotificationsForParent(email, 100).stream()
                .map(apiDtoMapper::toParentNotificationDto)
                .toList();
    }

    @GetMapping("/activities")
    public ParentActivitiesResponseDto activities(Authentication authentication) {
        String email = requireEmail(authentication);
        List<Activity> activities = parentService.getAllActivities().stream()
                .sorted(Comparator.comparing(Activity::getActivyName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<Enfant> enfants = parentService.getActiveEnfantsForParent(email).stream()
                .sorted(Comparator.comparing(Enfant::getPrenom, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<Inscription> inscriptions = parentService.getInscriptionsForParent(email).stream()
                .sorted(Comparator.comparing(Inscription::getId).reversed())
                .toList();

        Map<Long, List<CRM_Manara.CRM_Manara.dto.EnfantSummaryDto>> childrenByAnimation = inscriptions.stream()
                .filter(inscription -> inscription.getAnimation() != null && inscription.getEnfant() != null)
                .collect(Collectors.groupingBy(
                        inscription -> inscription.getAnimation().getId(),
                        Collectors.mapping(inscription -> apiDtoMapper.toEnfantSummaryDto(inscription.getEnfant()), Collectors.toList())
                ));

        List<ParentActivityViewDto> activityViews = activities.stream()
                .map(activity -> {
                    List<AnimationWithCapacityDto> animationViews = parentService.getAnimationsForActivity(activity.getId()).stream()
                            .sorted(Comparator.comparing(Animation::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(animation -> {
                                AnimationCapacityDto capacityDto = apiDtoMapper.toAnimationCapacityDto(
                                        animation.getId(),
                                        parentService.getAnimationCapacitySnapshot(animation)
                                );
                                return new AnimationWithCapacityDto(
                                        apiDtoMapper.toAnimationDto(animation),
                                        capacityDto,
                                        childrenByAnimation.getOrDefault(animation.getId(), Collections.emptyList())
                                );
                            })
                            .toList();
                    return new ParentActivityViewDto(apiDtoMapper.toActivityDto(activity), animationViews);
                })
                .toList();

        return new ParentActivitiesResponseDto(
                enfants.stream().map(apiDtoMapper::toEnfantSummaryDto).toList(),
                inscriptions.stream().map(apiDtoMapper::toInscriptionDto).toList(),
                activityViews
        );
    }

    @PostMapping("/inscriptions")
    public ActionResponseDto createInscription(@RequestBody InscriptionRequestDto request,
                                               Authentication authentication) {
        String email = requireEmail(authentication);
        if (request == null || request.enfantId() == null || request.animationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les identifiants enfant et animation sont requis");
        }
        try {
            Inscription inscription = parentService.inscrireEnfant(request.enfantId(), request.animationId(), email);
            return new ActionResponseDto(true, "Demande d'inscription envoyée.", inscription.getId());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        return authentication.getName();
    }
}
