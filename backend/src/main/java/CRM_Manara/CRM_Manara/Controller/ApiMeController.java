package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.CurrentUserDto;
import CRM_Manara.CRM_Manara.service.CurrentUserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiMeController {

    private final CurrentUserService currentUserService;

    public ApiMeController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public CurrentUserDto me(Authentication authentication) {
        return currentUserService.currentUser(authentication);
    }
}
