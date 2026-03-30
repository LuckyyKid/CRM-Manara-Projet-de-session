package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.Service.parentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SignUpController {

    @Autowired
    private parentService parentService;

    @GetMapping("/signUp")
    public String signUp(Model model) {
        model.addAttribute("parent", new Parent());
        return "auth/signUp";
    }

    @PostMapping("/signUp")
    public String signUp(@RequestParam(name = "nom") String nom,
                         @RequestParam(name = "prenom") String prenom,
                         @RequestParam(name = "adresse") String adresse,
                         @RequestParam(name = "email") String email,
                         @RequestParam(name = "password") String password,
                         RedirectAttributes redirectAttributes, @Valid Parent parent, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getAllErrors());
            return "auth/signUp";
        }
        parentService.createNewParent(nom, prenom, adresse, email, password);
        redirectAttributes.addFlashAttribute("message", "Inscription réussie ! Connectez-vous.");
            //Plus tard gérer l'affichage si email existant !

        return "redirect:/login";
    }
}

