package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ActivityRepoTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    ActivityRepo activityRepo;

    @Test
    void save_and_findAll_works() {
        Activity activity = new Activity("Natation", "Initiation", 6, 12, 8, status.OUVERTE, typeActivity.SPORT);
        entityManager.persist(activity);
        entityManager.flush();

        var all = activityRepo.findAll();
        assertThat(all).isNotEmpty();
    }
}
