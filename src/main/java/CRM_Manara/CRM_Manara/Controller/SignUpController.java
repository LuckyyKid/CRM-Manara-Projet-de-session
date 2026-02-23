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
        return "signUp";
    }

    @PostMapping("/signUp")
    public String handleSignUp(
            @RequestParam(name = "nom") String nom,
            @RequestParam(name = "prenom") String prenom,
            @RequestParam(name = "adresse", required = false, defaultValue = "") String adresse,
            @RequestParam(name = "email") String email,
            @RequestParam(name = "password") String password,
            RedirectAttributes redirectAttributes
    ) {
        parentService.createNewParent(email, password, nom, prenom, adresse);
        redirectAttributes.addFlashAttribute("message", "Inscription réussie ! Connectez-vous.");
        return "redirect:/login";
    }
}
