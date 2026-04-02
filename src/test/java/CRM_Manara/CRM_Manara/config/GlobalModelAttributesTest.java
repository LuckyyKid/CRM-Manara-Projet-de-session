package CRM_Manara.CRM_Manara.config;

import CRM_Manara.CRM_Manara.Model.Entity.Service.AdminService;
import CRM_Manara.CRM_Manara.Model.Entity.Service.AnimateurNotificationService;
import CRM_Manara.CRM_Manara.Model.Entity.Service.AvatarService;
import CRM_Manara.CRM_Manara.Model.Entity.Service.ParentNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GlobalModelAttributesTest {

    @Test
    void shouldDisableGoogleOAuthWhenPlaceholdersAreStillPresent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.security.oauth2.client.registration.google.client-id", "your-google-client-id")
                .withProperty("spring.security.oauth2.client.registration.google.client-secret", "your-google-client-secret");

        GlobalModelAttributes attributes = new GlobalModelAttributes(
                environment,
                mock(ParentNotificationService.class),
                mock(AnimateurNotificationService.class),
                mock(AdminService.class),
                mock(AvatarService.class)
        );

        assertFalse(attributes.googleOAuthEnabled());
    }

    @Test
    void shouldEnableGoogleOAuthWhenGoogleCredentialsAreConfigured() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.security.oauth2.client.registration.google.client-id", "client-id-123")
                .withProperty("spring.security.oauth2.client.registration.google.client-secret", "client-secret-456");

        GlobalModelAttributes attributes = new GlobalModelAttributes(
                environment,
                mock(ParentNotificationService.class),
                mock(AnimateurNotificationService.class),
                mock(AdminService.class),
                mock(AvatarService.class)
        );

        assertTrue(attributes.googleOAuthEnabled());
    }
}
