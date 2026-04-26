package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.ParentSubscriptionChild;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParentSubscriptionChildRepo extends JpaRepository<ParentSubscriptionChild, Long> {

    @Query("select psc from ParentSubscriptionChild psc join fetch psc.enfant e where psc.subscription.id = :subscriptionId order by psc.id")
    List<ParentSubscriptionChild> findBySubscriptionIdDetailed(@Param("subscriptionId") Long subscriptionId);

    boolean existsBySubscriptionIdAndEnfantId(Long subscriptionId, Long enfantId);

    long countBySubscriptionId(Long subscriptionId);

    @Modifying
    @Query("delete from ParentSubscriptionChild psc where psc.subscription.id = :subscriptionId and psc.enfant.id not in :enfantIds")
    void deleteMissingChildren(@Param("subscriptionId") Long subscriptionId, @Param("enfantIds") Collection<Long> enfantIds);

    @Modifying
    @Query("delete from ParentSubscriptionChild psc where psc.subscription.id = :subscriptionId")
    void deleteBySubscriptionId(@Param("subscriptionId") Long subscriptionId);
}
