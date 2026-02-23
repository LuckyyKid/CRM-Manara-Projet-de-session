package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Date;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EnfantRepoTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    EnfantRepo enfantRepo;

    @Test
    void findByParentId_returnsEnfants() {
        User user = new User("p1@test.com", "hash");
        user.setRole(SecurityRole.ROLE_PARENT);
        entityManager.persist(user);

        Parent parent = new Parent("Doe", "John", "addr");
        parent.SetUser(user);
        entityManager.persist(parent);

        Enfant e1 = new Enfant("Alpha", "Kid", Date.valueOf("2015-01-01"));
        e1.setParent(parent);
        entityManager.persist(e1);

        Enfant e2 = new Enfant("Beta", "Kid", Date.valueOf("2016-01-01"));
        e2.setParent(parent);
        entityManager.persist(e2);

        entityManager.flush();

        var found = enfantRepo.findByParentId(parent.getId());
        assertThat(found).hasSize(2);
    }

    @Test
    void findByIdAndParentId_returnsEnfant() {
        User user = new User("p2@test.com", "hash");
        user.setRole(SecurityRole.ROLE_PARENT);
        entityManager.persist(user);

        Parent parent = new Parent("Doe", "Jane", "addr");
        parent.SetUser(user);
        entityManager.persist(parent);

        Enfant e1 = new Enfant("Gamma", "Kid", Date.valueOf("2017-01-01"));
        e1.setParent(parent);
        entityManager.persist(e1);
        entityManager.flush();

        var found = enfantRepo.findByIdAndParentId(e1.getId(), parent.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getNom()).isEqualTo("Gamma");
    }
}
