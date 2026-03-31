package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class AccountSettingsService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepo userRepo;
    private final ParentRepo parentRepo;
    private final AnimateurRepo animateurRepo;
    private final AdminRepo adminRepo;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;

    public AccountSettingsService(UserRepo userRepo,
                                  ParentRepo parentRepo,
                                  AnimateurRepo animateurRepo,
                                  AdminRepo adminRepo,
                                  PasswordEncoder passwordEncoder,
                                  AvatarService avatarService) {
        this.userRepo = userRepo;
        this.parentRepo = parentRepo;
        this.animateurRepo = animateurRepo;
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
    }

    @Transactional(readOnly = true)
    public AccountSettingsView getSettings(String email) {
        User user = getUser(email);
        AccountSettingsView view = new AccountSettingsView();
        view.setEmail(user.getEmail());
        view.setAvatarUrl(user.getAvatarUrl());
        view.setRole(user.getRole().name().replace("ROLE_", ""));

        Parent parent = parentRepo.findByUser(user).orElse(null);
        if (parent != null) {
            view.setNom(parent.getNom());
            view.setPrenom(parent.getPrenom());
            view.setAdresse(parent.getAdresse());
            return view;
        }

        Animateur animateur = animateurRepo.findByUser(user).orElse(null);
        if (animateur != null) {
            view.setNom(animateur.getNom());
            view.setPrenom(animateur.getPrenom());
            return view;
        }

        Administrateurs admin = adminRepo.findByUser(user).orElse(null);
        if (admin != null) {
            view.setNom(admin.getNom());
            view.setPrenom(admin.getPrenom());
        }

        return view;
    }

    @Transactional
    public void updateSettings(String currentEmail,
                               String nom,
                               String prenom,
                               String email,
                               String adresse,
                               String newPassword,
                               String confirmPassword,
                               boolean resetAvatar,
                               MultipartFile avatarFile) {
        User user = getUser(currentEmail);
        Map<String, String> errors = validate(user, nom, prenom, email, adresse, newPassword, confirmPassword, avatarFile);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.values().iterator().next());
        }

        user.setEmail(email.trim());
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        if (resetAvatar) {
            avatarService.resetToDefaultAvatar(user);
        }
        if (avatarFile != null && !avatarFile.isEmpty()) {
            avatarService.storeUploadedAvatar(user, avatarFile);
        }

        Parent parent = parentRepo.findByUser(user).orElse(null);
        if (parent != null) {
            parent.setNom(nom.trim());
            parent.setPrenom(prenom.trim());
            parent.setAdresse(adresse == null ? "" : adresse.trim());
            parentRepo.save(parent);
        }

        Animateur animateur = animateurRepo.findByUser(user).orElse(null);
        if (animateur != null) {
            animateur.setNom(nom.trim());
            animateur.setPrenom(prenom.trim());
            animateurRepo.save(animateur);
        }

        Administrateurs admin = adminRepo.findByUser(user).orElse(null);
        if (admin != null) {
            admin.setNom(nom.trim());
            admin.setPrenom(prenom.trim());
            adminRepo.save(admin);
        }

        userRepo.save(user);
    }

    @Transactional(readOnly = true)
    public Map<String, String> validateForApi(String currentEmail,
                                              String nom,
                                              String prenom,
                                              String email,
                                              String adresse,
                                              String newPassword,
                                              String confirmPassword,
                                              MultipartFile avatarFile) {
        return validate(getUser(currentEmail), nom, prenom, email, adresse, newPassword, confirmPassword, avatarFile);
    }

    private Map<String, String> validate(User currentUser,
                                         String nom,
                                         String prenom,
                                         String email,
                                         String adresse,
                                         String newPassword,
                                         String confirmPassword,
                                         MultipartFile avatarFile) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (nom == null || nom.trim().length() < 2) {
            errors.put("nom", "Le nom doit contenir au moins 2 caractères.");
        }
        if (prenom == null || prenom.trim().length() < 2) {
            errors.put("prenom", "Le prénom doit contenir au moins 2 caractères.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            errors.put("email", "Veuillez saisir un courriel valide.");
        } else if (!email.trim().equalsIgnoreCase(currentUser.getEmail())
                && userRepo.existsByEmail(email.trim())) {
            errors.put("email", "Ce courriel est déjà utilisé.");
        }
        if (adresse != null && adresse.length() > 255) {
            errors.put("adresse", "L'adresse ne peut pas dépasser 255 caractères.");
        }
        if (newPassword != null && !newPassword.isBlank()) {
            if (newPassword.length() < 8) {
                errors.put("newPassword", "Le mot de passe doit contenir au moins 8 caractères.");
            }
            if (!newPassword.equals(confirmPassword)) {
                errors.put("confirmPassword", "La confirmation du mot de passe ne correspond pas.");
            }
        }
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String contentType = avatarFile.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                errors.put("avatarFile", "Le fichier doit être une image.");
            }
            if (avatarFile.getSize() > 2_000_000) {
                errors.put("avatarFile", "L'image ne doit pas dépasser 2 Mo.");
            }
        }

        return errors;
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
    }
}
