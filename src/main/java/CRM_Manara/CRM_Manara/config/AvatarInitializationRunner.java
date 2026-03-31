package CRM_Manara.CRM_Manara.config;

import CRM_Manara.CRM_Manara.Model.Entity.Service.AvatarService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class AvatarInitializationRunner implements CommandLineRunner {

    private final AvatarService avatarService;

    public AvatarInitializationRunner(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @Override
    public void run(String... args) {
        avatarService.ensureAvatarsForExistingUsers();
    }
}
