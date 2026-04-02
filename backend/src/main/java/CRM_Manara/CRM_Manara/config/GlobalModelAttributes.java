package CRM_Manara.CRM_Manara.config;

import CRM_Manara.CRM_Manara.service.AdminService;
import CRM_Manara.CRM_Manara.service.AnimateurNotificationService;
import CRM_Manara.CRM_Manara.service.AvatarService;
import CRM_Manara.CRM_Manara.service.ParentNotificationService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

    private final Environment environment;
    private final ParentNotificationService parentNotificationService;
    private final AnimateurNotificationService animateurNotificationService;
    private final AdminService adminService;
    private final AvatarService avatarService;

    public GlobalModelAttributes(Environment environment,
                                 ParentNotificationService parentNotificationService,
                                 AnimateurNotificationService animateurNotificationService,
                                 AdminService adminService,
                                 AvatarService avatarService) {
        this.environment = environment;
        this.parentNotificationService = parentNotificationService;
        this.animateurNotificationService = animateurNotificationService;
        this.adminService = adminService;
        this.avatarService = avatarService;
    }

    @ModelAttribute("googleOAuthEnabled")
    public boolean googleOAuthEnabled() {
        String clientId = environment.getProperty("spring.security.oauth2.client.registration.google.client-id");
        String clientSecret = environment.getProperty("spring.security.oauth2.client.registration.google.client-secret");

        return isConfigured(clientId, "your-google-client-id")
                && isConfigured(clientSecret, "your-google-client-secret");
    }

    private boolean isConfigured(String value, String placeholder) {
        return value != null
                && !value.isBlank()
                && !placeholder.equals(value.trim());
    }

    @ModelAttribute("sidebarParentUnreadNotifications")
    public long sidebarParentUnreadNotifications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication) || !hasRole(authentication, "ROLE_PARENT")) {
            return 0;
        }
        return parentNotificationService.countUnreadForParent(
                parentNotificationService.getParentByUserEmail(authentication.getName()).getId()
        );
    }

    @ModelAttribute("sidebarAdminPendingRequests")
    public long sidebarAdminPendingRequests() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication) || !hasRole(authentication, "ROLE_ADMIN")) {
            return 0;
        }
        return adminService.countPendingInscriptions()
                + adminService.countPendingParents()
                + adminService.countPendingChildren();
    }

    @ModelAttribute("sidebarAnimateurUnreadNotifications")
    public long sidebarAnimateurUnreadNotifications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication) || !hasRole(authentication, "ROLE_ANIMATEUR")) {
            return 0;
        }
        return animateurNotificationService.countUnreadForAnimateur(
                animateurNotificationService.getAnimateurByUserEmail(authentication.getName()).getId()
        );
    }

    @ModelAttribute("currentUserAvatar")
    public String currentUserAvatar() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return null;
        }
        return avatarService.resolveAvatarUrl(authentication.getName());
    }

    @ModelAttribute("currentUserDisplayName")
    public String currentUserDisplayName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return null;
        }
        return avatarService.resolveDisplayName(authentication.getName());
    }

    @ModelAttribute("currentUserHomeUrl")
    public String currentUserHomeUrl() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return "/";
        }
        if (hasRole(authentication, "ROLE_ADMIN")) {
            return "/admin/adminDashboard";
        }
        if (hasRole(authentication, "ROLE_PARENT")) {
            return "/parent/dashboard";
        }
        if (hasRole(authentication, "ROLE_ANIMATEUR")) {
            return "/animateur/dashboard";
        }
        return "/";
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
