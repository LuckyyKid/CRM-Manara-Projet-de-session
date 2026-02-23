package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ParentRepoTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    ParentRepo parentRepo;

    @Test
    void findByUserEmail_returnsParent() {
        User user = new User("parent@test.com", "hash");
        user.setRole(SecurityRole.ROLE_PARENT);
        entityManager.persist(user);

        Parent parent = new Parent("Doe", "Jane", "123 rue test");
        parent.SetUser(user);
        entityManager.persist(parent);
        entityManager.flush();

        var found = parentRepo.findByUserEmail("parent@test.com");
        assertThat(found).isPresent();
        assertThat(found.get().getNom()).isEqualTo("Doe");
    }
}
