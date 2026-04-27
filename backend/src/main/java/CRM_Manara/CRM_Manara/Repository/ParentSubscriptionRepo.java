package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.ParentSubscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ParentSubscriptionRepo extends JpaRepository<ParentSubscription, Long> {
    Optional<ParentSubscription> findByParentId(Long parentId);

    List<ParentSubscription> findAllByParentId(Long parentId);

    Optional<ParentSubscription> findByUserEmail(String email);

    Optional<ParentSubscription> findByStripeCustomerId(String stripeCustomerId);

    Optional<ParentSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    Optional<ParentSubscription> findByStripeCheckoutSessionId(String stripeCheckoutSessionId);

    @Query("select ps from ParentSubscription ps join fetch ps.parent p join fetch ps.user u")
    java.util.List<ParentSubscription> findAllDetailed();
}
