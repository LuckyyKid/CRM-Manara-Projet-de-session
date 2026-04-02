package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.service.parentService;
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
            redirectAttributes.addFlashAttribute("message", "Votre demande a ete prise en compte. Un administrateur doit encore approuver votre compte avant la connexion.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
        }

        return "redirect:/login";
    }
}
