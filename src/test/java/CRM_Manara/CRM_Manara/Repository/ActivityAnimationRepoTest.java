package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AnimationRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ActivityAnimationRepoTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    ActivityRepo activityRepo;

    @Autowired
    AnimationRepo animationRepo;

    @Test
    void findByActivityId_returnsAnimations() {
        Activity activity = new Activity("Soccer", "Sport", 6, 12, 10, status.OUVERTE, typeActivity.SPORT);
        entityManager.persist(activity);

        Animateur animateur = new Animateur("Coach", "Sam");
        entityManager.persist(animateur);

        Animation animation = new Animation(AnimationRole.PRINCIPAL, animationStatus.ACTIF,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        animation.setActivity(activity);
        animation.setAnimateur(animateur);
        entityManager.persist(animation);
        entityManager.flush();

        var found = animationRepo.findByActivityId(activity.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getActivity().getActivyName()).isEqualTo("Soccer");
    }
}
