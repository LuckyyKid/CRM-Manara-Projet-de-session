package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Service.parentService;
import CRM_Manara.CRM_Manara.Repository.ParentRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ParentControllerTest {

    private MockMvc mockMvc;
    private parentService parentService;
    private final Principal principal = () -> "parent@test.local";

    @BeforeEach
    void setUp() {
        parentService = mock(parentService.class);
        ParentRepo parentRepo = mock(ParentRepo.class);

        parentController controller = new parentController();
        ReflectionTestUtils.setField(controller, "parentService", parentService);
        ReflectionTestUtils.setField(controller, "parentRepo", parentRepo);

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void profilRouteRedirectsToSettings() throws Exception {
        mockMvc.perform(get("/parent/profil").principal(principal))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/settings"));
    }

    @Test
    void inscrireEnfantAjaxReturnsSuccessPayload() throws Exception {
        Inscription inscription = mock(Inscription.class);
        when(inscription.getId()).thenReturn(77L);
        when(parentService.inscrireEnfant(10L, 20L, "parent@test.local")).thenReturn(inscription);

        mockMvc.perform(post("/parent/api/inscriptions")
                        .principal(principal)
                        .param("enfantId", "10")
                        .param("animationId", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.inscriptionId").value(77L));
    }

    @Test
    void inscrireEnfantAjaxReturnsBusinessErrorPayload() throws Exception {
        when(parentService.inscrireEnfant(10L, 20L, "parent@test.local"))
                .thenThrow(new IllegalArgumentException("Une demande existe déjà pour cet enfant sur cette activité."));

        mockMvc.perform(post("/parent/api/inscriptions")
                        .principal(principal)
                        .param("enfantId", "10")
                        .param("animationId", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Une demande existe déjà pour cet enfant sur cette activité."));
    }
}
