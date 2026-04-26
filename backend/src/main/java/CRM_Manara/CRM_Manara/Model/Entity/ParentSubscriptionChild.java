package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "parent_subscription_child",
        uniqueConstraints = @UniqueConstraint(name = "uk_subscription_child", columnNames = {"subscription_id", "enfant_id"})
)
public class ParentSubscriptionChild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private ParentSubscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enfant_id", nullable = false)
    private Enfant enfant;

    private Instant createdAt;

    protected ParentSubscriptionChild() {
    }

    public ParentSubscriptionChild(ParentSubscription subscription, Enfant enfant) {
        this.subscription = subscription;
        this.enfant = enfant;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ParentSubscription getSubscription() {
        return subscription;
    }

    public Enfant getEnfant() {
        return enfant;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
