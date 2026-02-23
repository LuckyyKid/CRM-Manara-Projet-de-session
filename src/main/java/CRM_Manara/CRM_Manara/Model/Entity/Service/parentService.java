package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.ActivityRepo;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import CRM_Manara.CRM_Manara.Repository.EnfantRepo;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

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

    @Transactional
    public void createNewParent(String nom, String prenom, String adresse, String email, String password)
    {
       String hash = passwordEncoder.encode(password);

        User user1 = new User(email,hash);

        user1.setRole(SecurityRole.ROLE_PARENT);

        User userSaved = userRepo.save(user1);

        Parent parent = new Parent(nom, prenom, adresse);
        parent.SetUser(userSaved);
        parentRepo.save(parent);
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
        return parentRepo.save(parent);
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
        parent.AddEnfant(enfant);
        parentRepo.save(parent);
        return enfant;
    }

    @Transactional
    public Enfant updateEnfantForParent(Long enfantId, String email, String nom, String prenom, Date dateNaissance) {
        Enfant enfant = getEnfantForParent(enfantId, email);
        enfant.setNom(nom);
        enfant.setPrenom(prenom);
        return enfantRepo.save(enfant);
    }

    @Transactional
    public void deleteEnfantForParent(Long enfantId, String email) {
        Enfant enfant = getEnfantForParent(enfantId, email);
        enfantRepo.delete(enfant);
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
        Animation animation = animationRepo.findById(animationId)
                .orElseThrow(() -> new IllegalArgumentException("Animation introuvable"));
        Inscription inscription = new Inscription(enfant, animation);
        return inscriptionRepo.save(inscription);
    }

    @Transactional(readOnly = true)
    public List<Inscription> getInscriptionsForParent(String email) {
        Parent parent = getParentByEmail(email);
        return inscriptionRepo.findByParentId(parent.getId());
    }
}
