package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Service.parentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SignUpController {

    @Autowired
    private parentService parentService;

    @GetMapping("/signUp")
    public String signUp() {
        return "auth/signUp";
    }

    @PostMapping("/signUp")
    public String signUp(@RequestParam(name = "nom") String nom,
                         @RequestParam(name = "prenom") String prenom,
                         @RequestParam(name = "adresse") String adresse,
                         @RequestParam(name = "email") String email,
                         @RequestParam(name = "password") String password,
                         RedirectAttributes redirectAttributes) {
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
}
