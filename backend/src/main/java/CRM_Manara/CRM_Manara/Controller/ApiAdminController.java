package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Quiz;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AnimationRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Repository.QuizRepo;
import CRM_Manara.CRM_Manara.dto.ActionResponseDto;
import CRM_Manara.CRM_Manara.dto.ActivityDto;
import CRM_Manara.CRM_Manara.dto.ActivityRequestDto;
import CRM_Manara.CRM_Manara.dto.AdminAnimationRowDto;
import CRM_Manara.CRM_Manara.dto.AdminDemandesDto;
import CRM_Manara.CRM_Manara.dto.AdminInscriptionReviewDto;
import CRM_Manara.CRM_Manara.dto.AdminNotificationDto;
import CRM_Manara.CRM_Manara.dto.AdminOptionsDto;
import CRM_Manara.CRM_Manara.dto.AnimateurDto;
import CRM_Manara.CRM_Manara.dto.AnimateurRequestDto;
import CRM_Manara.CRM_Manara.dto.AnimationDto;
import CRM_Manara.CRM_Manara.dto.AnimationRequestDto;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.dto.EnfantDto;
import CRM_Manara.CRM_Manara.dto.ParentDto;
import CRM_Manara.CRM_Manara.dto.QuizAxisDto;
import CRM_Manara.CRM_Manara.dto.QuizDto;
import CRM_Manara.CRM_Manara.dto.QuizQuestionDto;
import CRM_Manara.CRM_Manara.service.AdminNotificationService;
import CRM_Manara.CRM_Manara.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/admin")
public class ApiAdminController {

    private final AdminService adminService;
    private final AdminNotificationService adminNotificationService;
    private final ApiDtoMapper apiDtoMapper;
    private final QuizRepo quizRepo;

    public ApiAdminController(AdminService adminService,
                              AdminNotificationService adminNotificationService,
                              ApiDtoMapper apiDtoMapper,
                              QuizRepo quizRepo) {
        this.adminService = adminService;
        this.adminNotificationService = adminNotificationService;
        this.apiDtoMapper = apiDtoMapper;
        this.quizRepo = quizRepo;
    }

    @GetMapping("/activities")
    @Transactional(readOnly = true)
    public List<ActivityDto> activities() {
        return adminService.getAllActivities().stream()
                .sorted(Comparator.comparing(activity -> activity.getActivyName(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(apiDtoMapper::toActivityDto)
                .toList();
    }

    @GetMapping("/activities/{id}")
    @Transactional(readOnly = true)
    public ActivityDto activity(@PathVariable Long id) {
        return apiDtoMapper.toActivityDto(adminService.getActivityById(id));
    }

    @PostMapping("/activities")
    public ActivityDto createActivity(@RequestBody ActivityRequestDto request) {
        validateActivityRequest(request);
        return apiDtoMapper.toActivityDto(adminService.createActivity(
                request.name().trim(),
                request.description().trim(),
                normalizeImageUrl(request.imageUrl()),
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
                normalizeImageUrl(request.imageUrl()),
                request.ageMin(),
                request.ageMax(),
                request.capacity(),
                status.valueOf(request.status()),
                typeActivity.valueOf(request.type())
        ));
    }

    @DeleteMapping("/activities/{id}")
    public ActionResponseDto deleteActivity(@PathVariable Long id) {
        adminService.deleteActivity(id);
        return new ActionResponseDto(true, "Activite supprimee.", id);
    }

    @GetMapping("/animations")
    @Transactional(readOnly = true)
    public List<AdminAnimationRowDto> animations() {
        List<CRM_Manara.CRM_Manara.Model.Entity.Animation> animations = adminService.getAllAnimations().stream()
                .sorted(Comparator.comparing(animation -> animation.getStartTime(), Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Map<Long, Map<String, Object>> capacitySnapshots = adminService.getAnimationCapacitySnapshotsForAnimations(animations);
        return animations.stream()
                .map(animation -> new AdminAnimationRowDto(
                        apiDtoMapper.toAnimationDto(animation),
                        apiDtoMapper.toAnimationCapacityDto(animation.getId(), capacitySnapshots.get(animation.getId()))
                ))
                .toList();
    }

    @GetMapping("/animations/{id}")
    @Transactional(readOnly = true)
    public AnimationDto animation(@PathVariable Long id) {
        return apiDtoMapper.toAnimationDto(adminService.getAnimationById(id));
    }

    @GetMapping("/animations/{id}/quizzes")
    @Transactional(readOnly = true)
    public List<QuizDto> animationQuizzes(@PathVariable Long id) {
        adminService.getAnimationById(id);
        return quizRepo.findByAnimation_IdOrderByCreatedAtDesc(id).stream()
                .map(this::toQuizDto)
                .toList();
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

    @DeleteMapping("/animations/{id}")
    public ActionResponseDto deleteAnimation(@PathVariable Long id) {
        adminService.deleteAnimation(id);
        return new ActionResponseDto(true, "Animation supprimee.", id);
    }

    @GetMapping("/animateurs")
    @Transactional(readOnly = true)
    public List<AnimateurDto> animateurs() {
        return adminService.getAllAnimateurs().stream()
                .sorted(Comparator.comparing(animateur -> animateur.getNom(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(apiDtoMapper::toAnimateurDto)
                .toList();
    }

    @GetMapping("/animateurs/{id}")
    @Transactional(readOnly = true)
    public AnimateurDto animateur(@PathVariable Long id) {
        return apiDtoMapper.toAnimateurDto(adminService.getAnimateurById(id));
    }

    @GetMapping("/parents")
    @Transactional(readOnly = true)
    public List<ParentDto> parents() {
        return adminService.getAllParents().stream()
                .map(apiDtoMapper::toParentDto)
                .toList();
    }

    @GetMapping("/enfants")
    @Transactional(readOnly = true)
    public List<EnfantDto> enfants() {
        return adminService.getAllEnfants().stream()
                .map(apiDtoMapper::toEnfantDto)
                .toList();
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

    @DeleteMapping("/animateurs/{id}")
    public ActionResponseDto deleteAnimateur(@PathVariable Long id) {
        adminService.deleteAnimateur(id);
        return new ActionResponseDto(true, "Animateur supprime.", id);
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
    @Transactional(readOnly = true)
    public AdminDemandesDto demandes() {
        List<Inscription> pendingInscriptions = adminService.getPendingInscriptions();
        List<Inscription> processedInscriptions = adminService.getProcessedInscriptions();
        Set<Long> animationIds = java.util.stream.Stream.concat(
                        pendingInscriptions.stream(),
                        processedInscriptions.stream()
                )
                .map(inscription -> inscription.getAnimation() == null ? null : inscription.getAnimation().getId())
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Map<Long, Map<String, Object>> capacitySnapshots = adminService.getAnimationCapacitySnapshotsForAnimationIds(animationIds);
        List<AdminInscriptionReviewDto> pending = pendingInscriptions.stream()
                .map(inscription -> toReviewDto(inscription, capacitySnapshots))
                .toList();
        List<AdminInscriptionReviewDto> processed = processedInscriptions.stream()
                .map(inscription -> toReviewDto(inscription, capacitySnapshots))
                .toList();

        return new AdminDemandesDto(
                adminService.getPendingParents().stream().map(apiDtoMapper::toParentDto).toList(),
                adminService.getPendingEnfants().stream().map(apiDtoMapper::toEnfantDto).toList(),
                pending,
                processed
        );
    }

    @GetMapping("/inscriptions")
    @Transactional(readOnly = true)
    public List<AdminInscriptionReviewDto> inscriptions(@RequestParam(required = false) Long animateurId,
                                                        @RequestParam(required = false) Long activityId,
                                                        @RequestParam(required = false) Long animationId,
                                                        @RequestParam(required = false) Long parentId,
                                                        @RequestParam(required = false) Long enfantId,
                                                        @RequestParam(required = false, defaultValue = "") String status,
                                                        @RequestParam(required = false, defaultValue = "") String search) {
        String normalizedStatus = status == null ? "" : status.trim().toUpperCase();
        String normalizedSearch = normalize(search);
        List<Inscription> matches = adminService.getAllInscriptions().stream()
                .filter(inscription -> animateurId == null
                        || inscription.getAnimation().getAnimateur().getId().equals(animateurId))
                .filter(inscription -> activityId == null
                        || inscription.getAnimation().getActivity().getId().equals(activityId))
                .filter(inscription -> animationId == null
                        || inscription.getAnimation().getId().equals(animationId))
                .filter(inscription -> parentId == null
                        || (inscription.getEnfant().getParent() != null
                        && inscription.getEnfant().getParent().getId().equals(parentId)))
                .filter(inscription -> enfantId == null
                        || inscription.getEnfant().getId().equals(enfantId))
                .filter(inscription -> normalizedStatus.isBlank()
                        || "DEMANDE".equals(normalizedStatus) && "EN_ATTENTE".equals(inscription.getStatusInscription().name())
                        || "APPROUVE".equals(normalizedStatus) && ("APPROUVEE".equals(inscription.getStatusInscription().name()) || "ACTIF".equals(inscription.getStatusInscription().name()))
                        || "REFUSER".equals(normalizedStatus) && "REFUSEE".equals(inscription.getStatusInscription().name()))
                .filter(inscription -> normalizedSearch.isBlank()
                        || normalize(inscription.getEnfant().getPrenom() + " " + inscription.getEnfant().getNom()).contains(normalizedSearch)
                        || inscription.getEnfant().getParent() != null
                        && normalize(inscription.getEnfant().getParent().getPrenom() + " " + inscription.getEnfant().getParent().getNom()).contains(normalizedSearch)
                        || normalize(inscription.getAnimation().getActivity().getActivyName()).contains(normalizedSearch)
                        || normalize(inscription.getAnimation().getAnimateur().getPrenom() + " " + inscription.getAnimation().getAnimateur().getNom()).contains(normalizedSearch))
                .sorted(Comparator.comparing(Inscription::getId).reversed())
                .toList();
        Map<Long, Map<String, Object>> capacitySnapshots = adminService.getAnimationCapacitySnapshotsForAnimationIds(
                matches.stream()
                        .map(inscription -> inscription.getAnimation() == null ? null : inscription.getAnimation().getId())
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toSet())
        );
        return matches.stream()
                .map(inscription -> toReviewDto(inscription, capacitySnapshots))
                .toList();
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
        return new ActionResponseDto(true, enabled ? "Compte parent confirme." : "Compte parent desactive.", id);
    }

    @DeleteMapping("/parents/{id}")
    public ActionResponseDto deleteParent(@PathVariable Long id) {
        adminService.deleteParent(id);
        return new ActionResponseDto(true, "Parent supprime.", id);
    }

    @PostMapping("/enfants/{id}/status")
    public ActionResponseDto updateEnfantStatus(@PathVariable Long id, @RequestParam boolean active) {
        adminService.updateEnfantActive(id, active);
        return new ActionResponseDto(true, active ? "Enfant active." : "Enfant desactive.", id);
    }

    @DeleteMapping("/enfants/{id}")
    public ActionResponseDto deleteEnfant(@PathVariable Long id) {
        adminService.deleteEnfant(id);
        return new ActionResponseDto(true, "Enfant supprime.", id);
    }

    @PostMapping("/animateurs/{id}/status")
    public ActionResponseDto updateAnimateurStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        adminService.updateAnimateurEnabled(id, enabled);
        return new ActionResponseDto(true, enabled ? "Compte animateur active." : "Compte animateur desactive.", id);
    }

    @PostMapping("/inscriptions/{id}/approve")
    public ActionResponseDto approveInscription(@PathVariable Long id) {
        try {
            adminService.approveInscription(id);
            return new ActionResponseDto(true, "Demande approuvee.", id);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @PostMapping("/inscriptions/{id}/reject")
    public ActionResponseDto rejectInscription(@PathVariable Long id) {
        adminService.rejectInscription(id);
        return new ActionResponseDto(true, "Demande refusee.", id);
    }

    private AdminInscriptionReviewDto toReviewDto(Inscription inscription, Map<Long, Map<String, Object>> capacitySnapshots) {
        Long animationId = inscription.getAnimation() == null ? null : inscription.getAnimation().getId();
        return new AdminInscriptionReviewDto(
                apiDtoMapper.toInscriptionDto(inscription),
                apiDtoMapper.toAnimationCapacityDto(
                        animationId,
                        animationId == null ? null : capacitySnapshots.get(animationId)
                )
        );
    }

    private QuizDto toQuizDto(Quiz quiz) {
        List<QuizAxisDto> axes = quiz.getAxes().stream()
                .map(axis -> new QuizAxisDto(
                        axis.getId(),
                        axis.getTitle(),
                        axis.getSummary(),
                        axis.getPosition(),
                        axis.getQuestions().stream()
                                .map(question -> new QuizQuestionDto(
                                        question.getId(),
                                        question.getAngle(),
                                        question.getType(),
                                        question.getQuestionText(),
                                        question.getExpectedAnswer(),
                                        question.getPosition(),
                                        question.getOptions()
                                ))
                                .toList()
                ))
                .toList();
        return new QuizDto(
                quiz.getId(),
                quiz.getTitle(),
                quiz.getSourceNotes(),
                quiz.getCreatedAt(),
                quiz.getAnimation() == null ? null : quiz.getAnimation().getId(),
                quiz.getAnimation() == null || quiz.getAnimation().getActivity() == null
                        ? null
                        : quiz.getAnimation().getActivity().getActivyName(),
                axes
        );
    }

    private String normalize(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .trim();
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
        if (request.imageUrl() != null && !request.imageUrl().isBlank()) {
            String imageUrl = request.imageUrl().trim();
            if (!imageUrl.startsWith("data:image/") && !imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le format de l'image de l'activite est invalide.");
            }
        }
    }

    private String normalizeImageUrl(String imageUrl) {
        return imageUrl == null || imageUrl.isBlank() ? null : imageUrl.trim();
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
