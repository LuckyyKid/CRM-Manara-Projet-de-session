package CRM_Manara.CRM_Manara.Controller;

import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.dto.ApiDtoMapper;
import CRM_Manara.CRM_Manara.service.parentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiParentControllerTest {

    private MockMvc mockMvc;
    private parentService parentService;
    private final TestingAuthenticationToken authentication = new TestingAuthenticationToken("parent@test.local", "password");

    @BeforeEach
    void setUp() {
        parentService = mock(parentService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ApiParentController(parentService, new ApiDtoMapper())).build();
    }

    @Test
    void enfantsEndpointReturnsDtos() throws Exception {
        Enfant enfant = new Enfant("Roy", "Emma", new Date());
        ReflectionTestUtils.setField(enfant, "id", 9L);
        enfant.setActive(true);
        when(parentService.getEnfantsForParent("parent@test.local")).thenReturn(Collections.singletonList(enfant));

        mockMvc.perform(get("/api/parent/enfants").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9L))
                .andExpect(jsonPath("$[0].prenom").value("Emma"));
    }

    @Test
    void createInscriptionEndpointReturnsCreatedPayload() throws Exception {
        Inscription inscription = mock(Inscription.class);
        when(inscription.getId()).thenReturn(55L);
        when(parentService.inscrireEnfant(10L, 20L, "parent@test.local")).thenReturn(inscription);

        mockMvc.perform(post("/api/parent/inscriptions")
                        .principal(authentication)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"enfantId":10,"animationId":20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value(55L));
    }
}
