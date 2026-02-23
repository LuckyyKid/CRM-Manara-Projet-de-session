package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Activity;
import CRM_Manara.CRM_Manara.Model.Entity.Animateur;
import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.AnimationRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.SecurityRole;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.animationStatus;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Parent;
import CRM_Manara.CRM_Manara.Model.Entity.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Date;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class InscriptionRepoTest {

    @Autowired
    EntityManager entityManager;

    @Autowired
    InscriptionRepo inscriptionRepo;

    @Test
    void findByParentId_returnsInscriptions() {
        User user = new User("p3@test.com", "hash");
        user.setRole(SecurityRole.ROLE_PARENT);
        entityManager.persist(user);

        Parent parent = new Parent("Doe", "Alex", "addr");
        parent.SetUser(user);
        entityManager.persist(parent);

        Enfant enfant = new Enfant("Kid", "One", Date.valueOf("2014-01-01"));
        enfant.setParent(parent);
        entityManager.persist(enfant);

        Activity activity = new Activity("Art", "Paint", 6, 12, 10, status.OUVERTE, typeActivity.ART);
        entityManager.persist(activity);

        Animateur animateur = new Animateur("Coach", "Lee");
        entityManager.persist(animateur);

        Animation animation = new Animation(AnimationRole.PRINCIPAL, animationStatus.ACTIF,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(1).plusHours(2));
        animation.setActivity(activity);
        animation.setAnimateur(animateur);
        entityManager.persist(animation);

        Inscription inscription = new Inscription(enfant, animation);
        entityManager.persist(inscription);
        entityManager.flush();

        var found = inscriptionRepo.findByParentId(parent.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getEnfant().getNom()).isEqualTo("Kid");
    }

    @Test
    void findByEnfantId_returnsInscriptions() {
        User user = new User("p4@test.com", "hash");
        user.setRole(SecurityRole.ROLE_PARENT);
        entityManager.persist(user);

        Parent parent = new Parent("Doe", "Marie", "addr");
        parent.SetUser(user);
        entityManager.persist(parent);

        Enfant enfant = new Enfant("Kid", "Two", Date.valueOf("2013-01-01"));
        enfant.setParent(parent);
        entityManager.persist(enfant);

        Activity activity = new Activity("Music", "Piano", 6, 12, 10, status.OUVERTE, typeActivity.MUSIQUE);
        entityManager.persist(activity);

        Animateur animateur = new Animateur("Coach", "Kim");
        entityManager.persist(animateur);

        Animation animation = new Animation(AnimationRole.PRINCIPAL, animationStatus.ACTIF,
                LocalDateTime.now().plusDays(2), LocalDateTime.now().plusDays(2).plusHours(2));
        animation.setActivity(activity);
        animation.setAnimateur(animateur);
        entityManager.persist(animation);

        Inscription inscription = new Inscription(enfant, animation);
        entityManager.persist(inscription);
        entityManager.flush();

        var found = inscriptionRepo.findByEnfantId(enfant.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).getAnimation().getActivity().getActivyName()).isEqualTo("Music");
    }
}
