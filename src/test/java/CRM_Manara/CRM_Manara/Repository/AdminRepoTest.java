package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Administrateurs;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AccountStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AdminRepoTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    AdminRepo adminRepo;

    @Test
    void save_and_findAll_works() {
        Administrateurs admin = new Administrateurs("Admin", "One");
        admin.setRole(SecurityRole.ROLE_ADMIN);
        admin.setStatus(AccountStatus.ACTIF);
        entityManager.persist(admin);
        entityManager.flush();

        var all = adminRepo.findAll();
        assertThat(all).isNotEmpty();
    }
}
