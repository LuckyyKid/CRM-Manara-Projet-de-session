package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.service.AccountSettingsService;
import CRM_Manara.CRM_Manara.dto.AccountSettingsView;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@Profile("thymeleaf")
@Controller
@RequestMapping
public class SettingsController {
    private final AccountSettingsService accountSettingsService;

    public SettingsController(AccountSettingsService accountSettingsService) {
        this.accountSettingsService = accountSettingsService;
    }

    @GetMapping("/settings")
    public String settings(Model model, Principal principal) {
        AccountSettingsView settings = accountSettingsService.getSettings(principal.getName());
        model.addAttribute("settings", settings);
        return "shared/settings";
    }

    @PostMapping("/api/settings")
    @ResponseBody
    public Map<String, Object> updateSettings(@RequestParam("nom") String nom,
                                              @RequestParam("prenom") String prenom,
                                              @RequestParam("email") String email,
                                              @RequestParam(value = "adresse", required = false) String adresse,
                                              @RequestParam(value = "newPassword", required = false) String newPassword,
                                              @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
                                              @RequestParam(value = "resetAvatar", defaultValue = "false") boolean resetAvatar,
                                              @RequestParam(value = "avatarFile", required = false) MultipartFile avatarFile,
                                              Principal principal,
                                              HttpServletRequest request,
                                              HttpServletResponse servletResponse) {
        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, String> errors = accountSettingsService.validateForApi(
                principal.getName(),
                nom,
                prenom,
                email,
                adresse,
                newPassword,
                confirmPassword,
                avatarFile
        );

        if (!errors.isEmpty()) {
            payload.put("success", false);
            payload.put("message", "Veuillez corriger les champs invalides.");
            payload.put("errors", errors);
            return payload;
        }

        boolean emailChanged = !principal.getName().equalsIgnoreCase(email.trim());
        accountSettingsService.updateSettings(
                principal.getName(),
                nom,
                prenom,
                email,
                adresse,
                newPassword,
                confirmPassword,
                resetAvatar,
                avatarFile
        );

        payload.put("success", true);
        if (emailChanged) {
            new SecurityContextLogoutHandler().logout(request, servletResponse, SecurityContextHolder.getContext().getAuthentication());
            payload.put("message", "Paramètres enregistrés. Reconnectez-vous avec votre nouveau courriel.");
            payload.put("redirectUrl", "/login");
        } else {
            payload.put("message", "Paramètres enregistrés avec succès.");
            payload.put("redirectUrl", "/settings");
        }
        return payload;
    }

}
