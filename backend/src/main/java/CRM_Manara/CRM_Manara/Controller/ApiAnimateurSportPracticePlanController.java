package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.SportPracticePlanCreateRequestDto;
import CRM_Manara.CRM_Manara.dto.SportPracticePlanDto;
import CRM_Manara.CRM_Manara.service.SportPracticePlanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/animateur/sport-practice-plans")
public class ApiAnimateurSportPracticePlanController {

    private final SportPracticePlanService sportPracticePlanService;

    public ApiAnimateurSportPracticePlanController(SportPracticePlanService sportPracticePlanService) {
        this.sportPracticePlanService = sportPracticePlanService;
    }

    @GetMapping
    public List<SportPracticePlanDto> list(Authentication authentication) {
        return sportPracticePlanService.listForAnimateur(requireEmail(authentication));
    }

    @GetMapping("/{id}")
    public SportPracticePlanDto get(@PathVariable Long id, Authentication authentication) {
        return sportPracticePlanService.getForAnimateur(id, requireEmail(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SportPracticePlanDto create(@RequestBody SportPracticePlanCreateRequestDto request,
                                       Authentication authentication) {
        return sportPracticePlanService.createForAnimateur(requireEmail(authentication), request);
    }

    private String requireEmail(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }
        return authentication.getName();
    }
}
