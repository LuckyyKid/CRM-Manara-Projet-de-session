package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnimationRepo extends JpaRepository<Animation, Long> {

    @Query("SELECT a FROM Animation a WHERE a.activity.id = :activityId")
    List<Animation> findByActivityId(@Param("activityId") Long activityId);

    @Query("SELECT a FROM Animation a WHERE a.animateur.id = :animateurId")
    List<Animation> findByAnimateurId(@Param("animateurId") Long animateurId);
}
