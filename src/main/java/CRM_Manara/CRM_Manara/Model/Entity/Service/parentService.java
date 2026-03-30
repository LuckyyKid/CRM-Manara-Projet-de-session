package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
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
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.Repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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
        // ADDED
        System.out.println("STEP 6 REACHED - User saved with id: " + userSaved.getId());

        Parent parent = new Parent(nom, prenom, adresse);
        parent.SetUser(userSaved);
        parentRepo.save(parent);
        // ADDED
        System.out.println("STEP 7 REACHED - Parent profile saved for user id: " + userSaved.getId());

        // ADDED
        VerificationToken verificationToken = new VerificationToken(
                UUID.randomUUID().toString(),
                userSaved,
                LocalDateTime.now().plusHours(24)
        );
        verificationTokenRepository.save(verificationToken);
        // ADDED
        System.out.println("STEP 8 REACHED - Verification token saved for email: " + userSaved.getEmail());
        System.out.println("Verification token value: " + verificationToken.getToken());

        // ADDED
        System.out.println("STEP 9 REACHED - About to call EmailService.sendEmail() for: " + userSaved.getEmail());
        emailService.sendEmail(
                userSaved.getEmail(),
                "Verification de votre compte CRM Manara",
                buildVerificationEmail(userSaved.getEmail(), verificationToken.getToken())
        );
        // ADDED
        System.out.println("STEP 10 REACHED - Returned from EmailService.sendEmail() for: " + userSaved.getEmail());
    }

    // ADDED
    @Transactional
    public void verifyUser(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de verification invalide."));

        if (verificationToken.getExpirationDate().isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(verificationToken);
            throw new IllegalArgumentException("Le lien de verification a expire.");
        }

        User user = verificationToken.getUser();
        user.setEnabled(true);
        userRepo.save(user);
        verificationTokenRepository.deleteByUser(user);
    }

    // ADDED
    private String buildVerificationEmail(String email, String token) {
        return "Bonjour,\n\n" +
                "Merci pour votre inscription sur CRM Manara.\n" +
                "Veuillez verifier votre adresse courriel en cliquant sur le lien suivant :\n" +
                "http://localhost:8080/verify?token=" + token + "\n\n" +
                "Compte: " + email + "\n\n" +
                "Ce lien expire dans 24 heures.\n\n" +
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
        enfant.setDate_de_naissance(dateNaissance);
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
        Inscription saved = inscriptionRepo.save(inscription);
        if (parent.getUser() != null) {
            emailService.sendInscriptionConfirmation(parent.getUser().getEmail(), saved);
        }
        return saved;
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
                .filter(i -> i.getAnimation().getStartTime().isAfter(now))
                .sorted(Comparator.comparing(i -> i.getAnimation().getStartTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
