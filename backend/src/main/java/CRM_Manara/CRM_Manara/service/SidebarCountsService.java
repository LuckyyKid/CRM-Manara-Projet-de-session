package CRM_Manara.CRM_Manara.service;

import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Repository.AdminNotificationRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurNotificationRepo;
import CRM_Manara.CRM_Manara.Repository.AnimateurRepo;
import CRM_Manara.CRM_Manara.Repository.ChatMessageRepo;
import CRM_Manara.CRM_Manara.Repository.ParentNotificationRepo;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import CRM_Manara.CRM_Manara.Repository.UserRepo;
import CRM_Manara.CRM_Manara.dto.SidebarCountsDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SidebarCountsService {

    private final UserRepo userRepo;
    private final ParentRepo parentRepo;
    private final AnimateurRepo animateurRepo;
    private final ParentNotificationRepo parentNotificationRepo;
    private final AnimateurNotificationRepo animateurNotificationRepo;
    private final AdminNotificationRepo adminNotificationRepo;
    private final ChatMessageRepo chatMessageRepo;

    public SidebarCountsService(UserRepo userRepo,
                                ParentRepo parentRepo,
                                AnimateurRepo animateurRepo,
                                ParentNotificationRepo parentNotificationRepo,
                                AnimateurNotificationRepo animateurNotificationRepo,
                                AdminNotificationRepo adminNotificationRepo,
                                ChatMessageRepo chatMessageRepo) {
        this.userRepo = userRepo;
        this.parentRepo = parentRepo;
        this.animateurRepo = animateurRepo;
        this.parentNotificationRepo = parentNotificationRepo;
        this.animateurNotificationRepo = animateurNotificationRepo;
        this.adminNotificationRepo = adminNotificationRepo;
        this.chatMessageRepo = chatMessageRepo;
    }

    @Transactional(readOnly = true)
    public SidebarCountsDto getCountsForEmail(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
        return getCountsForUser(user);
    }

    @Transactional(readOnly = true)
    public SidebarCountsDto getCountsForUser(User user) {
        long notifications = 0;
        if (user.getRole() == SecurityRole.ROLE_PARENT) {
            notifications = parentRepo.findByUser(user)
                    .map(parent -> parentNotificationRepo.countByParentIdAndReadStatusFalseAndArchivedStatusFalse(parent.getId()))
                    .orElse(0L);
        } else if (user.getRole() == SecurityRole.ROLE_ANIMATEUR) {
            notifications = animateurRepo.findByUser(user)
                    .map(animateur -> animateurNotificationRepo.countByAnimateurIdAndReadStatusFalseAndArchivedStatusFalse(animateur.getId()))
                    .orElse(0L);
        } else if (user.getRole() == SecurityRole.ROLE_ADMIN) {
            notifications = 0L;
        }

        long messages = user.getId() == null ? 0 : chatMessageRepo.countByRecipientIdAndReadStatusFalse(user.getId());
        return new SidebarCountsDto(notifications, messages);
    }
}
