package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.TutoringGroupAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TutoringGroupAlertRepository extends JpaRepository<TutoringGroupAlert, Long> {
    List<TutoringGroupAlert> findByAnimationIdOrderByCreatedAtDesc(Long animationId);
    void deleteByAxisSessionId(Long sessionId);
}
