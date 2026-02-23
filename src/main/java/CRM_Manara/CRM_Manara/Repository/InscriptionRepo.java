package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Inscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InscriptionRepo extends JpaRepository<Inscription, Long> {

    @Query("SELECT i FROM Inscription i WHERE i.enfant.id = :enfantId")
    List<Inscription> findByEnfantId(@Param("enfantId") Long enfantId);

    @Query("SELECT i FROM Inscription i JOIN i.enfant e WHERE e.parent.id = :parentId")
    List<Inscription> findByParentId(@Param("parentId") Long parentId);

    @Query("SELECT i FROM Inscription i WHERE i.animation.id = :animationId")
    List<Inscription> findByAnimationId(@Param("animationId") Long animationId);

    @Query("SELECT i FROM Inscription i WHERE i.animation.animateur.id = :animateurId")
    List<Inscription> findByAnimateurId(@Param("animateurId") Long animateurId);

    @Query("SELECT i FROM Inscription i WHERE i.id = :id AND i.animation.animateur.id = :animateurId")
    Inscription findByIdAndAnimateurId(@Param("id") Long id, @Param("animateurId") Long animateurId);

    long countByAnimationId(Long animationId);

    long countByEnfantId(Long enfantId);

    @Query("SELECT COUNT(i) FROM Inscription i WHERE i.animation.activity.id = :activityId")
    long countByActivityId(@Param("activityId") Long activityId);
}
