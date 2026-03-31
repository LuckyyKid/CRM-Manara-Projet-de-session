package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.ParentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentNotificationRepo extends JpaRepository<ParentNotification, Long> {

    List<ParentNotification> findByParentIdOrderByCreatedAtDesc(Long parentId);

    List<ParentNotification> findByParentIdAndArchivedStatusFalseOrderByCreatedAtDesc(Long parentId);

    List<ParentNotification> findByParentIdAndArchivedStatusTrueOrderByCreatedAtDesc(Long parentId);

    long countByParentIdAndReadStatusFalseAndArchivedStatusFalse(Long parentId);
}
