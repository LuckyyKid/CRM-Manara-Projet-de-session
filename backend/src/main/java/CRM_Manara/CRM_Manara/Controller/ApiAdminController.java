package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AnimationRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.dto.ActionResponseDto;
import CRM_Manara.CRM_Manara.dto.ActivityDto;
import CRM_Manara.CRM_Manara.dto.ActivityRequestDto;
import CRM_Manara.CRM_Manara.dto.AdminOptionsDto;
import CRM_Manara.CRM_Manara.dto.AdminAnimationRowDto;
import CRM_Manara.CRM_Manara.dto.AnimateurDto;
import CRM_Manara.CRM_Manara.dto.AnimateurRequestDto;
import CRM_Manara.CRM_Manara.dto.AnimationDto;
import CRM_Manara.CRM_Manara.dto.AnimationRequestDto;
import CRM_Manara.CRM_Manara.dto.AdminDemandesDto;
import CRM_Manara.CRM_Manara.dto.AdminInscriptionReviewDto;
import CRM_Manara.CRM_Manara.dto.AdminNotificationDto;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.service.AdminNotificationService;
import CRM_Manara.CRM_Manara.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

    private final AdminService adminService;
    private final AdminNotificationService adminNotificationService;
    private final ApiDtoMapper apiDtoMapper;

    public ApiAdminController(AdminService adminService,
                              AdminNotificationService adminNotificationService,
                              ApiDtoMapper apiDtoMapper) {
        this.adminService = adminService;
        this.adminNotificationService = adminNotificationService;
        this.apiDtoMapper = apiDtoMapper;
    }

    @GetMapping("/activities")
    public List<ActivityDto> activities() {
        return adminService.getAllActivities().stream()
                .sorted(Comparator.comparing(activity -> activity.getActivyName(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(apiDtoMapper::toActivityDto)
                .toList();
    }

    @GetMapping("/activities/{id}")
    public ActivityDto activity(@PathVariable Long id) {
        return apiDtoMapper.toActivityDto(adminService.getActivityById(id));
    }

    @PostMapping("/activities")
    public ActivityDto createActivity(@RequestBody ActivityRequestDto request) {
        validateActivityRequest(request);
        return apiDtoMapper.toActivityDto(adminService.createActivity(
                request.name().trim(),
                request.description().trim(),
                request.ageMin(),
                request.ageMax(),
                request.capacity(),
                status.valueOf(request.status()),
                typeActivity.valueOf(request.type())
        ));
    }

    @PutMapping("/activities/{id}")
    public ActivityDto updateActivity(@PathVariable Long id, @RequestBody ActivityRequestDto request) {
        validateActivityRequest(request);
        return apiDtoMapper.toActivityDto(adminService.updateActivity(
                id,
                request.name().trim(),
                request.description().trim(),
                request.ageMin(),
                request.ageMax(),
                request.capacity(),
                status.valueOf(request.status()),
                typeActivity.valueOf(request.type())
        ));
    }

    @GetMapping("/animations")
    public List<AdminAnimationRowDto> animations() {
        return adminService.getAllAnimations().stream()
                .sorted(Comparator.comparing(animation -> animation.getStartTime(), Comparator.nullsLast(Comparator.naturalOrder())))
                .map(animation -> new AdminAnimationRowDto(
                        apiDtoMapper.toAnimationDto(animation),
                        apiDtoMapper.toAnimationCapacityDto(animation.getId(), adminService.getAnimationCapacitySnapshot(animation))
                ))
                .toList();
    }

    @GetMapping("/animations/{id}")
    public AnimationDto animation(@PathVariable Long id) {
        return apiDtoMapper.toAnimationDto(adminService.getAnimationById(id));
    }

    @PostMapping("/animations")
    public AnimationDto createAnimation(@RequestBody AnimationRequestDto request) {
        validateAnimationRequest(request);
        return apiDtoMapper.toAnimationDto(adminService.createAnimation(
                request.activityId(),
                request.animateurId(),
                AnimationRole.valueOf(request.role()),
                animationStatus.valueOf(request.status()),
                request.startTime(),
                request.endTime()
        ));
    }

    @PutMapping("/animations/{id}")
    public AnimationDto updateAnimation(@PathVariable Long id, @RequestBody AnimationRequestDto request) {
        validateAnimationRequest(request);
        return apiDtoMapper.toAnimationDto(adminService.updateAnimation(
                id,
                request.activityId(),
                request.animateurId(),
                AnimationRole.valueOf(request.role()),
                animationStatus.valueOf(request.status()),
                request.startTime(),
                request.endTime()
        ));
    }

    @GetMapping("/animateurs")
    public List<AnimateurDto> animateurs() {
        return adminService.getAllAnimateurs().stream()
                .sorted(Comparator.comparing(animateur -> animateur.getNom(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(apiDtoMapper::toAnimateurDto)
                .toList();
    }

    @GetMapping("/animateurs/{id}")
    public AnimateurDto animateur(@PathVariable Long id) {
        return apiDtoMapper.toAnimateurDto(adminService.getAnimateurById(id));
    }

    @PostMapping("/animateurs")
    public AnimateurDto createAnimateur(@RequestBody AnimateurRequestDto request) {
        validateAnimateurRequest(request, true);
        return apiDtoMapper.toAnimateurDto(adminService.createAnimateur(
                request.nom().trim(),
                request.prenom().trim(),
                request.email().trim(),
                request.password()
        ));
    }

    @PutMapping("/animateurs/{id}")
    public AnimateurDto updateAnimateur(@PathVariable Long id, @RequestBody AnimateurRequestDto request) {
        validateAnimateurRequest(request, false);
        return apiDtoMapper.toAnimateurDto(adminService.updateAnimateur(
                id,
                request.nom().trim(),
                request.prenom().trim()
        ));
    }

    @GetMapping("/options")
    public AdminOptionsDto options() {
        return new AdminOptionsDto(
                Arrays.stream(status.values()).map(Enum::name).toList(),
                Arrays.stream(typeActivity.values()).map(Enum::name).toList(),
                Arrays.stream(AnimationRole.values()).map(Enum::name).toList(),
                Arrays.stream(animationStatus.values()).map(Enum::name).toList()
        );
    }

    @GetMapping("/demandes")
    public AdminDemandesDto demandes() {
        List<AdminInscriptionReviewDto> pending = adminService.getPendingInscriptions().stream()
                .map(this::toReviewDto)
                .toList();
        List<AdminInscriptionReviewDto> processed = adminService.getProcessedInscriptions().stream()
                .map(this::toReviewDto)
                .toList();

        return new AdminDemandesDto(
                adminService.getPendingParents().stream().map(apiDtoMapper::toParentDto).toList(),
                adminService.getPendingEnfants().stream().map(apiDtoMapper::toEnfantDto).toList(),
                pending,
                processed
        );
    }

    @GetMapping("/notifications")
    public List<AdminNotificationDto> notifications() {
        return adminNotificationService.getAll().stream()
                .map(apiDtoMapper::toAdminNotificationDto)
                .toList();
    }

    @PostMapping("/parents/{id}/status")
    public ActionResponseDto updateParentStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        adminService.updateParentEnabled(id, enabled);
        return new ActionResponseDto(true, enabled ? "Compte parent confirmé." : "Compte parent désactivé.", id);
    }

    @PostMapping("/enfants/{id}/status")
    public ActionResponseDto updateEnfantStatus(@PathVariable Long id, @RequestParam boolean active) {
        adminService.updateEnfantActive(id, active);
        return new ActionResponseDto(true, active ? "Enfant activé." : "Enfant désactivé.", id);
    }

    @PostMapping("/animateurs/{id}/status")
    public ActionResponseDto updateAnimateurStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        adminService.updateAnimateurEnabled(id, enabled);
        return new ActionResponseDto(true, enabled ? "Compte animateur activé." : "Compte animateur désactivé.", id);
    }

    @PostMapping("/inscriptions/{id}/approve")
    public ActionResponseDto approveInscription(@PathVariable Long id) {
        try {
            adminService.approveInscription(id);
            return new ActionResponseDto(true, "Demande approuvée.", id);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @PostMapping("/inscriptions/{id}/reject")
    public ActionResponseDto rejectInscription(@PathVariable Long id) {
        adminService.rejectInscription(id);
        return new ActionResponseDto(true, "Demande refusée.", id);
    }

    private AdminInscriptionReviewDto toReviewDto(Inscription inscription) {
        return new AdminInscriptionReviewDto(
                apiDtoMapper.toInscriptionDto(inscription),
                apiDtoMapper.toAnimationCapacityDto(
                        inscription.getAnimation() == null ? null : inscription.getAnimation().getId(),
                        inscription.getAnimation() == null ? null : adminService.getAnimationCapacitySnapshot(inscription.getAnimation())
                )
        );
    }

    private void validateActivityRequest(ActivityRequestDto request) {
        if (request == null
                || request.name() == null || request.name().isBlank()
                || request.description() == null || request.description().isBlank()
                || request.status() == null || request.status().isBlank()
                || request.type() == null || request.type().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tous les champs de l'activite sont obligatoires.");
        }
        if (request.ageMin() < 0 || request.ageMax() < 0 || request.ageMin() > request.ageMax()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La plage d'age est invalide.");
        }
        if (request.capacity() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La capacite doit etre d'au moins 1.");
        }
        try {
            status.valueOf(request.status());
            typeActivity.valueOf(request.type());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut ou type d'activite invalide.", exception);
        }
    }

    private void validateAnimationRequest(AnimationRequestDto request) {
        if (request == null
                || request.activityId() == null
                || request.animateurId() == null
                || request.role() == null || request.role().isBlank()
                || request.status() == null || request.status().isBlank()
                || request.startTime() == null
                || request.endTime() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tous les champs de l'animation sont obligatoires.");
        }
        if (!request.endTime().isAfter(request.startTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fin doit etre apres le debut.");
        }
        try {
            AnimationRole.valueOf(request.role());
            animationStatus.valueOf(request.status());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role ou statut d'animation invalide.", exception);
        }
    }

    private void validateAnimateurRequest(AnimateurRequestDto request, boolean includeCredentials) {
        if (request == null
                || request.nom() == null || request.nom().isBlank()
                || request.prenom() == null || request.prenom().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom et le prenom sont obligatoires.");
        }
        if (!includeCredentials) {
            return;
        }
        if (request.email() == null || request.email().isBlank()
                || !request.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entrez une adresse email valide.");
        }
        if (!adminService.isEmailAvailable(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un utilisateur existe deja avec cet email.");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins 6 caracteres.");
        }
    }
}
