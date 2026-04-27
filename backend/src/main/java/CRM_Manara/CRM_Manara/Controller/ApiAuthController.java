package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.dto.AuthResponseDto;
import CRM_Manara.CRM_Manara.dto.LoginRequestDto;
import CRM_Manara.CRM_Manara.service.CurrentUserService;
import CRM_Manara.CRM_Manara.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private static final Logger logger = LoggerFactory.getLogger(ApiAuthController.class);

    private final AuthenticationManager authenticationManager;
    private final CurrentUserService currentUserService;
    private final JwtService jwtService;
    private final UserRepo userRepo;

    public ApiAuthController(AuthenticationManager authenticationManager,
                             CurrentUserService currentUserService,
                             JwtService jwtService,
                             UserRepo userRepo) {
        this.authenticationManager = authenticationManager;
        this.currentUserService = currentUserService;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public AuthResponseDto login(@RequestBody LoginRequestDto request) {
        String email = request.email() == null ? "" : request.email().trim();
        String password = request.password() == null ? "" : request.password();
        logger.info("POST /api/auth/login attempt for {}", email);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );
            User user = userRepo.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
            String token = jwtService.generateToken(user);
            logger.info("POST /api/auth/login success for {}", email);
            return new AuthResponseDto(token, currentUserService.currentUser(authentication));
        } catch (DisabledException exception) {
            logger.warn("POST /api/auth/login blocked for {} because account is pending approval", email);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte en attente d'approbation", exception);
        } catch (AuthenticationException exception) {
            logger.warn("POST /api/auth/login failed for {}", email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects", exception);
        }
    }
}
