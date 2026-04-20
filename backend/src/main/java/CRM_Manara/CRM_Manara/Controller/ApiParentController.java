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
import CRM_Manara.CRM_Manara.dto.ParentQuizDto;
import CRM_Manara.CRM_Manara.dto.QuizAttemptDto;
import CRM_Manara.CRM_Manara.dto.QuizAttemptSubmitDto;
import CRM_Manara.CRM_Manara.service.ParentQuizService;
import CRM_Manara.CRM_Manara.service.parentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final ParentQuizService parentQuizService;
    private final ApiDtoMapper apiDtoMapper;

    @Autowired
    public ApiParentController(parentService parentService, ParentQuizService parentQuizService, ApiDtoMapper apiDtoMapper) {
        this.parentService = parentService;
        this.parentQuizService = parentQuizService;
        this.apiDtoMapper = apiDtoMapper;
    }

    ApiParentController(parentService parentService, ApiDtoMapper apiDtoMapper) {
        this(parentService, null, apiDtoMapper);
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

    @GetMapping("/quizzes")
    public List<ParentQuizDto> quizzes(Authentication authentication) {
        return parentQuizService.listAvailableQuizzes(requireEmail(authentication));
    }

    @GetMapping("/quizzes/{id}")
    public ParentQuizDto quiz(@PathVariable Long id, Authentication authentication) {
        return parentQuizService.getAvailableQuiz(id, requireEmail(authentication));
    }

    @GetMapping("/quiz-attempts")
    public List<QuizAttemptDto> quizAttempts(Authentication authentication) {
        return parentQuizService.listAttempts(requireEmail(authentication));
    }

    @PostMapping("/quizzes/{id}/attempts")
    public QuizAttemptDto submitQuiz(@PathVariable Long id,
                                     @RequestBody QuizAttemptSubmitDto request,
                                     Authentication authentication) {
        return parentQuizService.submitAttempt(id, request, requireEmail(authentication));
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

    @GetMapping("/enfants/{id}")
    public Object getEnfant(@PathVariable Long id, Authentication authentication) {
        String email = requireEmail(authentication);
        try {
            Enfant enfant = parentService.getEnfantForParent(id, email);
            return apiDtoMapper.toEnfantDto(enfant);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        }
    }

    @PostMapping("/enfants")
    public ActionResponseDto createEnfant(@RequestBody Map<String, String> body,
                                          Authentication authentication) {
        String email = requireEmail(authentication);
        String nom = body.get("nom");
        String prenom = body.get("prenom");
        String dateStr = body.get("dateDeNaissance");
        if (nom == null || nom.isBlank() || prenom == null || prenom.isBlank() || dateStr == null || dateStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom, prénom et date de naissance sont requis.");
        }
        try {
            java.sql.Date dateNaissance = java.sql.Date.valueOf(dateStr);
            Enfant enfant = parentService.createEnfantForParent(email, nom.trim(), prenom.trim(), dateNaissance);
            return new ActionResponseDto(true, "Enfant ajouté. Il est en attente d'approbation.", enfant.getId());
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format de date invalide (attendu : yyyy-MM-dd).");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg, e);
        }
    }

    @PutMapping("/enfants/{id}")
    public ActionResponseDto updateEnfant(@PathVariable Long id,
                                          @RequestBody Map<String, String> body,
                                          Authentication authentication) {
        String email = requireEmail(authentication);
        String nom = body.get("nom");
        String prenom = body.get("prenom");
        String dateStr = body.get("dateDeNaissance");
        if (nom == null || nom.isBlank() || prenom == null || prenom.isBlank() || dateStr == null || dateStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nom, prénom et date de naissance sont requis.");
        }
        try {
            java.sql.Date dateNaissance = java.sql.Date.valueOf(dateStr);
            parentService.updateEnfantForParent(id, email, nom.trim(), prenom.trim(), dateNaissance);
            return new ActionResponseDto(true, "Profil enfant mis à jour.", id);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Format de date invalide (attendu : yyyy-MM-dd).");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg, e);
        }
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }
        return authentication.getName();
    }
}
