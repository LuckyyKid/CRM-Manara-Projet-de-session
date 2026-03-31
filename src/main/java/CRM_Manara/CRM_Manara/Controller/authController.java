package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Service.parentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class authController {

    // ADDED
    @Autowired
    private parentService parentService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    // ADDED
    @GetMapping("/verify")
    public String verifyEmail(@RequestParam("token") String token, RedirectAttributes redirectAttributes) {
        try {
            parentService.verifyUser(token);
            redirectAttributes.addFlashAttribute("message", "Votre compte a ete verifie. Vous pouvez vous connecter.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }

        return "redirect:/login";
    }
}
