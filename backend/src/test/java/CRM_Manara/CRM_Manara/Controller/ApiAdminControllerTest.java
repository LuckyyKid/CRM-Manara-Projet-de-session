package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.AdminNotification;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Repository.QuizRepo;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.service.AdminNotificationService;
import CRM_Manara.CRM_Manara.service.AdminService;
import CRM_Manara.CRM_Manara.service.BillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiAdminControllerTest {

    private MockMvc mockMvc;
    private AdminService adminService;
    private AdminNotificationService adminNotificationService;
    private QuizRepo quizRepo;
    private BillingService billingService;

    @BeforeEach
    void setUp() {
        adminService = mock(AdminService.class);
        adminNotificationService = mock(AdminNotificationService.class);
        quizRepo = mock(QuizRepo.class);
        billingService = mock(BillingService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ApiAdminController(adminService, adminNotificationService, new ApiDtoMapper(), quizRepo, billingService)).build();
    }

    @Test
    void demandesEndpointAggregatesPendingEntities() throws Exception {
        Parent parent = new Parent("Roy", "Nadia", "123 rue test");
        ReflectionTestUtils.setField(parent, "id", 5L);
        User user = new User("nadia@test.local", "hash");
        user.setRole(SecurityRole.ROLE_PARENT);
        user.setEnabled(false);
        parent.SetUser(user);

        when(adminService.getPendingParents()).thenReturn(List.of(parent));
        when(adminService.getPendingEnfants()).thenReturn(Collections.emptyList());
        when(adminService.getPendingInscriptions()).thenReturn(Collections.emptyList());
        when(adminService.getProcessedInscriptions()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/demandes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingParents[0].id").value(5L))
                .andExpect(jsonPath("$.pendingParents[0].user.enabled").value(false));
    }

    @Test
    void notificationsEndpointReturnsAdminNotifications() throws Exception {
        when(adminNotificationService.getAll()).thenReturn(List.of(new AdminNotification("PARENT", "COMPTE", "Compte en attente")));

        mockMvc.perform(get("/api/admin/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].source").value("PARENT"))
                .andExpect(jsonPath("$[0].type").value("COMPTE"));
    }

    @Test
    void parentStatusEndpointReturnsActionPayload() throws Exception {
        mockMvc.perform(post("/api/admin/parents/8/status").param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value(8L));
    }
}
