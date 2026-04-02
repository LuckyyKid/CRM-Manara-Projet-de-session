package CRM_Manara.CRM_Manara.service;

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
        return parentNotificationRepo.findByParentIdAndArchivedStatusFalseOrderByCreatedAtDesc(parentId).stream()
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnreadForParent(Long parentId) {
        return parentNotificationRepo.countByParentIdAndReadStatusFalseAndArchivedStatusFalse(parentId);
    }

    @Transactional(readOnly = true)
    public List<ParentNotification> getArchivedNotificationsForParent(Long parentId, int limit) {
        return parentNotificationRepo.findByParentIdAndArchivedStatusTrueOrderByCreatedAtDesc(parentId).stream()
                .limit(limit)
                .toList();
    }

    @Transactional
    public void markAllAsReadForParent(Long parentId) {
        List<ParentNotification> notifications = parentNotificationRepo.findByParentIdAndArchivedStatusFalseOrderByCreatedAtDesc(parentId);
        for (ParentNotification notification : notifications) {
            notification.setReadStatus(true);
        }
        parentNotificationRepo.saveAll(notifications);
    }

    @Transactional
    public void markAsRead(Long parentId, Long notificationId) {
        ParentNotification notification = getNotificationForParent(parentId, notificationId);
        notification.setReadStatus(true);
        parentNotificationRepo.save(notification);
    }

    @Transactional
    public void archive(Long parentId, Long notificationId) {
        ParentNotification notification = getNotificationForParent(parentId, notificationId);
        notification.setReadStatus(true);
        notification.setArchivedStatus(true);
        parentNotificationRepo.save(notification);
    }

    @Transactional
    public void restore(Long parentId, Long notificationId) {
        ParentNotification notification = getNotificationForParent(parentId, notificationId);
        notification.setArchivedStatus(false);
        parentNotificationRepo.save(notification);
    }

    @Transactional(readOnly = true)
    public Parent getParentByUserEmail(String email) {
        return parentRepo.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Parent introuvable pour cet email"));
    }

    private ParentNotification getNotificationForParent(Long parentId, Long notificationId) {
        ParentNotification notification = parentNotificationRepo.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable"));
        if (notification.getParent() == null || !notification.getParent().getId().equals(parentId)) {
            throw new IllegalArgumentException("Notification introuvable pour ce parent");
        }
        return notification;
    }
}
