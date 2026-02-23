package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AnimationRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.ActivityRepo;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
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
    UserRepo userRepo;

    @Autowired
    InscriptionRepo inscriptionRepo;

    @Autowired
    PasswordEncoder passwordEncoder;

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

    @Transactional
    public Animateur createAnimateur(String nom, String prenom, String email, String password) {
        String hash = passwordEncoder.encode(password);
        User user = new User(email, hash);
        user.setRole(SecurityRole.ROLE_ANIMATEUR);
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
    public List<Inscription> getAllInscriptions() {
        return inscriptionRepo.findAll();
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
                .filter(a -> a.getStartTime().isAfter(now))
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
}
