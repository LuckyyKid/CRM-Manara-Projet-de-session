package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.dto.CurrentUserDto;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.service.AvatarService;
import CRM_Manara.CRM_Manara.service.CurrentUserService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiMeController {

    private static final Logger logger = LoggerFactory.getLogger(ApiMeController.class);

    private final CurrentUserService currentUserService;
    private final UserRepo userRepo;
    private final AvatarService avatarService;

    public ApiMeController(CurrentUserService currentUserService,
                           UserRepo userRepo,
                           AvatarService avatarService) {
        this.currentUserService = currentUserService;
        this.userRepo = userRepo;
        this.avatarService = avatarService;
    }

    @GetMapping("/me")
    public CurrentUserDto me(Authentication authentication) {
        logger.info("GET /api/me called with principal={}", authentication == null ? null : authentication.getName());
        return currentUserService.currentUser(authentication);
    }

    @PostMapping("/me/avatar")
    public Map<String, Object> updateAvatar(@RequestParam("avatarFile") MultipartFile avatarFile,
                                            Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifie");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        Map<String, String> errors = validateAvatar(avatarFile);
        if (!errors.isEmpty()) {
            payload.put("success", false);
            payload.put("message", "Veuillez choisir une image valide.");
            payload.put("errors", errors);
            return payload;
        }

        User user = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        avatarService.storeUploadedAvatar(user, avatarFile);

        payload.put("success", true);
        payload.put("message", "Photo de profil mise a jour.");
        payload.put("avatarUrl", user.getAvatarUrl());
        return payload;
    }

    private Map<String, String> validateAvatar(MultipartFile avatarFile) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (avatarFile == null || avatarFile.isEmpty()) {
            errors.put("avatarFile", "Veuillez choisir une image.");
            return errors;
        }

        String contentType = avatarFile.getContentType();
        if (!isAllowedAvatarType(contentType)) {
            errors.put("avatarFile", "Le fichier doit etre une image JPG, PNG, GIF ou WebP.");
        }
        if (avatarFile.getSize() > 2_000_000) {
            errors.put("avatarFile", "L'image ne doit pas depasser 2 Mo.");
        }
        return errors;
    }

    private boolean isAllowedAvatarType(String contentType) {
        return "image/jpeg".equals(contentType)
                || "image/png".equals(contentType)
                || "image/gif".equals(contentType)
                || "image/webp".equals(contentType);
    }
}
