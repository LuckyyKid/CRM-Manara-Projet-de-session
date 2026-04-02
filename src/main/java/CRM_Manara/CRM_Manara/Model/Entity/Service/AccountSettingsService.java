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
import java.util.List;
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
    private final EmailService emailService;
    private final AdminNotificationService adminNotificationService;

    public AccountSettingsService(UserRepo userRepo,
                                  ParentRepo parentRepo,
                                  AnimateurRepo animateurRepo,
                                  AdminRepo adminRepo,
                                  PasswordEncoder passwordEncoder,
                                  AvatarService avatarService,
                                  EmailService emailService,
                                  AdminNotificationService adminNotificationService) {
        this.userRepo = userRepo;
        this.parentRepo = parentRepo;
        this.animateurRepo = animateurRepo;
        this.adminRepo = adminRepo;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
        this.emailService = emailService;
        this.adminNotificationService = adminNotificationService;
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
        String previousEmail = user.getEmail();
        Map<String, String> errors = validate(user, nom, prenom, email, adresse, newPassword, confirmPassword, avatarFile);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(errors.values().iterator().next());
        }

        String normalizedNom = nom.trim();
        String normalizedPrenom = prenom.trim();
        String normalizedEmail = email.trim();
        String normalizedAdresse = adresse == null ? "" : adresse.trim();

        String oldNom = null;
        String oldPrenom = null;
        String oldAdresse = null;
        String roleLabel = user.getRole().name().replace("ROLE_", "");

        user.setEmail(email.trim());
        boolean passwordChanged = newPassword != null && !newPassword.isBlank();
        if (newPassword != null && !newPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        boolean avatarChanged = false;
        if (resetAvatar) {
            avatarService.resetToDefaultAvatar(user);
            avatarChanged = true;
        }
        if (avatarFile != null && !avatarFile.isEmpty()) {
            avatarService.storeUploadedAvatar(user, avatarFile);
            avatarChanged = true;
        }

        Parent parent = parentRepo.findByUser(user).orElse(null);
        if (parent != null) {
            oldNom = parent.getNom();
            oldPrenom = parent.getPrenom();
            oldAdresse = parent.getAdresse();
            roleLabel = "PARENT";
            parent.setNom(normalizedNom);
            parent.setPrenom(normalizedPrenom);
            parent.setAdresse(normalizedAdresse);
            parentRepo.save(parent);
        }

        Animateur animateur = animateurRepo.findByUser(user).orElse(null);
        if (animateur != null) {
            oldNom = animateur.getNom();
            oldPrenom = animateur.getPrenom();
            roleLabel = "ANIMATEUR";
            animateur.setNom(normalizedNom);
            animateur.setPrenom(normalizedPrenom);
            animateurRepo.save(animateur);
        }

        Administrateurs admin = adminRepo.findByUser(user).orElse(null);
        if (admin != null) {
            oldNom = admin.getNom();
            oldPrenom = admin.getPrenom();
            roleLabel = "ADMIN";
            admin.setNom(normalizedNom);
            admin.setPrenom(normalizedPrenom);
            adminRepo.save(admin);
        }

        userRepo.save(user);

        String displayName = normalizedPrenom + " " + normalizedNom;
        String changeSummary = buildChangeSummary(
                oldPrenom,
                oldNom,
                oldAdresse,
                previousEmail,
                normalizedPrenom,
                normalizedNom,
                normalizedAdresse,
                normalizedEmail,
                passwordChanged,
                avatarChanged
        );
        if (changeSummary.isBlank()) {
            changeSummary = "Paramètres du compte enregistrés.";
        }
        emailService.sendAccountUpdatedConfirmation(normalizedEmail, displayName, roleLabel, changeSummary);
        emailService.notifyAdminsOfAccountUpdate(displayName, normalizedEmail, roleLabel, changeSummary);
        adminNotificationService.create(
                roleLabel,
                "COMPTE",
                "Compte modifié: " + displayName + " (" + normalizedEmail + "). " + changeSummary.replace("\n- ", " | ")
        );
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

    private String buildChangeSummary(String oldPrenom,
                                      String oldNom,
                                      String oldAdresse,
                                      String oldEmail,
                                      String newPrenom,
                                      String newNom,
                                      String newAdresse,
                                      String newEmail,
                                      boolean passwordChanged,
                                      boolean avatarChanged) {
        List<String> changes = new java.util.ArrayList<>();

        if (!safeEquals(oldPrenom, newPrenom) || !safeEquals(oldNom, newNom)) {
            changes.add("Nom complet mis à jour : " + newPrenom + " " + newNom);
        }
        if (!safeEquals(oldEmail, newEmail)) {
            changes.add("Courriel mis à jour : " + newEmail);
        }
        if (oldAdresse != null && !safeEquals(oldAdresse, newAdresse)) {
            changes.add("Adresse mise à jour : " + newAdresse);
        }
        if (passwordChanged) {
            changes.add("Mot de passe modifié");
        }
        if (avatarChanged) {
            changes.add("Photo de profil modifiée");
        }

        return String.join("\n- ", changes);
    }

    private boolean safeEquals(String left, String right) {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        return normalizedLeft.equals(normalizedRight);
    }
}
