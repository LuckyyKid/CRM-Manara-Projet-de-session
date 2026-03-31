package CRM_Manara.CRM_Manara.Model.Entity.Service;

import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.ParentNotification;
import CRM_Manara.CRM_Manara.Repository.ParentNotificationRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ParentNotificationService {

    private final ParentNotificationRepo parentNotificationRepo;
    private final ParentRepo parentRepo;

    public ParentNotificationService(ParentNotificationRepo parentNotificationRepo, ParentRepo parentRepo) {
        this.parentNotificationRepo = parentNotificationRepo;
        this.parentRepo = parentRepo;
    }

    @Transactional
    public void createForParent(Parent parent, String category, String title, String message) {
        parentNotificationRepo.save(new ParentNotification(parent, category, title, message));
    }

    @Transactional(readOnly = true)
    public List<ParentNotification> getNotificationsForParent(Long parentId, int limit) {
        return parentNotificationRepo.findByParentIdOrderByCreatedAtDesc(parentId).stream()
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnreadForParent(Long parentId) {
        return parentNotificationRepo.countByParentIdAndReadStatusFalse(parentId);
    }

    @Transactional
    public void markAllAsReadForParent(Long parentId) {
        List<ParentNotification> notifications = parentNotificationRepo.findByParentIdOrderByCreatedAtDesc(parentId);
        for (ParentNotification notification : notifications) {
            notification.setReadStatus(true);
        }
        parentNotificationRepo.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public Parent getParentByUserEmail(String email) {
        return parentRepo.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Parent introuvable pour cet email"));
    }
}
