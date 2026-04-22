package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Animation;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AnimationRepo extends JpaRepository<Animation, Long> {

    @Override
    @EntityGraph(attributePaths = {"activity", "animateur", "animateur.user"})
    List<Animation> findAll();

    @Override
    List<Animation> findAllById(Iterable<Long> longs);

    @EntityGraph(attributePaths = {"activity", "animateur", "animateur.user"})
    @Query("SELECT a FROM Animation a WHERE a.activity.id = :activityId ORDER BY a.startTime")
    List<Animation> findByActivityId(@Param("activityId") Long activityId);

    @EntityGraph(attributePaths = {"activity", "animateur", "animateur.user"})
    @Query("SELECT a FROM Animation a WHERE a.activity.id IN :activityIds ORDER BY a.startTime")
    List<Animation> findByActivityIds(@Param("activityIds") List<Long> activityIds);

    @EntityGraph(attributePaths = {"activity", "animateur", "animateur.user"})
    @Query("SELECT a FROM Animation a WHERE a.animateur.id = :animateurId ORDER BY a.startTime")
    List<Animation> findByAnimateurId(@Param("animateurId") Long animateurId);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM Animation a
            WHERE a.animateur.id = :animateurId
              AND a.activity.type = :type
            """)
    boolean existsByAnimateurIdAndActivityType(@Param("animateurId") Long animateurId,
                                               @Param("type") typeActivity type);
}
