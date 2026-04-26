package CRM_Manara.CRM_Manara.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
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
import CRM_Manara.CRM_Manara.service.ParentNotificationService;
import CRM_Manara.CRM_Manara.service.AdminNotificationService;
import CRM_Manara.CRM_Manara.service.AnimateurNotificationService;
import CRM_Manara.CRM_Manara.service.AvatarService;
import CRM_Manara.CRM_Manara.service.BillingService;
import CRM_Manara.CRM_Manara.service.parentService;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.ActivityRepo;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import CRM_Manara.CRM_Manara.Repository.EnfantRepo;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.Repository.VerificationTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {

    @Mock
    ParentRepo parentRepo;
    @Mock
    UserRepo userRepo;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    EnfantRepo enfantRepo;
    @Mock
    ActivityRepo activityRepo;
    @Mock
    AnimationRepo animationRepo;
    @Mock
    InscriptionRepo inscriptionRepo;
    @Mock
    CRM_Manara.CRM_Manara.service.EmailService emailService;
    @Mock
    VerificationTokenRepository verificationTokenRepository;
    @Mock
    ParentNotificationService parentNotificationService;
    @Mock
    AvatarService avatarService;
    @Mock
    AnimateurNotificationService animateurNotificationService;
    @Mock
    AdminNotificationService adminNotificationService;
    @Mock
    BillingService billingService;

    @InjectMocks
    parentService parentService;

    @Test
    void createNewParent_createsDisabledAccountAndNotifiesAdmins() {
        when(userRepo.existsByEmail("newparent@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepo.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 77L);
            return saved;
        });
        when(parentRepo.save(any(Parent.class))).thenAnswer(invocation -> {
            Parent saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 88L);
            return saved;
        });

        parentService.createNewParent("Roy", "Mia", "Adresse", "newparent@test.com", "secret123");

        verify(userRepo).save(any(User.class));
        verify(avatarService).assignDefaultAvatar(any(User.class), org.mockito.ArgumentMatchers.eq("Mia Roy"));
        verify(emailService).sendEmail(
                org.mockito.ArgumentMatchers.eq("newparent@test.com"),
                org.mockito.ArgumentMatchers.contains("en attente"),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(emailService).notifyAdminsOfParentSignup("Mia Roy", "newparent@test.com", "Formulaire");
    }

    @Test
    void inscrireEnfant_rejectsDuplicateRequestForSameActivity() {
        Parent parent = buildParent(10L, "parent@test.local");
        Enfant enfant = buildEnfant(21L, parent, true, "Lina");
        Activity activity = buildActivity(31L, 8);
        Animation animation = buildAnimation(41L, activity);

        Inscription existing = new Inscription(enfant, animation);
        existing.setStatusInscription(statusInscription.EN_ATTENTE);

        when(parentRepo.findByUserEmail("parent@test.local")).thenReturn(Optional.of(parent));
        when(billingService.hasActiveSubscription("parent@test.local")).thenReturn(true);
        when(billingService.hasAvailableChildSlot("parent@test.local", 10L, 21L)).thenReturn(true);
        when(enfantRepo.findByIdAndParentId(21L, 10L)).thenReturn(Optional.of(enfant));
        when(animationRepo.findById(41L)).thenReturn(Optional.of(animation));
        when(inscriptionRepo.findByEnfantIdAndActivityId(21L, 31L)).thenReturn(List.of(existing));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parentService.inscrireEnfant(21L, 41L, "parent@test.local")
        );

        assertEquals("Une demande existe déjà pour cet enfant sur cette activité.", exception.getMessage());
        verify(inscriptionRepo, never()).save(any(Inscription.class));
    }

    @Test
    void inscrireEnfant_rejectsWhenChildAgeIsOutsideAllowedRange() {
        Parent parent = buildParent(10L, "parent@test.local");
        Enfant enfant = buildEnfant(21L, parent, true, "Lina", java.sql.Date.valueOf("2019-04-02"));
        Activity activity = buildActivity(31L, 8);
        Animation animation = new Animation(
                AnimationRole.COACH,
                animationStatus.ACTIF,
                LocalDateTime.of(2026, 4, 2, 18, 0),
                LocalDateTime.of(2026, 4, 2, 19, 0)
        );
        animation.setActivity(activity);
        ReflectionTestUtils.setField(animation, "id", 41L);

        when(parentRepo.findByUserEmail("parent@test.local")).thenReturn(Optional.of(parent));
        when(billingService.hasActiveSubscription("parent@test.local")).thenReturn(true);
        when(billingService.hasAvailableChildSlot("parent@test.local", 10L, 21L)).thenReturn(true);
        when(enfantRepo.findByIdAndParentId(21L, 10L)).thenReturn(Optional.of(enfant));
        when(animationRepo.findById(41L)).thenReturn(Optional.of(animation));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> parentService.inscrireEnfant(21L, 41L, "parent@test.local")
        );

        assertEquals("L'age de Lina ne correspond pas a cette activite. Age requis: 8 a 12 ans.", exception.getMessage());
        verify(inscriptionRepo, never()).save(any(Inscription.class));
    }

    @Test
    void inscrireEnfant_rejectsWhenSubscriptionIsInactive() {
        Parent parent = buildParent(10L, "parent@test.local");
        Enfant enfant = buildEnfant(21L, parent, true, "Lina");
        when(parentRepo.findByUserEmail("parent@test.local")).thenReturn(Optional.of(parent));
        when(enfantRepo.findByIdAndParentId(21L, 10L)).thenReturn(Optional.of(enfant));
        when(billingService.hasActiveSubscription("parent@test.local")).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> parentService.inscrireEnfant(21L, 41L, "parent@test.local")
        );

        assertEquals("Un abonnement actif est requis pour inscrire un enfant à une activité.", exception.getMessage());
        verify(inscriptionRepo, never()).save(any(Inscription.class));
    }

    @Test
    void inscrireEnfant_rejectsWhenNoPaidChildSlotIsAvailable() {
        Parent parent = buildParent(10L, "parent@test.local");
        Enfant enfant = buildEnfant(21L, parent, true, "Lina");
        when(parentRepo.findByUserEmail("parent@test.local")).thenReturn(Optional.of(parent));
        when(enfantRepo.findByIdAndParentId(21L, 10L)).thenReturn(Optional.of(enfant));
        when(billingService.hasActiveSubscription("parent@test.local")).thenReturn(true);
        when(billingService.hasAvailableChildSlot("parent@test.local", 10L, 21L)).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> parentService.inscrireEnfant(21L, 41L, "parent@test.local")
        );

        assertEquals("Votre abonnement ne couvre pas encore cet enfant. Ajoutez une place enfant mensuelle pour l'inscrire.", exception.getMessage());
        verify(inscriptionRepo, never()).save(any(Inscription.class));
    }

    @Test
    void inscrireEnfant_createsRequestWhenSubscriptionIsActive() {
        Parent parent = buildParent(10L, "parent@test.local");
        Enfant enfant = buildEnfant(21L, parent, true, "Lina");
        Activity activity = buildActivity(31L, 8);
        Animation animation = buildAnimation(41L, activity);
        Inscription saved = new Inscription(enfant, animation);
        ReflectionTestUtils.setField(saved, "id", 99L);

        when(parentRepo.findByUserEmail("parent@test.local")).thenReturn(Optional.of(parent));
        when(billingService.hasActiveSubscription("parent@test.local")).thenReturn(true);
        when(billingService.hasAvailableChildSlot("parent@test.local", 10L, 21L)).thenReturn(true);
        when(enfantRepo.findByIdAndParentId(21L, 10L)).thenReturn(Optional.of(enfant));
        when(animationRepo.findById(41L)).thenReturn(Optional.of(animation));
        when(inscriptionRepo.findByEnfantIdAndActivityId(21L, 31L)).thenReturn(List.of());
        when(inscriptionRepo.findByEnfantIdAndAnimationId(21L, 41L)).thenReturn(Optional.empty());
        when(inscriptionRepo.save(any(Inscription.class))).thenReturn(saved);
        when(inscriptionRepo.findByAnimationId(41L)).thenReturn(List.of());

        Inscription result = parentService.inscrireEnfant(21L, 41L, "parent@test.local");

        assertEquals(99L, result.getId());
        verify(inscriptionRepo).save(any(Inscription.class));
    }

    @Test
    void getAnimationCapacitySnapshot_computesWaitlistAndFillRate() {
        Parent parent = buildParent(10L, "parent@test.local");
        Enfant enfant = buildEnfant(21L, parent, true, "Lina");
        Activity activity = buildActivity(31L, 2);
        Animation animation = buildAnimation(41L, activity);

        Inscription approvedOne = new Inscription(enfant, animation);
        approvedOne.setStatusInscription(statusInscription.APPROUVEE);
        Inscription approvedTwo = new Inscription(enfant, animation);
        approvedTwo.setStatusInscription(statusInscription.ACTIF);
        Inscription pendingOne = new Inscription(enfant, animation);
        pendingOne.setStatusInscription(statusInscription.EN_ATTENTE);

        when(inscriptionRepo.findByAnimationId(41L)).thenReturn(List.of(approvedOne, approvedTwo, pendingOne));

        Map<String, Object> snapshot = parentService.getAnimationCapacitySnapshot(animation);

        assertEquals(2, snapshot.get("approved"));
        assertEquals(1, snapshot.get("pending"));
        assertEquals(0, snapshot.get("remaining"));
        assertEquals(1, snapshot.get("waitlist"));
        assertEquals(100, snapshot.get("fillRate"));
    }

    private Parent buildParent(Long id, String email) {
        Parent parent = new Parent("Chenier", "Nadia", "Adresse");
        User user = new User(email, "hash", SecurityRole.ROLE_PARENT, true);
        parent.SetUser(user);
        ReflectionTestUtils.setField(parent, "id", id);
        return parent;
    }

    private Enfant buildEnfant(Long id, Parent parent, boolean active, String prenom) {
        return buildEnfant(id, parent, active, prenom, java.sql.Date.valueOf("2015-05-10"));
    }

    private Enfant buildEnfant(Long id, Parent parent, boolean active, String prenom, Date birthDate) {
        Enfant enfant = new Enfant("Chenier", prenom, birthDate);
        enfant.setParent(parent);
        enfant.setActive(active);
        ReflectionTestUtils.setField(enfant, "id", id);
        return enfant;
    }

    private Activity buildActivity(Long id, int capacity) {
        Activity activity = new Activity("Robotique", "Desc", 8, 12, capacity, status.OUVERTE, typeActivity.TUTORAT);
        ReflectionTestUtils.setField(activity, "id", id);
        return activity;
    }

    private Animation buildAnimation(Long id, Activity activity) {
        Animation animation = new Animation(AnimationRole.COACH, animationStatus.ACTIF, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(1));
        animation.setActivity(activity);
        ReflectionTestUtils.setField(animation, "id", id);
        return animation;
    }
}
