package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Quiz;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.statusInscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizRepo extends JpaRepository<Quiz, Long> {

    @EntityGraph(attributePaths = {"animation", "animation.activity"})
    List<Quiz> findByAnimateurIdOrderByCreatedAtDesc(Long animateurId);

    @EntityGraph(attributePaths = {"animation", "animation.activity", "axes"})
    List<Quiz> findByAnimation_IdOrderByCreatedAtDesc(Long animationId);

    @EntityGraph(attributePaths = {"animation", "animation.activity"})
    Optional<Quiz> findByIdAndAnimateurId(Long id, Long animateurId);

    @EntityGraph(attributePaths = {"animation", "animation.activity"})
    @Query("""
            SELECT DISTINCT q
            FROM Quiz q
            WHERE q.animation IS NOT NULL
              AND q.animation.id IN (
                    SELECT i.animation.id
                    FROM Inscription i
                    WHERE i.enfant.parent.id = :parentId
                      AND i.statusInscription IN :statuses
              )
            ORDER BY q.createdAt DESC
            """)
    List<Quiz> findVisibleForParent(@Param("parentId") Long parentId,
                                    @Param("statuses") List<statusInscription> statuses);
}
