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
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.ActivityRepo;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Activity> getAllActivities() {
        return activityRepo.findAll();
    }

    @Transactional
    public Activity createActivity(String name, String description, int ageMin, int ageMax, int capacity,
                                   status status, typeActivity type) {
        Activity activity = new Activity(name, description, ageMin, ageMax, capacity, status, type);
        return activityRepo.save(activity);
    }

    @Transactional(readOnly = true)
    public List<Animation> getAllAnimations() {
        return animationRepo.findAll();
    }

    @Transactional
    public Animation createAnimation(Long activityId, Long animateurId, AnimationRole role,
                                     animationStatus status, LocalDateTime start, LocalDateTime end) {
        Activity activity = activityRepo.findById(activityId)
                .orElseThrow(() -> new IllegalArgumentException("Activite introuvable"));
        Animateur animateur = animateurRepo.findById(animateurId)
                .orElseThrow(() -> new IllegalArgumentException("Animateur introuvable"));
        Animation animation = new Animation(role, status, start, end);
        animation.setRole(role);
        animation.setStatusAnimation(status);
        animation.setStartTime(start);
        animation.setEndTime(end);
        animation.setActivity(activity);
        animation.setAnimateur(animateur);
        return animationRepo.save(animation);
    }

    @Transactional(readOnly = true)
    public List<Animateur> getAllAnimateurs() {
        return animateurRepo.findAll();
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

    @Transactional(readOnly = true)
    public List<Administrateurs> getAllAdmins() {
        return adminRepo.findAll();
    }
}
