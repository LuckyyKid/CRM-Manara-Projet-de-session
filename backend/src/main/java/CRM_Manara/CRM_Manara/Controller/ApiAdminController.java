package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.dto.ActionResponseDto;
import CRM_Manara.CRM_Manara.dto.AdminAnimationRowDto;
import CRM_Manara.CRM_Manara.dto.AdminDemandesDto;
import CRM_Manara.CRM_Manara.dto.AdminInscriptionReviewDto;
import CRM_Manara.CRM_Manara.dto.AdminNotificationDto;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.service.AdminNotificationService;
import CRM_Manara.CRM_Manara.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;

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
    public List<?> activities() {
        return adminService.getAllActivities().stream()
                .sorted(Comparator.comparing(activity -> activity.getActivyName(), Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(apiDtoMapper::toActivityDto)
                .toList();
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
}
