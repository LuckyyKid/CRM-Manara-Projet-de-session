package CRM_Manara.CRM_Manara.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SubscriptionStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.ParentSubscription;
import CRM_Manara.CRM_Manara.Model.Entity.ParentSubscriptionChild;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Repository.EnfantRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.ParentSubscriptionChildRepo;
import CRM_Manara.CRM_Manara.Repository.ParentSubscriptionRepo;
import CRM_Manara.CRM_Manara.dto.BillingChildCoverageDto;
import CRM_Manara.CRM_Manara.dto.SubscriptionDto;
import CRM_Manara.CRM_Manara.service.AdminNotificationService;
import CRM_Manara.CRM_Manara.service.BillingService;
import CRM_Manara.CRM_Manara.service.EmailService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    ParentRepo parentRepo;
    @Mock
    ParentSubscriptionRepo subscriptionRepo;
    @Mock
    ParentSubscriptionChildRepo subscriptionChildRepo;
    @Mock
    EnfantRepo enfantRepo;
    @Mock
    EmailService emailService;
    @Mock
    AdminNotificationService adminNotificationService;

    @Test
    void getSubscriptionStatusCreatesInactiveSubscriptionWhenMissing() {
        Parent parent = buildParent();
        when(parentRepo.findByUserEmail("parent@test.com")).thenReturn(Optional.of(parent));
        when(subscriptionRepo.findByParentId(12L)).thenReturn(Optional.empty());
        when(subscriptionRepo.save(any(ParentSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillingService service = service();

        SubscriptionDto dto = service.getSubscriptionStatus("parent@test.com");

        assertEquals("INACTIVE", dto.status());
        assertFalse(dto.active());
    }

    @Test
    void hasActiveSubscriptionReturnsTrueOnlyForActiveStatus() {
        ParentSubscription subscription = new ParentSubscription(buildParent(), buildParent().getUser());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        when(subscriptionRepo.findByUserEmail("parent@test.com")).thenReturn(Optional.of(subscription));

        BillingService service = service();

        assertTrue(service.hasActiveSubscription("parent@test.com"));
    }

    @Test
    void hasActiveSubscriptionReturnsFalseWhenMissing() {
        when(subscriptionRepo.findByUserEmail("parent@test.com")).thenReturn(Optional.empty());
        BillingService service = service();

        assertFalse(service.hasActiveSubscription("parent@test.com"));
    }

    @Test
    void handleSubscriptionUpdatedMarksActiveFromStripeStatus() {
        ParentSubscription subscription = new ParentSubscription(buildParent(), buildParent().getUser());
        subscription.setStripeSubscriptionId("sub_test");
        when(subscriptionRepo.findByStripeSubscriptionId("sub_test")).thenReturn(Optional.of(subscription));

        com.stripe.model.Subscription stripeSubscription = new com.stripe.model.Subscription();
        stripeSubscription.setId("sub_test");
        stripeSubscription.setCustomer("cus_test");
        stripeSubscription.setStatus("active");
        stripeSubscription.setCancelAtPeriodEnd(false);

        BillingService service = service();
        service.handleSubscriptionUpdated(stripeSubscription);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        assertFalse(subscription.isCancelAtPeriodEnd());
    }

    @Test
    void handleSubscriptionUpdatedMarksPastDueFromStripeStatus() {
        ParentSubscription subscription = new ParentSubscription(buildParent(), buildParent().getUser());
        subscription.setStripeSubscriptionId("sub_test");
        when(subscriptionRepo.findByStripeSubscriptionId("sub_test")).thenReturn(Optional.of(subscription));

        com.stripe.model.Subscription stripeSubscription = new com.stripe.model.Subscription();
        stripeSubscription.setId("sub_test");
        stripeSubscription.setCustomer("cus_test");
        stripeSubscription.setStatus("past_due");
        stripeSubscription.setCancelAtPeriodEnd(true);

        BillingService service = service();
        service.handleSubscriptionUpdated(stripeSubscription);

        assertEquals(SubscriptionStatus.PAST_DUE, subscription.getStatus());
        assertTrue(subscription.isCancelAtPeriodEnd());
    }

    @Test
    void hasAvailableChildSlotAllowsExistingCoveredChild() {
        ParentSubscription subscription = new ParentSubscription(buildParent(), buildParent().getUser());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        ReflectionTestUtils.setField(subscription, "id", 90L);
        when(subscriptionRepo.findByUserEmail("parent@test.com")).thenReturn(Optional.of(subscription));
        when(subscriptionChildRepo.existsBySubscriptionIdAndEnfantId(90L, 44L))
                .thenReturn(true);

        assertTrue(service().hasAvailableChildSlot("parent@test.com", 12L, 44L));
    }

    @Test
    void hasAvailableChildSlotRejectsNewChildWhenPaidSlotsAreFull() {
        ParentSubscription subscription = new ParentSubscription(buildParent(), buildParent().getUser());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        ReflectionTestUtils.setField(subscription, "id", 90L);
        when(subscriptionRepo.findByUserEmail("parent@test.com")).thenReturn(Optional.of(subscription));
        when(subscriptionChildRepo.existsBySubscriptionIdAndEnfantId(90L, 55L))
                .thenReturn(false);

        assertFalse(service().hasAvailableChildSlot("parent@test.com", 12L, 55L));
    }

    @Test
    void listChildCoverageMarksCoveredChildren() {
        ParentSubscription subscription = new ParentSubscription(buildParent(), buildParent().getUser());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        ReflectionTestUtils.setField(subscription, "id", 90L);
        Enfant covered = buildChild(44L, "Lina");
        Enfant uncovered = buildChild(55L, "Noah");
        when(parentRepo.findByUserEmail("parent@test.com")).thenReturn(Optional.of(buildParent()));
        when(subscriptionRepo.findByParentId(12L)).thenReturn(Optional.of(subscription));
        when(enfantRepo.findByParentId(12L)).thenReturn(List.of(covered, uncovered));
        when(subscriptionChildRepo.findBySubscriptionIdDetailed(90L))
                .thenReturn(List.of(new ParentSubscriptionChild(subscription, covered)));

        List<BillingChildCoverageDto> result = service().listChildCoverage("parent@test.com");

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(child -> child.enfantId().equals(44L) && child.covered()));
        assertTrue(result.stream().anyMatch(child -> child.enfantId().equals(55L) && !child.covered()));
    }

    @Test
    void handleInvoicePaidSendsConfirmationEmailAndAdminNotificationOnFirstActivation() {
        ParentSubscription subscription = new ParentSubscription(buildParent(), buildParent().getUser());
        subscription.setStatus(SubscriptionStatus.CHECKOUT_PENDING);
        ReflectionTestUtils.setField(subscription, "id", 90L);
        when(subscriptionRepo.findByStripeCustomerId("cus_test")).thenReturn(Optional.of(subscription));

        com.stripe.model.Invoice invoice = new com.stripe.model.Invoice();
        invoice.setCustomer("cus_test");

        service().handleInvoicePaid(invoice);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        verify(emailService).sendSubscriptionActivatedEmail("parent@test.com");
        verify(adminNotificationService).create(eq("BILLING"), eq("SUBSCRIPTION_ACTIVATED"), anyString());
    }

    @Test
    void handleInvoicePaidSendsEmailButNoAdminNotificationOnRenewal() {
        ParentSubscription subscription = new ParentSubscription(buildParent(), buildParent().getUser());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        ReflectionTestUtils.setField(subscription, "id", 90L);
        when(subscriptionRepo.findByStripeCustomerId("cus_test")).thenReturn(Optional.of(subscription));

        com.stripe.model.Invoice invoice = new com.stripe.model.Invoice();
        invoice.setCustomer("cus_test");

        service().handleInvoicePaid(invoice);

        assertEquals(SubscriptionStatus.ACTIVE, subscription.getStatus());
        verify(emailService).sendSubscriptionActivatedEmail("parent@test.com");
        verify(adminNotificationService, org.mockito.Mockito.never()).create(anyString(), anyString(), anyString());
    }

    @Test
    void handleInvoicePaymentFailedSendsAlertEmail() {
        ParentSubscription subscription = new ParentSubscription(buildParent(), buildParent().getUser());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        ReflectionTestUtils.setField(subscription, "id", 90L);
        when(subscriptionRepo.findByStripeCustomerId("cus_test")).thenReturn(Optional.of(subscription));

        com.stripe.model.Invoice invoice = new com.stripe.model.Invoice();
        invoice.setCustomer("cus_test");

        service().handleInvoicePaymentFailed(invoice);

        assertEquals(SubscriptionStatus.PAST_DUE, subscription.getStatus());
        verify(emailService).sendPaymentFailedEmail("parent@test.com");
    }

    private BillingService service() {
        return new BillingService(parentRepo, subscriptionRepo, subscriptionChildRepo, enfantRepo,
                emailService, adminNotificationService, "", "", "price_first", "price_extra", 6000L, 4000L, "http://localhost:4200");
    }

    private Parent buildParent() {
        Parent parent = new Parent("Roy", "Mia", "Adresse");
        User user = new User("parent@test.com", "hash", SecurityRole.ROLE_PARENT, true);
        ReflectionTestUtils.setField(parent, "id", 12L);
        ReflectionTestUtils.setField(user, "id", 34L);
        parent.SetUser(user);
        return parent;
    }

    private Enfant buildChild(Long id, String prenom) {
        Enfant enfant = new Enfant("Roy", prenom, java.sql.Date.valueOf("2015-05-10"));
        enfant.setActive(true);
        ReflectionTestUtils.setField(enfant, "id", id);
        return enfant;
    }
}
