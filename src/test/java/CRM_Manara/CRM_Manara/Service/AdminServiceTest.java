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
import CRM_Manara.CRM_Manara.Model.Entity.Service.AdminService;
import CRM_Manara.CRM_Manara.Model.Entity.Service.ParentNotificationService;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.ActivityRepo;
import CRM_Manara.CRM_Manara.Repository.AdminRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.AnimationRepo;
import CRM_Manara.CRM_Manara.Repository.EnfantRepo;
import CRM_Manara.CRM_Manara.Repository.InscriptionRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    ActivityRepo activityRepo;
    @Mock
    AnimationRepo animationRepo;
    @Mock
    AnimateurRepo animateurRepo;
    @Mock
    AdminRepo adminRepo;
    @Mock
    ParentRepo parentRepo;
    @Mock
    EnfantRepo enfantRepo;
    @Mock
    UserRepo userRepo;
    @Mock
    InscriptionRepo inscriptionRepo;
    @Mock
    PasswordEncoder passwordEncoder;
    @Mock
    CRM_Manara.CRM_Manara.Model.Entity.Service.EmailService emailService;
    @Mock
    ParentNotificationService parentNotificationService;

    @InjectMocks
    AdminService adminService;

    @Test
    void approveInscription_rejectsWhenNoPlacesRemain() {
        Parent parent = buildParent(10L, "parent@test.local");
        Enfant enfant = buildEnfant(20L, parent);
        Activity activity = buildActivity(30L, 1);
        Animation animation = buildAnimation(40L, activity);

        Inscription pending = new Inscription(enfant, animation);
        ReflectionTestUtils.setField(pending, "id", 50L);
        pending.setStatusInscription(statusInscription.EN_ATTENTE);

        Inscription approvedExisting = new Inscription(enfant, animation);
        approvedExisting.setStatusInscription(statusInscription.APPROUVEE);

        when(inscriptionRepo.findById(50L)).thenReturn(Optional.of(pending));
        when(inscriptionRepo.findByAnimationId(40L)).thenReturn(List.of(approvedExisting));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adminService.approveInscription(50L)
        );

        assertEquals("Plus de places disponibles sur cette session. La demande reste en attente.", exception.getMessage());
        verify(inscriptionRepo, never()).save(any(Inscription.class));
    }

    @Test
    void approveInscription_sendsReceiptAndCreatesParentNotification() {
        Parent parent = buildParent(10L, "parent@test.local");
        Enfant enfant = buildEnfant(20L, parent);
        Activity activity = buildActivity(30L, 3);
        Animation animation = buildAnimation(40L, activity);

        Inscription pending = new Inscription(enfant, animation);
        ReflectionTestUtils.setField(pending, "id", 50L);
        pending.setStatusInscription(statusInscription.EN_ATTENTE);

        when(inscriptionRepo.findById(50L)).thenReturn(Optional.of(pending));
        when(inscriptionRepo.findByAnimationId(40L)).thenReturn(List.of());
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Inscription saved = adminService.approveInscription(50L);

        assertEquals(statusInscription.APPROUVEE, saved.getStatusInscription());
        verify(emailService).sendInscriptionConfirmation("parent@test.local", saved);
        verify(parentNotificationService).createForParent(
                any(Parent.class),
                any(String.class),
                any(String.class),
                any(String.class)
        );
    }

    private Parent buildParent(Long id, String email) {
        Parent parent = new Parent("Manara", "Parent", "Adresse");
        User user = new User(email, "hash", SecurityRole.ROLE_PARENT, true);
        parent.SetUser(user);
        ReflectionTestUtils.setField(parent, "id", id);
        return parent;
    }

    private Enfant buildEnfant(Long id, Parent parent) {
        Enfant enfant = new Enfant("Manara", "Yasmine", new Date());
        enfant.setParent(parent);
        enfant.setActive(true);
        ReflectionTestUtils.setField(enfant, "id", id);
        return enfant;
    }

    private Activity buildActivity(Long id, int capacity) {
        Activity activity = new Activity("Atelier", "Desc", 6, 12, capacity, status.OUVERTE, typeActivity.ART);
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
