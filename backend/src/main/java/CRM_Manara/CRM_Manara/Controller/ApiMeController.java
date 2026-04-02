package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.dto.CurrentUserDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiMeController {

    private final UserRepo userRepo;
    private final ParentRepo parentRepo;
    private final AnimateurRepo animateurRepo;
    private final AdminRepo adminRepo;
    private final ApiDtoMapper apiDtoMapper;

    public ApiMeController(UserRepo userRepo,
                           ParentRepo parentRepo,
                           AnimateurRepo animateurRepo,
                           AdminRepo adminRepo,
                           ApiDtoMapper apiDtoMapper) {
        this.userRepo = userRepo;
        this.parentRepo = parentRepo;
        this.animateurRepo = animateurRepo;
        this.adminRepo = adminRepo;
        this.apiDtoMapper = apiDtoMapper;
    }

    @GetMapping("/me")
    public CurrentUserDto me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }

        User user = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        Parent parent = parentRepo.findByUser(user).orElse(null);
        Animateur animateur = animateurRepo.findByUser(user).orElse(null);
        Administrateurs admin = adminRepo.findByUser(user).orElse(null);

        String accountType = user.getRole() == null ? null : user.getRole().name();

        return new CurrentUserDto(
                user.getId(),
                accountType,
                apiDtoMapper.toUserDto(user),
                apiDtoMapper.toParentDto(parent),
                apiDtoMapper.toAnimateurDto(animateur),
                apiDtoMapper.toAdminDto(admin)
        );
    }
}
