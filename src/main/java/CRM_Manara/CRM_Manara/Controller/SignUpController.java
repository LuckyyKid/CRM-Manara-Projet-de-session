package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Service.parentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class SignUpController {

    @Autowired
    private parentService parentService;

    @GetMapping("/signUp")
    public String signUp(Model model) {
        model.addAttribute("formData", new LinkedHashMap<String, String>());
        model.addAttribute("errors", new LinkedHashMap<String, String>());
        return "auth/signUp";
    }

    @GetMapping("/api/signUp/email-availability")
    @ResponseBody
    public Map<String, Object> checkEmailAvailability(@RequestParam("email") String email) {
        boolean available = parentService.isEmailAvailable(email);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("available", available);
        response.put("message", available ? "Email disponible." : "Un compte existe deja avec cet email.");
        return response;
    }

    @PostMapping("/signUp")
    public String signUp(@RequestParam(name = "nom") String nom,
                         @RequestParam(name = "prenom") String prenom,
                         @RequestParam(name = "adresse") String adresse,
                         @RequestParam(name = "email") String email,
                         @RequestParam(name = "password") String password,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        Map<String, String> errors = validateSignUpInputs(nom, prenom, adresse, email, password);
        if (!errors.isEmpty()) {
            model.addAttribute("errors", errors);
            model.addAttribute("formData", buildSignUpFormData(nom, prenom, adresse, email));
            return "auth/signUp";
        }
        // ADDED
        System.out.println("STEP 1 REACHED - SignUpController.signUp()");
        System.out.println("Signup request received for email: " + email);
        try {
            // MODIFIED
            // ADDED
            System.out.println("STEP 2 REACHED - Calling parentService.createNewParent()");
            parentService.createNewParent(nom, prenom, adresse, email, password);
            // ADDED
            System.out.println("STEP 3 REACHED - parentService.createNewParent() completed successfully for: " + email);
            redirectAttributes.addFlashAttribute("message", "Inscription reussie. Verifiez votre email avant de vous connecter.");
            return "redirect:/login";
        } catch (IllegalArgumentException exception) {
            // ADDED
            System.out.println("SIGNUP BUSINESS ERROR for email " + email + ": " + exception.getMessage());
            exception.printStackTrace();
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/signUp";
        } catch (Exception exception) {
            // ADDED
            System.out.println("SIGNUP UNEXPECTED ERROR for email " + email + ": " + exception.getMessage());
            exception.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Erreur technique pendant l'inscription.");
            return "redirect:/signUp";
        }
    }

    private Map<String, String> validateSignUpInputs(String nom, String prenom, String adresse, String email, String password) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (nom == null || nom.trim().isEmpty()) {
            errors.put("nom", "Le nom est obligatoire.");
        }
        if (prenom == null || prenom.trim().isEmpty()) {
            errors.put("prenom", "Le prénom est obligatoire.");
        }
        if (adresse == null || adresse.trim().isEmpty()) {
            errors.put("adresse", "L'adresse est obligatoire.");
        }
        if (email == null || email.trim().isEmpty()) {
            errors.put("email", "L'email est obligatoire.");
        } else if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            errors.put("email", "Entrez une adresse email valide.");
        }
        if (password == null || password.isBlank()) {
            errors.put("password", "Le mot de passe est obligatoire.");
        } else if (password.length() < 6) {
            errors.put("password", "Le mot de passe doit contenir au moins 6 caractères.");
        }
        return errors;
    }

    private Map<String, String> buildSignUpFormData(String nom, String prenom, String adresse, String email) {
        Map<String, String> formData = new LinkedHashMap<>();
        formData.put("nom", nom == null ? "" : nom);
        formData.put("prenom", prenom == null ? "" : prenom);
        formData.put("adresse", adresse == null ? "" : adresse);
        formData.put("email", email == null ? "" : email);
        return formData;
    }
}
