package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.CurrentUserDto;
import CRM_Manara.CRM_Manara.dto.LoginRequestDto;
import CRM_Manara.CRM_Manara.service.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiAuthController {

    private final AuthenticationManager authenticationManager;
    private final CurrentUserService currentUserService;

    public ApiAuthController(AuthenticationManager authenticationManager,
                             CurrentUserService currentUserService) {
        this.authenticationManager = authenticationManager;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/login")
    public CurrentUserDto login(@RequestBody LoginRequestDto request, HttpServletRequest httpRequest) {
        String email = request.email() == null ? "" : request.email().trim();
        String password = request.password() == null ? "" : request.password();

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            httpRequest.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext
            );

            return currentUserService.currentUser(authentication);
        } catch (DisabledException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte en attente d'approbation", exception);
        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants incorrects", exception);
        }
    }
}
