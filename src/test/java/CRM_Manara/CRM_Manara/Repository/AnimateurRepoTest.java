package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AnimateurRepoTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    AnimateurRepo animateurRepo;

    @Test
    void findByUserEmail_returnsAnimateur() {
        User user = new User("anim@test.com", "hash");
        user.setRole(SecurityRole.ROLE_ANIMATEUR);
        entityManager.persist(user);

        Animateur animateur = new Animateur("Ali", "N");
        animateur.setUser(user);
        entityManager.persist(animateur);
        entityManager.flush();

        var found = animateurRepo.findByUserEmail("anim@test.com");
        assertThat(found).isPresent();
        assertThat(found.get().getNom()).isEqualTo("Ali");
    }
}
