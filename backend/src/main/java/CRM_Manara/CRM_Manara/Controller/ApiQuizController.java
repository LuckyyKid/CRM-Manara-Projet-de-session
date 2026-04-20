package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.QuizCreateRequestDto;
import CRM_Manara.CRM_Manara.dto.QuizDto;
import CRM_Manara.CRM_Manara.dto.TutorDashboardDto;
import CRM_Manara.CRM_Manara.service.QuizService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/animateur/quizzes")
public class ApiQuizController {

    private final QuizService quizService;

    public ApiQuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    @GetMapping
    public List<QuizDto> list(Authentication authentication) {
        return quizService.listForAnimateur(requireEmail(authentication));
    }

    @GetMapping("/dashboard")
    public TutorDashboardDto dashboard(Authentication authentication) {
        return quizService.getTutorDashboard(requireEmail(authentication));
    }

    @GetMapping("/{id}")
    public QuizDto get(@PathVariable Long id, Authentication authentication) {
        return quizService.getForAnimateur(id, requireEmail(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuizDto create(@RequestBody QuizCreateRequestDto request, Authentication authentication) {
        return quizService.createForAnimateur(requireEmail(authentication), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        quizService.deleteForAnimateur(id, requireEmail(authentication));
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }
        return authentication.getName();
    }
}
