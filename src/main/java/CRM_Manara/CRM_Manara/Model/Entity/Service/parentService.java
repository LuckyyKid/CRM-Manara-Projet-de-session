package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Model.Entity.VerificationToken;
import CRM_Manara.CRM_Manara.Repository.ActivityRepo;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import CRM_Manara.CRM_Manara.Repository.EnfantRepo;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.ParentNotificationRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.Repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class parentService {

    @Autowired
    ParentRepo parentRepo;

    @Autowired
    UserRepo userRepo;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EnfantRepo enfantRepo;

    @Autowired
    ActivityRepo activityRepo;

    @Autowired
    AnimationRepo animationRepo;

    @Autowired
    InscriptionRepo inscriptionRepo;

    @Autowired
    EmailService emailService;

    // ADDED
    @Autowired
    VerificationTokenRepository verificationTokenRepository;

    @Autowired
    ParentNotificationService parentNotificationService;

    @Autowired
    AvatarService avatarService;

    @Autowired
    AnimateurNotificationService animateurNotificationService;

    @Autowired
    AdminNotificationService adminNotificationService;

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        if (email == null) {
            return false;
        }
        return !userRepo.existsByEmail(email.trim());
    }

    @Transactional
    public void createNewParent(String nom, String prenom, String adresse, String email, String password) {
        // ADDED
        System.out.println("STEP 4 REACHED - parentService.createNewParent()");
        System.out.println("Preparing parent signup for email: " + email);

        // ADDED
        if (userRepo.existsByEmail(email.trim())) {
            // ADDED
            System.out.println("STEP 4.1 REACHED - Email already exists: " + email);
            throw new IllegalArgumentException("Un compte existe deja avec cet email.");
        }

        String hash = passwordEncoder.encode(password);
        // ADDED
        System.out.println("STEP 5 REACHED - Password encoded for email: " + email);

        // MODIFIED
        User user1 = new User(email.trim(), hash);
        user1.setRole(SecurityRole.ROLE_PARENT);
        // ADDED
        user1.setEnabled(false);

        User userSaved = userRepo.save(user1);
        avatarService.assignDefaultAvatar(userSaved, prenom + " " + nom);
        // ADDED
        System.out.println("STEP 6 REACHED - User saved with id: " + userSaved.getId());

        Parent parent = new Parent(nom, prenom, adresse);
        parent.SetUser(userSaved);
        Parent savedParent = parentRepo.save(parent);
        // ADDED
        System.out.println("STEP 7 REACHED - Parent profile saved for user id: " + userSaved.getId());

        VerificationToken verificationToken = new VerificationToken(
                UUID.randomUUID().toString(),
                userSaved,
                LocalDateTime.now().plusHours(24)
        );
        verificationTokenRepository.save(verificationToken);

        emailService.sendEmail(
                userSaved.getEmail(),
                "Compte parent en attente d'approbation - CRM Manara",
                buildPendingApprovalEmail(userSaved.getEmail(), prenom + " " + nom)
        );
        emailService.notifyAdminsOfParentSignup(prenom + " " + nom, userSaved.getEmail(), "Formulaire");
        adminNotificationService.create(
                "PARENT",
                "COMPTE",
                "Nouveau compte parent créé en attente: " + prenom + " " + nom + " (" + userSaved.getEmail() + ")."
        );
        parentNotificationService.createForParent(
                savedParent,
                "COMPTE",
                "Compte créé",
                "Votre compte parent a été créé. Il est maintenant en attente d'approbation par l'administration."
        );
    }

    @Transactional
    public void verifyUser(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de verification invalide."));

        if (verificationToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Le lien de verification a expire.");
        }

        User user = verificationToken.getUser();
        verificationTokenRepository.deleteByUser(user);
    }

    private String buildPendingApprovalEmail(String email, String fullName) {
        return "Bonjour,\n\n" +
                "Merci pour votre inscription sur CRM Manara.\n" +
                "Le compte de " + fullName + " a bien été créé.\n" +
                "Votre demande est maintenant en attente d'approbation par l'administration.\n\n" +
                "Compte: " + email + "\n\n" +
                "Tant que le compte n'est pas approuvé, vous ne pourrez pas vous connecter.\n\n" +
                "Merci,\nCRM Manara";
    }

    @Transactional(readOnly = true)
    public Parent getParentByEmail(String email) {
        return parentRepo.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Parent introuvable pour cet email"));
    }

    @Transactional
    public Parent updateParentProfile(String email, String nom, String prenom, String adresse) {
        Parent parent = getParentByEmail(email);
        parent.setNom(nom);
        parent.setPrenom(prenom);
        parent.setAdresse(adresse);
        Parent savedParent = parentRepo.save(parent);
        adminNotificationService.create(
                "PARENT",
                "PROFIL",
                "Profil parent mis à jour: " + savedParent.getPrenom() + " " + savedParent.getNom() + "."
        );
        parentNotificationService.createForParent(
                savedParent,
                "PROFIL",
                "Profil mis à jour",
                "Vos informations de profil ont été mises à jour avec succès."
        );
        return savedParent;
    }

    @Transactional(readOnly = true)
    public List<Enfant> getEnfantsForParent(String email) {
        Parent parent = getParentByEmail(email);
        return enfantRepo.findByParentId(parent.getId());
    }

    @Transactional(readOnly = true)
    public Enfant getEnfantForParent(Long enfantId, String email) {
        Parent parent = getParentByEmail(email);
        return enfantRepo.findByIdAndParentId(enfantId, parent.getId())
                .orElseThrow(() -> new IllegalArgumentException("Enfant introuvable"));
    }

    @Transactional
    public Enfant createEnfantForParent(String email, String nom, String prenom, Date dateNaissance) {
        Parent parent = getParentByEmail(email);
        Enfant enfant = new Enfant(nom, prenom, dateNaissance);
        enfant.setActive(false);
        parent.AddEnfant(enfant);
        parentRepo.save(parent);
        adminNotificationService.create(
                "PARENT",
                "ENFANT",
                "Nouvel enfant ajouté: " + prenom + " " + nom + " pour " + parent.getPrenom() + " " + parent.getNom() + "."
        );
        parentNotificationService.createForParent(
                parent,
                "ENFANT",
                "Nouvel enfant ajouté",
                prenom + " " + nom + " a été ajouté. Son profil est en attente d'approbation par l'administration."
        );
        return enfant;
    }

    @Transactional(readOnly = true)
    public List<Enfant> getActiveEnfantsForParent(String email) {
        return getEnfantsForParent(email).stream()
                .filter(Enfant::isActive)
                .collect(Collectors.toList());
    }

    @Transactional
    public Enfant updateEnfantForParent(Long enfantId, String email, String nom, String prenom, Date dateNaissance) {
        Enfant enfant = getEnfantForParent(enfantId, email);
        enfant.setNom(nom);
        enfant.setPrenom(prenom);
        enfant.setDate_de_naissance(dateNaissance);
        Enfant savedEnfant = enfantRepo.save(enfant);
        adminNotificationService.create(
                "PARENT",
                "ENFANT",
                "Profil enfant mis à jour: " + savedEnfant.getPrenom() + " " + savedEnfant.getNom() + "."
        );
        parentNotificationService.createForParent(
                savedEnfant.getParent(),
                "ENFANT",
                "Profil enfant mis à jour",
                "Le profil de " + savedEnfant.getPrenom() + " " + savedEnfant.getNom() + " a été mis à jour."
        );
        return savedEnfant;
    }

    @Transactional
    public void deleteEnfantForParent(Long enfantId, String email) {
        Enfant enfant = getEnfantForParent(enfantId, email);
        Parent parent = enfant.getParent();
        String fullName = enfant.getPrenom() + " " + enfant.getNom();
        enfantRepo.delete(enfant);
        adminNotificationService.create(
                "PARENT",
                "ENFANT",
                "Enfant supprimé: " + fullName + " du compte de " + parent.getPrenom() + " " + parent.getNom() + "."
        );
        parentNotificationService.createForParent(
                parent,
                "ENFANT",
                "Profil enfant supprimé",
                "Le profil de " + fullName + " a été supprimé de votre compte."
        );
    }

    @Transactional(readOnly = true)
    public List<Activity> getAllActivities() {
        return activityRepo.findAll();
    }

    @Transactional(readOnly = true)
    public long countInscriptionsForEnfant(Long enfantId) {
        return inscriptionRepo.countByEnfantId(enfantId);
    }

    @Transactional(readOnly = true)
    public List<Animation> getAnimationsForActivity(Long activityId) {
        return animationRepo.findByActivityId(activityId);
    }

    @Transactional
    public Inscription inscrireEnfant(Long enfantId, Long animationId, String email) {
        Parent parent = getParentByEmail(email);
        Enfant enfant = enfantRepo.findByIdAndParentId(enfantId, parent.getId())
                .orElseThrow(() -> new IllegalArgumentException("Enfant introuvable pour ce parent"));
        if (!enfant.isActive()) {
            throw new IllegalArgumentException("Cet enfant doit être approuvé par l'administration avant toute inscription.");
        }
        Animation animation = animationRepo.findById(animationId)
                .orElseThrow(() -> new IllegalArgumentException("Animation introuvable"));
        validateAgeEligibility(enfant, animation);
        boolean alreadyRequestedForActivity = inscriptionRepo.findByEnfantIdAndActivityId(enfantId, animation.getActivity().getId()).stream()
                .anyMatch(existing -> existing.getStatusInscription() != statusInscription.REFUSEE
                        && existing.getStatusInscription() != statusInscription.ANNULÉE);
        if (alreadyRequestedForActivity) {
            throw new IllegalArgumentException("Une demande existe déjà pour cet enfant sur cette activité.");
        }
        inscriptionRepo.findByEnfantIdAndAnimationId(enfantId, animationId)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Une demande existe déjà pour cet enfant sur cette session.");
                });
        Inscription inscription = new Inscription(enfant, animation);
        inscription.setStatusInscription(statusInscription.EN_ATTENTE);
        Inscription saved = inscriptionRepo.save(inscription);
        Map<String, Object> capacity = getAnimationCapacitySnapshot(animation);
        int remaining = (int) capacity.get("remaining");
        int pending = (int) capacity.get("pending");
        String message = remaining > 0
                ? "Votre demande pour " + enfant.getPrenom() + " a été envoyée. Elle sera validée par l'administration."
                : "Votre demande pour " + enfant.getPrenom() + " a été envoyée. La session est complète pour l'instant et la demande rejoint la liste d'attente.";
        if (remaining == 0 && pending > 0) {
            message += " Position estimée en attente: " + capacity.get("waitlistPosition") + ".";
        }
        parentNotificationService.createForParent(
                parent,
                "INSCRIPTION",
                "Demande d'inscription envoyée",
                message
        );
        adminNotificationService.create(
                "PARENT",
                "INSCRIPTION",
                "Demande d'inscription: " + enfant.getPrenom() + " " + enfant.getNom()
                        + " pour " + animation.getActivity().getActivyName()
                        + " le " + animation.getStartTime() + "."
        );
        if (animation.getAnimateur() != null) {
            animateurNotificationService.createForAnimateur(
                    animation.getAnimateur(),
                    "INSCRIPTION",
                    "Nouvel enfant ajouté à votre session",
                    enfant.getPrenom() + " " + enfant.getNom()
                            + " a fait l'objet d'une nouvelle demande pour "
                            + animation.getActivity().getActivyName()
                            + " le " + animation.getStartTime() + "."
            );
        }
        emailService.notifyAdminsOfInscriptionRequest(saved);
        return saved;
    }

    private void validateAgeEligibility(Enfant enfant, Animation animation) {
        if (enfant.getDate_de_naissance() == null || animation.getStartTime() == null || animation.getActivity() == null) {
            return;
        }

        Date birthDateValue = enfant.getDate_de_naissance();
        LocalDate birthDate = birthDateValue instanceof java.sql.Date sqlDate
                ? sqlDate.toLocalDate()
                : birthDateValue.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate activityDate = animation.getStartTime().toLocalDate();
        int ageAtActivity = Period.between(birthDate, activityDate).getYears();

        if (ageAtActivity < animation.getActivity().getAgeMin() || ageAtActivity > animation.getActivity().getAgeMax()) {
            throw new IllegalArgumentException(
                    "L'age de " + enfant.getPrenom() + " ne correspond pas a cette activite. " +
                            "Age requis: " + animation.getActivity().getAgeMin() + " a " + animation.getActivity().getAgeMax() + " ans."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Inscription> getInscriptionsForParent(String email) {
        Parent parent = getParentByEmail(email);
        return inscriptionRepo.findByParentId(parent.getId());
    }

    @Transactional(readOnly = true)
    public long countEnfantsForParent(String email) {
        Parent parent = getParentByEmail(email);
        return enfantRepo.findByParentId(parent.getId()).size();
    }

    @Transactional(readOnly = true)
    public long countInscriptionsForParent(String email) {
        Parent parent = getParentByEmail(email);
        return inscriptionRepo.findByParentId(parent.getId()).size();
    }

    @Transactional(readOnly = true)
    public List<Inscription> getUpcomingInscriptionsForParent(String email, int limit) {
        Parent parent = getParentByEmail(email);
        LocalDateTime now = LocalDateTime.now();
        return inscriptionRepo.findByParentId(parent.getId()).stream()
                .filter(i -> i.getAnimation() != null
                        && i.getAnimation().getStartTime() != null
                        && i.getAnimation().getStartTime().isAfter(now))
                .sorted(Comparator.comparing(i -> i.getAnimation().getStartTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CRM_Manara.CRM_Manara.Model.Entity.ParentNotification> getNotificationsForParent(String email, int limit) {
        Parent parent = getParentByEmail(email);
        return parentNotificationService.getNotificationsForParent(parent.getId(), limit);
    }

    @Transactional(readOnly = true)
    public long countUnreadNotificationsForParent(String email) {
        Parent parent = getParentByEmail(email);
        return parentNotificationService.countUnreadForParent(parent.getId());
    }

    @Transactional(readOnly = true)
    public List<CRM_Manara.CRM_Manara.Model.Entity.ParentNotification> getArchivedNotificationsForParent(String email, int limit) {
        Parent parent = getParentByEmail(email);
        return parentNotificationService.getArchivedNotificationsForParent(parent.getId(), limit);
    }

    @Transactional
    public void markNotificationsAsRead(String email) {
        Parent parent = getParentByEmail(email);
        parentNotificationService.markAllAsReadForParent(parent.getId());
    }

    @Transactional
    public void markNotificationAsRead(String email, Long notificationId) {
        Parent parent = getParentByEmail(email);
        parentNotificationService.markAsRead(parent.getId(), notificationId);
    }

    @Transactional
    public void archiveNotification(String email, Long notificationId) {
        Parent parent = getParentByEmail(email);
        parentNotificationService.archive(parent.getId(), notificationId);
    }

    @Transactional
    public void restoreNotification(String email, Long notificationId) {
        Parent parent = getParentByEmail(email);
        parentNotificationService.restore(parent.getId(), notificationId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Map<String, Object>> getAnimationCapacitySnapshotsForActivities(List<Activity> activities) {
        Map<Long, Map<String, Object>> snapshots = new LinkedHashMap<>();
        for (Activity activity : activities) {
            for (Animation animation : getAnimationsForActivity(activity.getId())) {
                snapshots.put(animation.getId(), getAnimationCapacitySnapshot(animation));
            }
        }
        return snapshots;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAnimationCapacitySnapshot(Animation animation) {
        List<Inscription> inscriptions = inscriptionRepo.findByAnimationId(animation.getId());
        int approved = (int) inscriptions.stream()
                .filter(i -> i.getStatusInscription() == statusInscription.APPROUVEE || i.getStatusInscription() == statusInscription.ACTIF)
                .count();
        int pending = (int) inscriptions.stream()
                .filter(i -> i.getStatusInscription() == statusInscription.EN_ATTENTE)
                .count();
        int capacity = animation.getActivity() != null ? animation.getActivity().getCapacity() : 0;
        int remaining = Math.max(0, capacity - approved);
        int waitlist = Math.max(0, pending - remaining);
        int fillRate = capacity > 0 ? Math.min(100, (approved * 100) / capacity) : 0;

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("approved", approved);
        snapshot.put("pending", pending);
        snapshot.put("capacity", capacity);
        snapshot.put("remaining", remaining);
        snapshot.put("waitlist", waitlist);
        snapshot.put("fillRate", fillRate);
        snapshot.put("full", remaining == 0);
        snapshot.put("waitlistPosition", waitlist > 0 ? waitlist : 0);
        return snapshot;
    }
}
