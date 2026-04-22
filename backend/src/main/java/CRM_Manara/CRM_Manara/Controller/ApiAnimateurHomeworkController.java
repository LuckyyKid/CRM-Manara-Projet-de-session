package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.AnimateurHomeworkOverviewDto;
import CRM_Manara.CRM_Manara.dto.AnimateurHomeworkStudentDetailDto;
import CRM_Manara.CRM_Manara.dto.HomeworkAttemptDto;
import CRM_Manara.CRM_Manara.dto.HomeworkDto;
import CRM_Manara.CRM_Manara.service.AnimateurService;
import CRM_Manara.CRM_Manara.service.HomeworkService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/animateur/homeworks")
public class ApiAnimateurHomeworkController {

    private final HomeworkService homeworkService;
    private final AnimateurService animateurService;

    public ApiAnimateurHomeworkController(HomeworkService homeworkService, AnimateurService animateurService) {
        this.homeworkService = homeworkService;
        this.animateurService = animateurService;
    }

    @GetMapping
    public AnimateurHomeworkOverviewDto overview(Authentication authentication) {
        return homeworkService.getOverviewForAnimateur(requireEmail(authentication), animateurService);
    }

    @GetMapping("/students/{enfantId}")
    public AnimateurHomeworkStudentDetailDto studentDetail(@PathVariable Long enfantId, Authentication authentication) {
        return homeworkService.getStudentDetailForAnimateur(enfantId, requireEmail(authentication), animateurService);
    }

    @GetMapping("/{assignmentId}")
    public HomeworkDto assignment(@PathVariable Long assignmentId, Authentication authentication) {
        return homeworkService.getAssignmentForAnimateur(assignmentId, requireEmail(authentication), animateurService);
    }

    @GetMapping("/{assignmentId}/latest-attempt")
    public HomeworkAttemptDto latestAttempt(@PathVariable Long assignmentId, Authentication authentication) {
        return homeworkService.getLatestAttemptForAnimateurAssignment(assignmentId, requireEmail(authentication), animateurService);
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }
        return authentication.getName();
    }
}
