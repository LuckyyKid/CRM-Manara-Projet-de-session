package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.AnimateurNotification;
import CRM_Manara.CRM_Manara.Repository.AnimateurNotificationRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnimateurNotificationService {

    private final AnimateurNotificationRepo animateurNotificationRepo;
    private final AnimateurRepo animateurRepo;

    public AnimateurNotificationService(AnimateurNotificationRepo animateurNotificationRepo,
                                        AnimateurRepo animateurRepo) {
        this.animateurNotificationRepo = animateurNotificationRepo;
        this.animateurRepo = animateurRepo;
    }

    @Transactional
    public void createForAnimateur(Animateur animateur, String category, String title, String message) {
        if (animateur == null) {
            return;
        }
        animateurNotificationRepo.save(new AnimateurNotification(animateur, category, title, message));
    }

    @Transactional(readOnly = true)
    public Animateur getAnimateurByUserEmail(String email) {
        return animateurRepo.findByUserEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Animateur introuvable pour cet email"));
    }

    @Transactional(readOnly = true)
    public List<AnimateurNotification> getNotificationsForAnimateur(Long animateurId, int limit) {
        return animateurNotificationRepo.findByAnimateurIdAndArchivedStatusFalseOrderByCreatedAtDesc(animateurId)
                .stream()
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AnimateurNotification> getArchivedNotificationsForAnimateur(Long animateurId, int limit) {
        return animateurNotificationRepo.findByAnimateurIdAndArchivedStatusTrueOrderByCreatedAtDesc(animateurId)
                .stream()
                .limit(limit)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countUnreadForAnimateur(Long animateurId) {
        return animateurNotificationRepo.countByAnimateurIdAndReadStatusFalseAndArchivedStatusFalse(animateurId);
    }

    @Transactional
    public void markAllAsReadForAnimateur(Long animateurId) {
        List<AnimateurNotification> notifications = animateurNotificationRepo
                .findByAnimateurIdAndArchivedStatusFalseOrderByCreatedAtDesc(animateurId);
        for (AnimateurNotification notification : notifications) {
            notification.setReadStatus(true);
        }
        animateurNotificationRepo.saveAll(notifications);
    }

    @Transactional
    public void markAsRead(Long animateurId, Long notificationId) {
        AnimateurNotification notification = getNotificationForAnimateur(animateurId, notificationId);
        notification.setReadStatus(true);
        animateurNotificationRepo.save(notification);
    }

    @Transactional
    public void archive(Long animateurId, Long notificationId) {
        AnimateurNotification notification = getNotificationForAnimateur(animateurId, notificationId);
        notification.setArchivedStatus(true);
        notification.setReadStatus(true);
        animateurNotificationRepo.save(notification);
    }

    @Transactional
    public void restore(Long animateurId, Long notificationId) {
        AnimateurNotification notification = getNotificationForAnimateur(animateurId, notificationId);
        notification.setArchivedStatus(false);
        animateurNotificationRepo.save(notification);
    }

    private AnimateurNotification getNotificationForAnimateur(Long animateurId, Long notificationId) {
        AnimateurNotification notification = animateurNotificationRepo.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification introuvable."));
        if (notification.getAnimateur() == null || !notification.getAnimateur().getId().equals(animateurId)) {
            throw new IllegalArgumentException("Notification introuvable.");
        }
        return notification;
    }
}
