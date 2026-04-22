package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InscriptionRepo extends JpaRepository<Inscription, Long> {

    @Override
    @EntityGraph(attributePaths = {
            "enfant", "enfant.parent", "enfant.parent.user",
            "animation", "animation.activity", "animation.animateur", "animation.animateur.user"
    })
    List<Inscription> findAll();

    @EntityGraph(attributePaths = {
            "enfant", "enfant.parent", "enfant.parent.user",
            "animation", "animation.activity", "animation.animateur", "animation.animateur.user"
    })
    @Query("SELECT i FROM Inscription i WHERE i.enfant.id = :enfantId")
    List<Inscription> findByEnfantId(@Param("enfantId") Long enfantId);

    @EntityGraph(attributePaths = {
            "enfant", "enfant.parent", "enfant.parent.user",
            "animation", "animation.activity", "animation.animateur", "animation.animateur.user"
    })
    @Query("SELECT i FROM Inscription i JOIN i.enfant e WHERE e.parent.id = :parentId")
    List<Inscription> findByParentId(@Param("parentId") Long parentId);

    @EntityGraph(attributePaths = {
            "enfant", "enfant.parent", "enfant.parent.user",
            "animation", "animation.activity", "animation.animateur", "animation.animateur.user"
    })
    @Query("SELECT i FROM Inscription i WHERE i.animation.id = :animationId")
    List<Inscription> findByAnimationId(@Param("animationId") Long animationId);

    @EntityGraph(attributePaths = {
            "enfant", "enfant.parent", "enfant.parent.user",
            "animation", "animation.activity", "animation.animateur", "animation.animateur.user"
    })
    @Query("SELECT i FROM Inscription i WHERE i.animation.id IN :animationIds")
    List<Inscription> findByAnimationIdIn(@Param("animationIds") List<Long> animationIds);

    @EntityGraph(attributePaths = {
            "enfant", "enfant.parent", "enfant.parent.user",
            "animation", "animation.activity", "animation.animateur", "animation.animateur.user"
    })
    @Query("SELECT i FROM Inscription i WHERE i.animation.animateur.id = :animateurId")
    List<Inscription> findByAnimateurId(@Param("animateurId") Long animateurId);

    @EntityGraph(attributePaths = {
            "enfant", "enfant.parent", "enfant.parent.user",
            "animation", "animation.activity", "animation.animateur", "animation.animateur.user"
    })
    @Query("SELECT i FROM Inscription i WHERE i.id = :id AND i.animation.animateur.id = :animateurId")
    Inscription findByIdAndAnimateurId(@Param("id") Long id, @Param("animateurId") Long animateurId);

    @Query("SELECT i FROM Inscription i WHERE i.enfant.id = :enfantId AND i.animation.id = :animationId")
    Optional<Inscription> findByEnfantIdAndAnimationId(@Param("enfantId") Long enfantId, @Param("animationId") Long animationId);

    @EntityGraph(attributePaths = {
            "enfant", "enfant.parent", "enfant.parent.user",
            "animation", "animation.activity", "animation.animateur", "animation.animateur.user"
    })
    @Query("SELECT i FROM Inscription i WHERE i.enfant.id = :enfantId AND i.animation.activity.id = :activityId")
    List<Inscription> findByEnfantIdAndActivityId(@Param("enfantId") Long enfantId, @Param("activityId") Long activityId);

    long countByAnimationId(Long animationId);

    long countByEnfantId(Long enfantId);

    long countByEnfantParentId(Long parentId);

    long countByStatusInscription(statusInscription status);

    @Query("SELECT COUNT(i) FROM Inscription i WHERE i.animation.activity.id = :activityId")
    long countByActivityId(@Param("activityId") Long activityId);

    @Query("""
            SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
            FROM Inscription i
            WHERE i.enfant.parent.id = :parentId
              AND i.animation.activity.type = :type
            """)
    boolean existsByParentIdAndActivityType(@Param("parentId") Long parentId,
                                            @Param("type") typeActivity type);
}
