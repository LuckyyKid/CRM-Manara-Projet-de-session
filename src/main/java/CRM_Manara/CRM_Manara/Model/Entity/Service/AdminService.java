package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AnimationRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.ActivityRepo;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import CRM_Manara.CRM_Manara.Repository.EnfantRepo;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    ActivityRepo activityRepo;

    @Autowired
    AnimationRepo animationRepo;

    @Autowired
    AnimateurRepo animateurRepo;

    @Autowired
    AdminRepo adminRepo;

    @Autowired
    ParentRepo parentRepo;

    @Autowired
    EnfantRepo enfantRepo;

    @Autowired
    UserRepo userRepo;

    @Autowired
    InscriptionRepo inscriptionRepo;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    EmailService emailService;

    @Autowired
    ParentNotificationService parentNotificationService;


    @Transactional(readOnly = true)
    public List<Activity> getAllActivities() {
        return activityRepo.findAll();
    }

    @Transactional(readOnly = true)
    public long countActivities() {
        return activityRepo.count();
    }

    @Transactional(readOnly = true)
    public Activity getActivityById(Long id) {
        return activityRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activite introuvable"));
    }

    @Transactional
    public Activity createActivity(String name, String description, int ageMin, int ageMax, int capacity,
                                   status status, typeActivity type) {
        Activity activity = new Activity(name, description, ageMin, ageMax, capacity, status, type);
        return activityRepo.save(activity);
    }

    @Transactional
    public Activity updateActivity(Long id, String name, String description, int ageMin, int ageMax, int capacity,
                                   status status, typeActivity type) {
        Activity activity = getActivityById(id);
        activity.setActivyName(name);
        activity.setDescription(description);
        activity.setAgeMin(ageMin);
        activity.setAgeMax(ageMax);
        activity.setCapacity(capacity);
        return activityRepo.save(activity);
    }

    @Transactional
    public void deleteActivity(Long id) {
        animationRepo.findByActivityId(id).forEach(a -> deleteAnimation(a.getId()));
        activityRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Animation> getAllAnimations() {
        return animationRepo.findAll();
    }

    @Transactional(readOnly = true)
    public long countAnimations() {
        return animationRepo.count();
    }

    @Transactional(readOnly = true)
    public Animation getAnimationById(Long id) {
        return animationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animation introuvable"));
    }

    @Transactional
    public Animation createAnimation(Long activityId, Long animateurId, AnimationRole role,
                                     animationStatus status, LocalDateTime start, LocalDateTime end) {
        Activity activity = getActivityById(activityId);
        Animateur animateur = getAnimateurById(animateurId);
        Animation animation = new Animation(role, status, start, end);
        animation.setRole(role);
        animation.setStatusAnimation(status);
        animation.setStartTime(start);
        animation.setEndTime(end);
        animation.setActivity(activity);
        animation.setAnimateur(animateur);
        return animationRepo.save(animation);
    }

    @Transactional
    public Animation updateAnimation(Long id, Long activityId, Long animateurId, AnimationRole role,
                                     animationStatus status, LocalDateTime start, LocalDateTime end) {
        Animation animation = getAnimationById(id);
        Activity activity = getActivityById(activityId);
        Animateur animateur = getAnimateurById(animateurId);
        animation.setActivity(activity);
        animation.setAnimateur(animateur);
        animation.setRole(role);
        animation.setStatusAnimation(status);
        animation.setStartTime(start);
        animation.setEndTime(end);
        return animationRepo.save(animation);
    }

    @Transactional
    public void deleteAnimation(Long id) {
        List<Inscription> inscriptions = inscriptionRepo.findByAnimationId(id);
        if (!inscriptions.isEmpty()) {
            inscriptionRepo.deleteAll(inscriptions);
        }
        animationRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Animateur> getAllAnimateurs() {
        return animateurRepo.findAll();
    }

    @Transactional(readOnly = true)
    public long countAnimateurs() {
        return animateurRepo.count();
    }

    @Transactional(readOnly = true)
    public long countInscriptions() {
        return inscriptionRepo.count();
    }

    @Transactional(readOnly = true)
    public long countPendingInscriptions() {
        return inscriptionRepo.findAll().stream()
                .filter(inscription -> inscription.getStatusInscription() == statusInscription.EN_ATTENTE)
                .count();
    }

    @Transactional(readOnly = true)
    public long countWaitlistEntries() {
        return getAllAnimations().stream()
                .mapToLong(animation -> ((Number) getAnimationCapacitySnapshot(animation).get("waitlist")).longValue())
                .sum();
    }

    @Transactional(readOnly = true)
    public long countActiveParents() {
        return parentRepo.findAll().stream()
                .filter(parent -> parent.getUser() != null && parent.getUser().isEnabled())
                .count();
    }

    @Transactional(readOnly = true)
    public long countPendingChildren() {
        return enfantRepo.findAll().stream()
                .filter(enfant -> !enfant.isActive())
                .count();
    }

    @Transactional(readOnly = true)
    public int getAverageFillRate() {
        List<Animation> animations = getAllAnimations().stream()
                .filter(animation -> animation.getActivity() != null && animation.getActivity().getCapacity() > 0)
                .toList();
        if (animations.isEmpty()) {
            return 0;
        }
        int total = animations.stream()
                .mapToInt(animation -> ((Number) getAnimationCapacitySnapshot(animation).get("fillRate")).intValue())
                .sum();
        return total / animations.size();
    }


    @Transactional(readOnly = true)
    public long countInscriptionsForAnimation(Long animationId) {
        return inscriptionRepo.countByAnimationId(animationId);
    }

    @Transactional(readOnly = true)
    public long countInscriptionsForActivity(Long activityId) {
        return inscriptionRepo.countByActivityId(activityId);
    }

    @Transactional(readOnly = true)
    public Animateur getAnimateurById(Long id) {
        return animateurRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Animateur introuvable"));
    }

    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        if (email == null) {
            return false;
        }
        return !userRepo.existsByEmail(email.trim());
    }

    @Transactional
    public Animateur createAnimateur(String nom, String prenom, String email, String password) {
        String hash = passwordEncoder.encode(password);
        User user = new User(email, hash);
        user.setRole(SecurityRole.ROLE_ANIMATEUR);
        // ADDED
        user.setEnabled(true);
        User savedUser = userRepo.save(user);
        Animateur animateur = new Animateur(nom, prenom);
        animateur.setUser(savedUser);
        return animateurRepo.save(animateur);
    }

    @Transactional
    public Animateur updateAnimateur(Long id, String nom, String prenom) {
        Animateur animateur = getAnimateurById(id);
        animateur.setNom(nom);
        animateur.setPrenom(prenom);
        return animateurRepo.save(animateur);
    }

    @Transactional
    public void deleteAnimateur(Long id) {
        animationRepo.findByAnimateurId(id).forEach(a -> deleteAnimation(a.getId()));
        animateurRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Administrateurs> getAllAdmins() {
        return adminRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Parent> getAllParents() {
        return parentRepo.findAll().stream()
                .sorted(Comparator.comparing(Parent::getNom).thenComparing(Parent::getPrenom))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Parent getParentById(Long id) {
        return parentRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parent introuvable"));
    }

    @Transactional(readOnly = true)
    public Enfant getEnfantById(Long id) {
        return enfantRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Enfant introuvable"));
    }

    @Transactional
    public Parent updateParentEnabled(Long id, boolean enabled) {
        Parent parent = getParentById(id);
        if (parent.getUser() == null) {
            throw new IllegalArgumentException("Aucun compte utilisateur lie a ce parent");
        }
        parent.getUser().setEnabled(enabled);
        userRepo.save(parent.getUser());
        parentNotificationService.createForParent(
                parent,
                "COMPTE",
                enabled ? "Compte approuvé" : "Compte désactivé",
                enabled
                        ? "Votre compte parent a été approuvé par l'administration."
                        : "Votre compte parent a été désactivé par l'administration."
        );
        return parent;
    }

    @Transactional
    public Enfant updateEnfantActive(Long id, boolean active) {
        Enfant enfant = getEnfantById(id);
        enfant.setActive(active);
        Enfant saved = enfantRepo.save(enfant);
        parentNotificationService.createForParent(
                saved.getParent(),
                "ENFANT",
                active ? "Enfant approuvé" : "Enfant désactivé",
                active
                        ? saved.getPrenom() + " " + saved.getNom() + " est maintenant actif et peut être inscrit aux activités."
                        : saved.getPrenom() + " " + saved.getNom() + " a été désactivé par l'administration."
        );
        return saved;
    }

    @Transactional
    public void deleteParent(Long id) {
        Parent parent = getParentById(id);
        User user = parent.getUser();
        parentRepo.delete(parent);
        if (user != null) {
            userRepo.delete(user);
        }
    }

    @Transactional
    public void deleteEnfant(Long id) {
        Enfant enfant = getEnfantById(id);
        Parent parent = enfant.getParent();
        String childName = enfant.getPrenom() + " " + enfant.getNom();
        enfantRepo.delete(enfant);
        parentNotificationService.createForParent(
                parent,
                "ENFANT",
                "Enfant supprimé",
                "Le profil de " + childName + " a été supprimé par l'administration."
        );
    }

    @Transactional(readOnly = true)
    public List<Inscription> getAllInscriptions() {
        return inscriptionRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Inscription> getPendingInscriptions() {
        return inscriptionRepo.findAll().stream()
                .filter(inscription -> inscription.getStatusInscription() == statusInscription.EN_ATTENTE)
                .sorted(Comparator.comparing(Inscription::getId).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Inscription> getProcessedInscriptions() {
        return inscriptionRepo.findAll().stream()
                .filter(inscription -> inscription.getStatusInscription() != statusInscription.EN_ATTENTE)
                .sorted(Comparator.comparing(Inscription::getId).reversed())
                .collect(Collectors.toList());
    }

    @Transactional
    public Inscription approveInscription(Long id) {
        Inscription inscription = inscriptionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscription introuvable"));
        Map<String, Object> snapshot = getAnimationCapacitySnapshot(inscription.getAnimation());
        int remaining = ((Number) snapshot.get("remaining")).intValue();
        if (remaining <= 0 && inscription.getStatusInscription() == statusInscription.EN_ATTENTE) {
            throw new IllegalStateException("Plus de places disponibles sur cette session. La demande reste en attente.");
        }
        inscription.setStatusInscription(statusInscription.APPROUVEE);
        Inscription saved = inscriptionRepo.save(inscription);
        if (saved.getEnfant() != null
                && saved.getEnfant().getParent() != null
                && saved.getEnfant().getParent().getUser() != null) {
            emailService.sendInscriptionConfirmation(saved.getEnfant().getParent().getUser().getEmail(), saved);
            parentNotificationService.createForParent(
                    saved.getEnfant().getParent(),
                    "INSCRIPTION",
                    "Inscription approuvée",
                    "La demande pour " + saved.getEnfant().getPrenom() + " a été approuvée. Un reçu de confirmation a été envoyé par email."
            );
        }
        return saved;
    }

    @Transactional
    public Inscription rejectInscription(Long id) {
        Inscription inscription = inscriptionRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Inscription introuvable"));
        inscription.setStatusInscription(statusInscription.REFUSEE);
        Inscription saved = inscriptionRepo.save(inscription);
        parentNotificationService.createForParent(
                saved.getEnfant().getParent(),
                "INSCRIPTION",
                "Demande refusée",
                "La demande d'inscription pour " + saved.getEnfant().getPrenom() + " a été refusée."
        );
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Inscription> getRecentInscriptions(int limit) {
        return inscriptionRepo.findAll().stream()
                .sorted(Comparator.comparing(Inscription::getId).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Animation> getUpcomingAnimations(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return animationRepo.findAll().stream()
                .filter(a -> a.getStartTime() != null && a.getStartTime().isAfter(now))
                .sorted(Comparator.comparing(Animation::getStartTime))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long countOpenActivities() {
        return activityRepo.findAll().stream()
                .filter(a -> a.getStatus() == status.OUVERTE)
                .count();
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
        return snapshot;
    }

    @Transactional(readOnly = true)
    public Map<Long, Map<String, Object>> getAnimationCapacitySnapshots() {
        Map<Long, Map<String, Object>> snapshots = new LinkedHashMap<>();
        for (Animation animation : getAllAnimations()) {
            snapshots.put(animation.getId(), getAnimationCapacitySnapshot(animation));
        }
        return snapshots;
    }
}
