package CRM_Manara.CRM_Manara.dto;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AnimationRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiDtoMapperTest {

    private final ApiDtoMapper mapper = new ApiDtoMapper();

    @Test
    void mapsParentWithChildrenAndUser() {
        User user = new User("parent@test.local", "hash", SecurityRole.ROLE_PARENT, true);
        user.setAvatarUrl("/avatars/test.png");
        ReflectionTestUtils.setField(user, "id", 10L);

        Parent parent = new Parent("Bouchard", "Sophie", "123 rue Example");
        parent.SetUser(user);
        ReflectionTestUtils.setField(parent, "id", 20L);

        Enfant enfant = new Enfant("Bouchard", "Emma", Date.valueOf(LocalDate.of(2018, 4, 12)));
        enfant.setActive(true);
        enfant.setParent(parent);
        ReflectionTestUtils.setField(enfant, "id", 30L);
        ReflectionTestUtils.setField(parent, "enfants", List.of(enfant));

        ParentDto dto = mapper.toParentDto(parent);

        assertNotNull(dto);
        assertEquals(20L, dto.id());
        assertEquals("Sophie", dto.prenom());
        assertEquals("parent@test.local", dto.user().email());
        assertEquals(1, dto.enfants().size());
        assertEquals(LocalDate.of(2018, 4, 12), dto.enfants().get(0).dateNaissance());
        assertTrue(dto.user().enabled());
    }

    @Test
    void mapsAnimationSummaryWithActivityAndAnimateur() {
        Activity activity = new Activity("Robotique", "Atelier robotique", 8, 12, 10, status.OUVERTE, typeActivity.ART);
        ReflectionTestUtils.setField(activity, "id", 100L);

        User user = new User("anim@test.local", "hash", SecurityRole.ROLE_ANIMATEUR, true);
        ReflectionTestUtils.setField(user, "id", 200L);

        Animateur animateur = new Animateur("Tremblay", "Luc");
        animateur.setUser(user);
        ReflectionTestUtils.setField(animateur, "id", 300L);

        Animation animation = new Animation(AnimationRole.PRINCIPAL, animationStatus.ACTIF, LocalDateTime.of(2026, 4, 3, 18, 0), LocalDateTime.of(2026, 4, 3, 19, 30));
        animation.setActivity(activity);
        animation.setAnimateur(animateur);
        ReflectionTestUtils.setField(animation, "id", 400L);

        AnimationSummaryDto dto = mapper.toAnimationSummaryDto(animation);

        assertNotNull(dto);
        assertEquals(400L, dto.id());
        assertEquals("Robotique", dto.activity().name());
        assertEquals("Luc", dto.animateur().prenom());
        assertEquals("ACTIF", dto.status());
        assertEquals("PRINCIPAL", dto.role());
    }
}
