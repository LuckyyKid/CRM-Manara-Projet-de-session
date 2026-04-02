package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.AdminNotification;
import CRM_Manara.CRM_Manara.service.AdminNotificationService;
import CRM_Manara.CRM_Manara.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminControllerTest {

    private MockMvc mockMvc;
    private AdminService adminService;
    private AdminNotificationService adminNotificationService;

    @BeforeEach
    void setUp() {
        adminService = mock(AdminService.class);
        adminNotificationService = mock(AdminNotificationService.class);

        adminController controller = new adminController();
        ReflectionTestUtils.setField(controller, "adminService", adminService);
        ReflectionTestUtils.setField(controller, "adminNotificationService", adminNotificationService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void inscriptionsRouteRedirectsToDemandes() throws Exception {
        mockMvc.perform(get("/admin/inscriptions"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/demandes"));
    }

    @Test
    void notificationsPageDisplaysAdminNotifications() throws Exception {
        when(adminNotificationService.getAll()).thenReturn(List.of(new AdminNotification("PARENT", "COMPTE", "Compte modifié")));

        mockMvc.perform(get("/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/adminNotifications"));
    }

    @Test
    void approveInscriptionAjaxReturnsErrorPayloadWhenSessionIsFull() throws Exception {
        doThrow(new IllegalStateException("Plus de places disponibles sur cette session. La demande reste en attente."))
                .when(adminService).approveInscription(12L);

        mockMvc.perform(post("/admin/api/inscriptions/12/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Plus de places disponibles sur cette session. La demande reste en attente."));
    }

    @Test
    void approveInscriptionRedirectAddsFlashErrorWhenSessionIsFull() throws Exception {
        doThrow(new IllegalStateException("Plus de places disponibles sur cette session. La demande reste en attente."))
                .when(adminService).approveInscription(18L);

        mockMvc.perform(post("/admin/inscriptions/18/approve"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/demandes"))
                .andExpect(flash().attribute("error", "Plus de places disponibles sur cette session. La demande reste en attente."));
    }
}
