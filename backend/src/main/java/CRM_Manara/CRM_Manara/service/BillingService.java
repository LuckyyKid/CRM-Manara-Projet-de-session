package CRM_Manara.CRM_Manara.service;

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
import CRM_Manara.CRM_Manara.dto.AdminSubscriptionRowDto;
import CRM_Manara.CRM_Manara.dto.BillingChildCoverageDto;
import CRM_Manara.CRM_Manara.dto.CheckoutSessionDto;
import CRM_Manara.CRM_Manara.dto.SubscriptionDto;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.StripeObject;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final ParentRepo parentRepo;
    private final ParentSubscriptionRepo subscriptionRepo;
    private final ParentSubscriptionChildRepo subscriptionChildRepo;
    private final EnfantRepo enfantRepo;
    private final EmailService emailService;
    private final AdminNotificationService adminNotificationService;
    private final String stripeSecretKey;
    private final String stripeWebhookSecret;
    private final String stripeFirstChildPriceId;
    private final String stripeAdditionalChildPriceId;
    private final long defaultFirstChildMonthlyAmountCents;
    private final long defaultAdditionalChildMonthlyAmountCents;
    private final String frontendBaseUrl;

    public BillingService(ParentRepo parentRepo,
                          ParentSubscriptionRepo subscriptionRepo,
                          ParentSubscriptionChildRepo subscriptionChildRepo,
                          EnfantRepo enfantRepo,
                          EmailService emailService,
                          AdminNotificationService adminNotificationService,
                          @Value("${stripe.secret-key:}") String stripeSecretKey,
                          @Value("${stripe.webhook-secret:}") String stripeWebhookSecret,
                          @Value("${stripe.first-child-price-id:${stripe.monthly-price-id:}}") String stripeFirstChildPriceId,
                          @Value("${stripe.additional-child-price-id:}") String stripeAdditionalChildPriceId,
                          @Value("${stripe.first-child-monthly-amount-cents:6000}") long defaultFirstChildMonthlyAmountCents,
                          @Value("${stripe.additional-child-monthly-amount-cents:4000}") long defaultAdditionalChildMonthlyAmountCents,
                          @Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.parentRepo = parentRepo;
        this.subscriptionRepo = subscriptionRepo;
        this.subscriptionChildRepo = subscriptionChildRepo;
        this.enfantRepo = enfantRepo;
        this.emailService = emailService;
        this.adminNotificationService = adminNotificationService;
        this.stripeSecretKey = stripeSecretKey;
        this.stripeWebhookSecret = stripeWebhookSecret;
        this.stripeFirstChildPriceId = stripeFirstChildPriceId;
        this.stripeAdditionalChildPriceId = stripeAdditionalChildPriceId;
        this.defaultFirstChildMonthlyAmountCents = defaultFirstChildMonthlyAmountCents;
        this.defaultAdditionalChildMonthlyAmountCents = defaultAdditionalChildMonthlyAmountCents;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @PostConstruct
    void logStripeConfiguration() {
        boolean keyOk = stripeSecretKey != null && !stripeSecretKey.isBlank();
        boolean webhookOk = stripeWebhookSecret != null && !stripeWebhookSecret.isBlank();
        boolean firstPriceOk = stripeFirstChildPriceId != null && !stripeFirstChildPriceId.isBlank();
        boolean extraPriceOk = stripeAdditionalChildPriceId != null && !stripeAdditionalChildPriceId.isBlank();
        if (keyOk && webhookOk && firstPriceOk && extraPriceOk) {
            log.info("STRIPE: configuration complete. Checkout, webhooks et portail actifs.");
        } else {
            if (!keyOk)       log.warn("STRIPE: STRIPE_SECRET_KEY manquant — checkout et portail desactives.");
            if (!webhookOk)   log.warn("STRIPE: STRIPE_WEBHOOK_SECRET manquant — webhooks Stripe rejetes.");
            if (!firstPriceOk) log.warn("STRIPE: STRIPE_FIRST_CHILD_PRICE_ID manquant — checkout desactive.");
            if (!extraPriceOk) log.warn("STRIPE: STRIPE_ADDITIONAL_CHILD_PRICE_ID manquant — checkout multi-enfants desactive.");
        }
    }

    @Transactional
    public ParentSubscription getOrCreateSubscriptionForParent(String parentEmail) {
        Parent parent = requireParent(parentEmail);
        return subscriptionRepo.findByParentId(parent.getId())
                .orElseGet(() -> subscriptionRepo.save(new ParentSubscription(parent, parent.getUser())));
    }

    @Transactional
    public SubscriptionDto getSubscriptionStatus(String parentEmail) {
        return toSubscriptionDto(getOrCreateSubscriptionForParent(parentEmail));
    }

    @Transactional
    public CheckoutSessionDto createCheckoutSession(String parentEmail) {
        return createCheckoutSession(parentEmail, 1);
    }

    @Transactional
    public CheckoutSessionDto createCheckoutSession(String parentEmail, int requestedCoveredChildrenCount) {
        int coveredChildrenCount = normalizeCoveredChildrenCount(requestedCoveredChildrenCount);
        ParentSubscription subscription = getOrCreateSubscriptionForParent(parentEmail);
        if (subscription.getStatus() == SubscriptionStatus.ACTIVE
                && subscription.getStripeCustomerId() != null
                && !subscription.getStripeCustomerId().isBlank()
                && subscription.getStripeSubscriptionId() != null
                && !subscription.getStripeSubscriptionId().isBlank()) {
            return createBillingPortalSession(subscription);
        }

        requireStripeCheckoutConfig();
        Parent parent = subscription.getParent();
        User user = subscription.getUser();

        try {
            RequestOptions requestOptions = requestOptions();
            refreshStripePriceAmounts(subscription, requestOptions);
            String customerId = subscription.getStripeCustomerId();
            if (customerId == null || customerId.isBlank()) {
                CustomerCreateParams customerParams = CustomerCreateParams.builder()
                        .setEmail(user.getEmail())
                        .setName(displayName(parent))
                        .putMetadata("parentId", String.valueOf(parent.getId()))
                        .putMetadata("userId", String.valueOf(user.getId()))
                        .build();
                Customer customer = Customer.create(customerParams, requestOptions);
                customerId = customer.getId();
                subscription.setStripeCustomerId(customerId);
            }

            SessionCreateParams.Builder sessionParams = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(frontendUrl("/parent/billing?success=true"))
                    .setCancelUrl(frontendUrl("/parent/billing?canceled=true"))
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPrice(stripeFirstChildPriceId)
                                    .setQuantity(1L)
                                    .build()
                    )
                    .putMetadata("parentId", String.valueOf(parent.getId()))
                    .putMetadata("userId", String.valueOf(user.getId()))
                    .putMetadata("coveredChildrenCount", String.valueOf(coveredChildrenCount));
            if (coveredChildrenCount > 1) {
                sessionParams.addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(stripeAdditionalChildPriceId)
                                .setQuantity((long) coveredChildrenCount - 1L)
                                .build()
                );
            }
            SessionCreateParams params = sessionParams.build();
            Session session = Session.create(params, requestOptions);
            subscription.setStripeCheckoutSessionId(session.getId());
            subscription.setStripePriceId(stripeFirstChildPriceId);
            subscription.setStripeAdditionalPriceId(stripeAdditionalChildPriceId);
            subscription.setPendingCoveredChildrenCount(coveredChildrenCount);
            subscription.setStatus(SubscriptionStatus.CHECKOUT_PENDING);
            subscriptionRepo.save(subscription);
            return new CheckoutSessionDto(session.getUrl());
        } catch (StripeException exception) {
            throw new IllegalStateException("Impossible de creer la session Stripe Checkout.", exception);
        }
    }

    @Transactional
    public void handleStripeWebhook(String payload, String signatureHeader) {
        if (stripeWebhookSecret == null || stripeWebhookSecret.isBlank()) {
            throw new IllegalStateException("STRIPE_WEBHOOK_SECRET est requis pour verifier les webhooks Stripe.");
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeWebhookSecret);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Signature webhook Stripe invalide.", exception);
        }

        StripeObject object = event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> new IllegalArgumentException("Payload webhook Stripe non supporte."));

        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutSessionCompleted((Session) object);
            case "customer.subscription.created", "customer.subscription.updated" ->
                    handleSubscriptionUpdated((com.stripe.model.Subscription) object);
            case "customer.subscription.deleted" ->
                    handleSubscriptionDeleted((com.stripe.model.Subscription) object);
            case "invoice.paid" -> handleInvoicePaid((Invoice) object);
            case "invoice.payment_failed" -> handleInvoicePaymentFailed((Invoice) object);
            default -> {
            }
        }
    }

    @Transactional
    public void handleCheckoutSessionCompleted(Session session) {
        subscriptionRepo.findByStripeCheckoutSessionId(session.getId())
                .or(() -> subscriptionRepo.findByStripeCustomerId(session.getCustomer()))
                .ifPresent(subscription -> {
                    subscription.setStripeCustomerId(session.getCustomer());
                    subscription.setStripeSubscriptionId(session.getSubscription());
                    subscription.setCoveredChildrenCount(subscription.getPendingCoveredChildrenCount());
                    subscriptionRepo.save(subscription);
                });
    }

    @Transactional
    public void handleSubscriptionUpdated(com.stripe.model.Subscription stripeSubscription) {
        subscriptionRepo.findByStripeSubscriptionId(stripeSubscription.getId())
                .or(() -> subscriptionRepo.findByStripeCustomerId(stripeSubscription.getCustomer()))
                .ifPresent(subscription -> {
                    subscription.setStripeCustomerId(stripeSubscription.getCustomer());
                    subscription.setStripeSubscriptionId(stripeSubscription.getId());
                    subscription.setStatus(toLocalStatus(stripeSubscription.getStatus()));
                    subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripeSubscription.getCancelAtPeriodEnd()));
                    subscription.setCoveredChildrenCount(resolveCoveredChildrenCount(stripeSubscription, subscription.getCoveredChildrenCount()));
                    trimCoveredChildrenToLimit(subscription);
                    subscriptionRepo.save(subscription);
                });
    }

    @Transactional
    public void handleSubscriptionDeleted(com.stripe.model.Subscription stripeSubscription) {
        subscriptionRepo.findByStripeSubscriptionId(stripeSubscription.getId())
                .or(() -> subscriptionRepo.findByStripeCustomerId(stripeSubscription.getCustomer()))
                .ifPresent(subscription -> {
                    subscription.setStatus(SubscriptionStatus.CANCELED);
                    subscription.setCancelAtPeriodEnd(true);
                    subscriptionRepo.save(subscription);
                });
    }

    @Transactional
    public void handleInvoicePaid(Invoice invoice) {
        findByInvoice(invoice).ifPresent(subscription -> {
            String subscriptionId = extractInvoiceSubscriptionId(invoice);
            if (subscriptionId != null && !subscriptionId.isBlank()) {
                subscription.setStripeSubscriptionId(subscriptionId);
            }
            boolean firstActivation = subscription.getStatus() == SubscriptionStatus.CHECKOUT_PENDING;
            if (firstActivation) {
                subscription.setCoveredChildrenCount(subscription.getPendingCoveredChildrenCount());
            }
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setCurrentPeriodStart(toInstant(invoice.getPeriodStart()));
            subscription.setCurrentPeriodEnd(toInstant(invoice.getPeriodEnd()));
            subscriptionRepo.save(subscription);

            String parentEmail = subscription.getUser().getEmail();
            emailService.sendSubscriptionActivatedEmail(parentEmail);
            if (firstActivation) {
                adminNotificationService.create(
                        "BILLING",
                        "SUBSCRIPTION_ACTIVATED",
                        displayName(subscription.getParent()) + " (" + parentEmail + ") a active son abonnement mensuel."
                );
            }
        });
    }

    @Transactional
    public void handleInvoicePaymentFailed(Invoice invoice) {
        findByInvoice(invoice).ifPresent(subscription -> {
            String subscriptionId = extractInvoiceSubscriptionId(invoice);
            if (subscriptionId != null && !subscriptionId.isBlank()) {
                subscription.setStripeSubscriptionId(subscriptionId);
            }
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
            subscription.setCurrentPeriodStart(toInstant(invoice.getPeriodStart()));
            subscription.setCurrentPeriodEnd(toInstant(invoice.getPeriodEnd()));
            subscriptionRepo.save(subscription);

            emailService.sendPaymentFailedEmail(subscription.getUser().getEmail());
        });
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(String parentEmail) {
        return subscriptionRepo.findByUserEmail(parentEmail)
                .map(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean hasAvailableChildSlot(String parentEmail, Long parentId, Long enfantId) {
        return subscriptionRepo.findByUserEmail(parentEmail)
                .filter(subscription -> subscription.getStatus() == SubscriptionStatus.ACTIVE)
                .filter(subscription -> subscription.getParent().getId().equals(parentId))
                .map(subscription -> subscriptionChildRepo.existsBySubscriptionIdAndEnfantId(subscription.getId(), enfantId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<BillingChildCoverageDto> listChildCoverage(String parentEmail) {
        Parent parent = requireParent(parentEmail);
        ParentSubscription subscription = getOrCreateSubscriptionForParent(parentEmail);
        Set<Long> coveredChildIds = coveredChildIds(subscription);
        return enfantRepo.findByParentId(parent.getId()).stream()
                .map(enfant -> new BillingChildCoverageDto(
                        enfant.getId(),
                        enfant.getNom(),
                        enfant.getPrenom(),
                        enfant.isActive(),
                        coveredChildIds.contains(enfant.getId())
                ))
                .toList();
    }

    @Transactional
    public List<BillingChildCoverageDto> updateCoveredChildren(String parentEmail, List<Long> enfantIds) {
        Parent parent = requireParent(parentEmail);
        ParentSubscription subscription = getOrCreateSubscriptionForParent(parentEmail);
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Un abonnement actif est requis pour choisir les enfants couverts.");
        }

        List<Long> requestedIds = enfantIds == null ? List.of() : enfantIds.stream().distinct().toList();
        if (requestedIds.size() > subscription.getCoveredChildrenCount()) {
            throw new IllegalArgumentException("Votre forfait couvre seulement " + subscription.getCoveredChildrenCount() + " enfant(s).");
        }

        List<Enfant> children = enfantRepo.findByParentId(parent.getId());
        Set<Long> validChildIds = children.stream().map(Enfant::getId).collect(java.util.stream.Collectors.toSet());
        if (!validChildIds.containsAll(requestedIds)) {
            throw new IllegalArgumentException("Un ou plusieurs enfants ne font pas partie de votre compte.");
        }

        subscriptionChildRepo.deleteBySubscriptionId(subscription.getId());
        List<ParentSubscriptionChild> coverages = children.stream()
                .filter(enfant -> requestedIds.contains(enfant.getId()))
                .map(enfant -> new ParentSubscriptionChild(subscription, enfant))
                .toList();
        subscriptionChildRepo.saveAll(coverages);
        return listChildCoverage(parentEmail);
    }

    @Transactional(readOnly = true)
    public List<AdminSubscriptionRowDto> listAdminSubscriptions() {
        Map<Long, ParentSubscription> subscriptionByParentId = subscriptionRepo.findAllDetailed().stream()
                .collect(Collectors.toMap(sub -> sub.getParent().getId(), sub -> sub));
        return parentRepo.findAllWithUserAndEnfants().stream()
                .map(parent -> {
                    ParentSubscription sub = subscriptionByParentId.get(parent.getId());
                    if (sub == null) {
                        return new AdminSubscriptionRowDto(
                                parent.getId(),
                                displayName(parent),
                                parent.getUser() != null ? parent.getUser().getEmail() : "",
                                SubscriptionStatus.INACTIVE.name(),
                                false,
                                null,
                                false,
                                0,
                                List.of()
                        );
                    }
                    return new AdminSubscriptionRowDto(
                            sub.getParent().getId(),
                            displayName(sub.getParent()),
                            sub.getUser().getEmail(),
                            sub.getStatus().name(),
                            sub.getStatus() == SubscriptionStatus.ACTIVE,
                            sub.getCurrentPeriodEnd(),
                            sub.isCancelAtPeriodEnd(),
                            sub.getCoveredChildrenCount(),
                            subscriptionChildRepo.findBySubscriptionIdDetailed(sub.getId()).stream()
                                    .map(coverage -> displayName(coverage.getEnfant()))
                                    .toList()
                    );
                })
                .toList();
    }

    public SubscriptionDto toSubscriptionDto(ParentSubscription subscription) {
        return new SubscriptionDto(
                subscription.getStatus().name(),
                subscription.getStatus() == SubscriptionStatus.ACTIVE,
                subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getProvider().name(),
                subscription.getCoveredChildrenCount(),
                subscription.getPendingCoveredChildrenCount(),
                effectiveFirstChildAmount(subscription),
                effectiveAdditionalChildAmount(subscription)
        );
    }

    void requireStripeCheckoutConfig() {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY est requis pour lancer Checkout.");
        }
        if (stripeFirstChildPriceId == null || stripeFirstChildPriceId.isBlank()) {
            throw new IllegalStateException("STRIPE_FIRST_CHILD_PRICE_ID est requis pour lancer Checkout.");
        }
        if (stripeAdditionalChildPriceId == null || stripeAdditionalChildPriceId.isBlank()) {
            throw new IllegalStateException("STRIPE_ADDITIONAL_CHILD_PRICE_ID est requis pour lancer Checkout.");
        }
    }

    private int resolveCoveredChildrenCount(com.stripe.model.Subscription stripeSubscription, int fallback) {
        if (stripeSubscription.getItems() == null || stripeSubscription.getItems().getData() == null) {
            return Math.max(1, fallback);
        }
        int count = 0;
        for (SubscriptionItem item : stripeSubscription.getItems().getData()) {
            if (item.getPrice() == null || item.getPrice().getId() == null) {
                continue;
            }
            long quantity = item.getQuantity() == null ? 1L : item.getQuantity();
            String priceId = item.getPrice().getId();
            if (priceId.equals(stripeFirstChildPriceId) || priceId.equals(stripeAdditionalChildPriceId)) {
                count += (int) quantity;
            }
        }
        return count > 0 ? count : Math.max(1, fallback);
    }

    private int normalizeCoveredChildrenCount(int coveredChildrenCount) {
        return Math.max(1, Math.min(20, coveredChildrenCount));
    }

    private CheckoutSessionDto createBillingPortalSession(ParentSubscription subscription) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new IllegalStateException("STRIPE_SECRET_KEY est requis pour ouvrir le portail Stripe.");
        }
        try {
            com.stripe.param.billingportal.SessionCreateParams params =
                    com.stripe.param.billingportal.SessionCreateParams.builder()
                            .setCustomer(subscription.getStripeCustomerId())
                            .setReturnUrl(frontendUrl("/parent/billing"))
                            .build();
            com.stripe.model.billingportal.Session session =
                    com.stripe.model.billingportal.Session.create(params, requestOptions());
            return new CheckoutSessionDto(session.getUrl());
        } catch (StripeException exception) {
            throw new IllegalStateException("Impossible d'ouvrir le portail client Stripe.", exception);
        }
    }

    private void refreshStripePriceAmounts(ParentSubscription subscription, RequestOptions requestOptions) throws StripeException {
        Price firstChildPrice = Price.retrieve(stripeFirstChildPriceId, requestOptions);
        Price additionalChildPrice = Price.retrieve(stripeAdditionalChildPriceId, requestOptions);
        if (firstChildPrice.getUnitAmount() != null) {
            subscription.setFirstChildMonthlyAmountCents(firstChildPrice.getUnitAmount());
        }
        if (additionalChildPrice.getUnitAmount() != null) {
            subscription.setAdditionalChildMonthlyAmountCents(additionalChildPrice.getUnitAmount());
        }
    }

    private Set<Long> coveredChildIds(ParentSubscription subscription) {
        return subscriptionChildRepo.findBySubscriptionIdDetailed(subscription.getId()).stream()
                .map(coverage -> coverage.getEnfant().getId())
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    private void trimCoveredChildrenToLimit(ParentSubscription subscription) {
        List<ParentSubscriptionChild> coverages = subscriptionChildRepo.findBySubscriptionIdDetailed(subscription.getId());
        if (coverages.size() <= subscription.getCoveredChildrenCount()) {
            return;
        }
        List<Long> keptIds = coverages.stream()
                .limit(subscription.getCoveredChildrenCount())
                .map(coverage -> coverage.getEnfant().getId())
                .toList();
        List<String> removedNames = coverages.stream()
                .skip(subscription.getCoveredChildrenCount())
                .map(coverage -> displayName(coverage.getEnfant()))
                .toList();
        if (keptIds.isEmpty()) {
            subscriptionChildRepo.deleteBySubscriptionId(subscription.getId());
        } else {
            subscriptionChildRepo.deleteMissingChildren(subscription.getId(), keptIds);
        }
        emailService.sendCoveredChildrenReducedEmail(subscription.getUser().getEmail(), removedNames);
    }

    private long effectiveFirstChildAmount(ParentSubscription subscription) {
        return subscription.getFirstChildMonthlyAmountCents() > 0
                ? subscription.getFirstChildMonthlyAmountCents()
                : defaultFirstChildMonthlyAmountCents;
    }

    private long effectiveAdditionalChildAmount(ParentSubscription subscription) {
        return subscription.getAdditionalChildMonthlyAmountCents() > 0
                ? subscription.getAdditionalChildMonthlyAmountCents()
                : defaultAdditionalChildMonthlyAmountCents;
    }

    private String extractInvoiceSubscriptionId(Invoice invoice) {
        if (invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null) {
            return null;
        }
        return invoice.getParent().getSubscriptionDetails().getSubscription();
    }

    private java.util.Optional<ParentSubscription> findByInvoice(Invoice invoice) {
        String subscriptionId = extractInvoiceSubscriptionId(invoice);
        if (subscriptionId != null && !subscriptionId.isBlank()) {
            java.util.Optional<ParentSubscription> subscription = subscriptionRepo.findByStripeSubscriptionId(subscriptionId);
            if (subscription.isPresent()) {
                return subscription;
            }
        }
        String customerId = invoice.getCustomer();
        if (customerId != null && !customerId.isBlank()) {
            return subscriptionRepo.findByStripeCustomerId(customerId);
        }
        return java.util.Optional.empty();
    }

    private Parent requireParent(String parentEmail) {
        return parentRepo.findByUserEmail(parentEmail)
                .orElseThrow(() -> new IllegalArgumentException("Parent introuvable."));
    }

    private RequestOptions requestOptions() {
        return RequestOptions.builder().setApiKey(stripeSecretKey).build();
    }

    private String frontendUrl(String path) {
        return frontendBaseUrl.replaceAll("/+$", "") + path;
    }

    private String displayName(Parent parent) {
        return ((parent.getPrenom() == null ? "" : parent.getPrenom()) + " "
                + (parent.getNom() == null ? "" : parent.getNom())).trim();
    }

    private String displayName(Enfant enfant) {
        return ((enfant.getPrenom() == null ? "" : enfant.getPrenom()) + " "
                + (enfant.getNom() == null ? "" : enfant.getNom())).trim();
    }

    private SubscriptionStatus toLocalStatus(String stripeStatus) {
        if (stripeStatus == null) {
            return SubscriptionStatus.INACTIVE;
        }
        return switch (stripeStatus) {
            case "active", "trialing" -> SubscriptionStatus.ACTIVE;
            case "past_due", "unpaid" -> SubscriptionStatus.PAST_DUE;
            case "canceled", "incomplete_expired" -> SubscriptionStatus.CANCELED;
            default -> SubscriptionStatus.CHECKOUT_PENDING;
        };
    }

    private Instant toInstant(Long epochSeconds) {
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
    }
}
