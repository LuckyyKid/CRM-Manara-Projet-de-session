package CRM_Manara.CRM_Manara.Repository;

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
class UserRepoTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    UserRepo userRepo;

    @Test
    void findByEmail_returnsUser() {
        User user = new User("u1@test.com", "hash");
        user.setRole(SecurityRole.ROLE_PARENT);
        entityManager.persist(user);
        entityManager.flush();

        var found = userRepo.findByEmail("u1@test.com");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("u1@test.com");
    }

    @Test
    void findByEmailAndPassword_returnsUser() {
        User user = new User("u2@test.com", "secret");
        user.setRole(SecurityRole.ROLE_PARENT);
        entityManager.persist(user);
        entityManager.flush();

        var found = userRepo.findByEmailAndPassword("u2@test.com", "secret");
        assertThat(found).isPresent();
    }
}
