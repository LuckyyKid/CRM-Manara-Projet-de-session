package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.dto.CurrentUserDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserService {

    private final UserRepo userRepo;
    private final ParentRepo parentRepo;
    private final AnimateurRepo animateurRepo;
    private final AdminRepo adminRepo;
    private final AnimationRepo animationRepo;
    private final InscriptionRepo inscriptionRepo;
    private final ApiDtoMapper apiDtoMapper;

    public CurrentUserService(UserRepo userRepo,
                              ParentRepo parentRepo,
                              AnimateurRepo animateurRepo,
                              AdminRepo adminRepo,
                              AnimationRepo animationRepo,
                              InscriptionRepo inscriptionRepo,
                              ApiDtoMapper apiDtoMapper) {
        this.userRepo = userRepo;
        this.parentRepo = parentRepo;
        this.animateurRepo = animateurRepo;
        this.adminRepo = adminRepo;
        this.animationRepo = animationRepo;
        this.inscriptionRepo = inscriptionRepo;
        this.apiDtoMapper = apiDtoMapper;
    }

    @Transactional(readOnly = true)
    public CurrentUserDto currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }

        User user = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));

        Parent parent = parentRepo.findByUser(user).orElse(null);
        Animateur animateur = animateurRepo.findByUser(user).orElse(null);
        Administrateurs admin = adminRepo.findByUser(user).orElse(null);

        String accountType = user.getRole() == null ? null : user.getRole().name();
        boolean canAccessTutoringTools = hasTutoringToolsAccess(parent, animateur, admin);
        boolean canAccessSportPracticeTools = hasSportPracticeToolsAccess(parent, animateur, admin);

        return new CurrentUserDto(
                user.getId(),
                accountType,
                canAccessTutoringTools,
                canAccessSportPracticeTools,
                apiDtoMapper.toUserDto(user),
                apiDtoMapper.toParentDto(parent),
                apiDtoMapper.toAnimateurDto(animateur),
                apiDtoMapper.toAdminDto(admin)
        );
    }

    private boolean hasTutoringToolsAccess(Parent parent, Animateur animateur, Administrateurs admin) {
        if (admin != null) {
            return true;
        }
        if (animateur != null) {
            return animationRepo.findByAnimateurId(animateur.getId()).stream()
                    .anyMatch(animation -> animation.getActivity() != null && animation.getActivity().getType() == typeActivity.TUTORAT);
        }
        if (parent != null) {
            return inscriptionRepo.findByParentId(parent.getId()).stream()
                    .anyMatch(inscription -> inscription.getAnimation() != null
                            && inscription.getAnimation().getActivity() != null
                            && inscription.getAnimation().getActivity().getType() == typeActivity.TUTORAT);
        }
        return false;
    }

    private boolean hasSportPracticeToolsAccess(Parent parent, Animateur animateur, Administrateurs admin) {
        if (admin != null) {
            return true;
        }
        if (animateur != null) {
            return animationRepo.findByAnimateurId(animateur.getId()).stream()
                    .anyMatch(animation -> animation.getActivity() != null && animation.getActivity().getType() == typeActivity.SPORT);
        }
        if (parent != null) {
            return inscriptionRepo.findByParentId(parent.getId()).stream()
                    .anyMatch(inscription -> inscription.getAnimation() != null
                            && inscription.getAnimation().getActivity() != null
                            && inscription.getAnimation().getActivity().getType() == typeActivity.SPORT);
        }
        return false;
    }
}
