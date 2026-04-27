package CRM_Manara.CRM_Manara.Model.Entity;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.BillingProvider;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "parent_subscription")
public class ParentSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "subscription", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<ParentSubscriptionChild> coveredChildren = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status = SubscriptionStatus.INACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingProvider provider = BillingProvider.STRIPE;

    private String stripeCustomerId;
    private String stripeSubscriptionId;
    private String stripeCheckoutSessionId;
    private String stripePriceId;
    private String stripeAdditionalPriceId;
    private int coveredChildrenCount = 1;
    private int pendingCoveredChildrenCount = 1;
    private long firstChildMonthlyAmountCents = 6000L;
    private long additionalChildMonthlyAmountCents = 4000L;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private boolean cancelAtPeriodEnd;
    private Instant createdAt;
    private Instant updatedAt;

    protected ParentSubscription() {
    }

    public ParentSubscription(Parent parent, User user) {
        this.parent = parent;
        this.user = user;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Parent getParent() {
        return parent;
    }

    public void setParent(Parent parent) {
        this.parent = parent;
    }

    public User getUser() {
        return user;
    }

    public List<ParentSubscriptionChild> getCoveredChildren() {
        return coveredChildren;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public BillingProvider getProvider() {
        return provider;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public void setStripeSubscriptionId(String stripeSubscriptionId) {
        this.stripeSubscriptionId = stripeSubscriptionId;
    }

    public String getStripeCheckoutSessionId() {
        return stripeCheckoutSessionId;
    }

    public void setStripeCheckoutSessionId(String stripeCheckoutSessionId) {
        this.stripeCheckoutSessionId = stripeCheckoutSessionId;
    }

    public String getStripePriceId() {
        return stripePriceId;
    }

    public void setStripePriceId(String stripePriceId) {
        this.stripePriceId = stripePriceId;
    }

    public String getStripeAdditionalPriceId() {
        return stripeAdditionalPriceId;
    }

    public void setStripeAdditionalPriceId(String stripeAdditionalPriceId) {
        this.stripeAdditionalPriceId = stripeAdditionalPriceId;
    }

    public int getCoveredChildrenCount() {
        return coveredChildrenCount;
    }

    public void setCoveredChildrenCount(int coveredChildrenCount) {
        this.coveredChildrenCount = Math.max(1, coveredChildrenCount);
    }

    public int getPendingCoveredChildrenCount() {
        return pendingCoveredChildrenCount;
    }

    public void setPendingCoveredChildrenCount(int pendingCoveredChildrenCount) {
        this.pendingCoveredChildrenCount = Math.max(1, pendingCoveredChildrenCount);
    }

    public long getFirstChildMonthlyAmountCents() {
        return firstChildMonthlyAmountCents;
    }

    public void setFirstChildMonthlyAmountCents(long firstChildMonthlyAmountCents) {
        this.firstChildMonthlyAmountCents = Math.max(0L, firstChildMonthlyAmountCents);
    }

    public long getAdditionalChildMonthlyAmountCents() {
        return additionalChildMonthlyAmountCents;
    }

    public void setAdditionalChildMonthlyAmountCents(long additionalChildMonthlyAmountCents) {
        this.additionalChildMonthlyAmountCents = Math.max(0L, additionalChildMonthlyAmountCents);
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public void setCurrentPeriodStart(Instant currentPeriodStart) {
        this.currentPeriodStart = currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public void setCurrentPeriodEnd(Instant currentPeriodEnd) {
        this.currentPeriodEnd = currentPeriodEnd;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public void setCancelAtPeriodEnd(boolean cancelAtPeriodEnd) {
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
