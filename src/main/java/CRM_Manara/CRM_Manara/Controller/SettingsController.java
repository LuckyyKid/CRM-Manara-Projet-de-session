package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.service.AccountSettingsService;
import CRM_Manara.CRM_Manara.dto.AccountSettingsView;
import CRM_Manara.CRM_Manara.service.AvatarService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping
public class SettingsController {
    private static final Path AVATAR_STORAGE = AvatarService.avatarStoragePath();

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

    @GetMapping("/avatars/{filename:.+}")
    public ResponseEntity<Resource> avatar(@org.springframework.web.bind.annotation.PathVariable("filename") String filename) throws MalformedURLException {
        Path file = AVATAR_STORAGE.resolve(filename).normalize();
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        String contentType = MediaType.IMAGE_PNG_VALUE;
        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        } else if (lower.endsWith(".gif")) {
            contentType = MediaType.IMAGE_GIF_VALUE;
        } else if (lower.endsWith(".webp")) {
            contentType = "image/webp";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
