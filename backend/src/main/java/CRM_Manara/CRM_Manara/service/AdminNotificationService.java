package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.AdminNotification;
import CRM_Manara.CRM_Manara.Repository.AdminNotificationRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminNotificationService {

    private final AdminNotificationRepo adminNotificationRepo;

    public AdminNotificationService(AdminNotificationRepo adminNotificationRepo) {
        this.adminNotificationRepo = adminNotificationRepo;
    }

    @Transactional
    public void create(String source, String type, String message) {
        adminNotificationRepo.save(new AdminNotification(source, type, message));
    }

    @Transactional(readOnly = true)
    public List<AdminNotification> getAll() {
        return adminNotificationRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AdminNotification> getRecent(int limit) {
        List<AdminNotification> notifications = adminNotificationRepo.findAllByOrderByCreatedAtDesc();
        return notifications.stream().limit(limit).toList();
    }
}
