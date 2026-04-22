package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
import CRM_Manara.CRM_Manara.Model.Entity.SportPracticePlan;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SportPracticePlanRepo extends JpaRepository<SportPracticePlan, Long> {

    @EntityGraph(attributePaths = {"animation", "animation.activity", "items"})
    List<SportPracticePlan> findByAnimateurIdOrderByCreatedAtDesc(Long animateurId);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "items"})
    Optional<SportPracticePlan> findByIdAndAnimateurId(Long id, Long animateurId);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "items"})
    @Query("""
            SELECT DISTINCT p
            FROM SportPracticePlan p
            WHERE p.animation IS NOT NULL
              AND p.animation.id IN (
                    SELECT i.animation.id
                    FROM Inscription i
                    WHERE i.enfant.parent.id = :parentId
                      AND i.statusInscription IN :statuses
              )
            ORDER BY p.createdAt DESC
            """)
    List<SportPracticePlan> findVisibleForParent(@Param("parentId") Long parentId,
                                                 @Param("statuses") List<statusInscription> statuses);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "items"})
    @Query("""
            SELECT p
            FROM SportPracticePlan p
            WHERE p.id = :id
              AND p.animation IS NOT NULL
              AND p.animation.id IN (
                    SELECT i.animation.id
                    FROM Inscription i
                    WHERE i.enfant.parent.id = :parentId
                      AND i.statusInscription IN :statuses
              )
            """)
    Optional<SportPracticePlan> findVisibleDetailForParent(@Param("id") Long id,
                                                           @Param("parentId") Long parentId,
                                                           @Param("statuses") List<statusInscription> statuses);
}
