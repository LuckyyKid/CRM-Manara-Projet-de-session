package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.Service.parentService;
import CRM_Manara.CRM_Manara.Model.Entity.Service.userService;
import CRM_Manara.CRM_Manara.Model.Entity.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SignUpController {
    @Autowired
    parentService parentService;
    userService userService;

    @GetMapping("/signUp")
    public String afficherSignUp(Model model) {
        //ENVOYER L'OBJET USER QUAND ON AFFICHE LE FORMULAIRE
        model.addAttribute("Parent", new Parent());
        return "signUp";
    }

    @PostMapping("/signUp_form")
    public String signUp(Parent parent, @RequestParam String email, @RequestParam String password) {
        parentService.signUp(parent, email, password);
        return "redirect:/login";
    }



    }



