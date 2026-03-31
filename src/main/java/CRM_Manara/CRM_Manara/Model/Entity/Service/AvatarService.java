package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class AvatarService {
    private static final String DEFAULT_AVATAR_URL = "/images/default-avatar.svg";
    private static final Path AVATAR_STORAGE = Paths.get("storage", "avatars");

    private final UserRepo userRepo;
    private final ParentRepo parentRepo;
    private final AnimateurRepo animateurRepo;
    private final AdminRepo adminRepo;

    public AvatarService(UserRepo userRepo,
                         ParentRepo parentRepo,
                         AnimateurRepo animateurRepo,
                         AdminRepo adminRepo) {
        this.userRepo = userRepo;
        this.parentRepo = parentRepo;
        this.animateurRepo = animateurRepo;
        this.adminRepo = adminRepo;
    }

    @Transactional
    public void assignDefaultAvatar(User user, String displayName) {
        if (user == null) {
            return;
        }
        if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
            return;
        }
        user.setAvatarUrl(DEFAULT_AVATAR_URL);
        userRepo.save(user);
    }

    @Transactional
    public void resetToDefaultAvatar(User user) {
        if (user == null) {
            return;
        }
        deleteLocalAvatarIfNeeded(user.getAvatarUrl());
        user.setAvatarUrl(DEFAULT_AVATAR_URL);
        userRepo.save(user);
    }

    @Transactional
    public void assignOAuthAvatar(User user, String displayName, String pictureUrl) {
        if (user == null) {
            return;
        }
        user.setAvatarUrl(DEFAULT_AVATAR_URL);
        userRepo.save(user);
    }

    @Transactional
    public void ensureAvatarsForExistingUsers() {
        List<User> users = userRepo.findAll();
        for (User user : users) {
            if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
                user.setAvatarUrl(DEFAULT_AVATAR_URL);
            }
        }
        userRepo.saveAll(users);
    }

    @Transactional(readOnly = true)
    public String resolveAvatarUrl(String email) {
        return userRepo.findByEmail(email)
                .map(user -> {
                    if (user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank()) {
                        return user.getAvatarUrl();
                    }
                    return DEFAULT_AVATAR_URL;
                })
                .orElse(DEFAULT_AVATAR_URL);
    }

    @Transactional(readOnly = true)
    public String resolveDisplayName(String email) {
        return userRepo.findByEmail(email)
                .map(this::resolveDisplayName)
                .orElse(email);
    }

    @Transactional(readOnly = true)
    public String resolveDisplayName(User user) {
        if (user == null) {
            return "Utilisateur";
        }

        Parent parent = parentRepo.findByUser(user).orElse(null);
        if (parent != null) {
            return parent.getPrenom() + " " + parent.getNom();
        }

        Animateur animateur = animateurRepo.findByUser(user).orElse(null);
        if (animateur != null) {
            return animateur.getPrenom() + " " + animateur.getNom();
        }

        Administrateurs admin = adminRepo.findByUser(user).orElse(null);
        if (admin != null) {
            return admin.getPrenom() + " " + admin.getNom();
        }

        if (user.getEmail() != null && user.getEmail().contains("@")) {
            return user.getEmail().substring(0, user.getEmail().indexOf('@'));
        }

        return "Utilisateur";
    }

    @Transactional
    public void storeUploadedAvatar(User user, MultipartFile file) {
        if (user == null || file == null || file.isEmpty()) {
            return;
        }

        try {
            Files.createDirectories(AVATAR_STORAGE);
            String extension = resolveExtension(file.getOriginalFilename(), file.getContentType());
            String filename = "user-" + user.getId() + "-" + UUID.randomUUID() + extension;
            Path target = AVATAR_STORAGE.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            deleteLocalAvatarIfNeeded(user.getAvatarUrl());
            user.setAvatarUrl("/avatars/" + filename);
            userRepo.save(user);
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible d'enregistrer l'image de profil.", exception);
        }
    }

    private String resolveExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        if (contentType == null) {
            return ".png";
        }
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".png";
        };
    }

    private void deleteLocalAvatarIfNeeded(String avatarUrl) {
        if (avatarUrl == null || !avatarUrl.startsWith("/avatars/")) {
            return;
        }
        try {
            Files.deleteIfExists(AVATAR_STORAGE.resolve(avatarUrl.substring("/avatars/".length())));
        } catch (IOException ignored) {
        }
    }

}
