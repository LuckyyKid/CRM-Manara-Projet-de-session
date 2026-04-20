package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.ActionResponseDto;
import CRM_Manara.CRM_Manara.service.parentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/signUp")
public class ApiSignUpController {

    private final parentService parentService;

    public ApiSignUpController(parentService parentService) {
        this.parentService = parentService;
    }

    @GetMapping("/email-availability")
    public Map<String, Object> checkEmailAvailability(@RequestParam("email") String email) {
        boolean available = parentService.isEmailAvailable(email);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("available", available);
        response.put("message", available ? "Email disponible." : "Un compte existe deja avec cet email.");
        return response;
    }

    @PostMapping
    public ActionResponseDto signUp(@RequestBody SignUpRequest request) {
        validate(request);
        try {
            parentService.createNewParent(
                    request.nom().trim(),
                    request.prenom().trim(),
                    request.adresse().trim(),
                    request.email().trim(),
                    request.password()
            );
            return new ActionResponseDto(true, "Inscription reussie. Votre compte est en attente d'approbation.", null);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private void validate(SignUpRequest request) {
        if (request == null
                || request.nom() == null || request.nom().isBlank()
                || request.prenom() == null || request.prenom().isBlank()
                || request.adresse() == null || request.adresse().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tous les champs sont obligatoires.");
        }
        if (!request.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entrez une adresse email valide.");
        }
        if (request.password().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins 6 caracteres.");
        }
    }

    public record SignUpRequest(String nom, String prenom, String adresse, String email, String password) {
    }
}
