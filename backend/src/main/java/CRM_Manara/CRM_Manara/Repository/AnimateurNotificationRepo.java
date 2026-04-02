package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.AnimateurNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnimateurNotificationRepo extends JpaRepository<AnimateurNotification, Long> {
    List<AnimateurNotification> findByAnimateurIdOrderByCreatedAtDesc(Long animateurId);

    List<AnimateurNotification> findByAnimateurIdAndArchivedStatusFalseOrderByCreatedAtDesc(Long animateurId);

    List<AnimateurNotification> findByAnimateurIdAndArchivedStatusTrueOrderByCreatedAtDesc(Long animateurId);

    long countByAnimateurIdAndReadStatusFalseAndArchivedStatusFalse(Long animateurId);

    void deleteByAnimateurId(Long animateurId);
}
