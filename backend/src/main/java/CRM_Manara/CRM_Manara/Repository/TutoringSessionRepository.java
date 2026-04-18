package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.TutoringSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TutoringSessionRepository extends JpaRepository<TutoringSession, Long> {
    List<TutoringSession> findByAnimationId(Long animationId);

    @Query("SELECT s FROM TutoringSession s WHERE s.animation.id IN (SELECT i.animation.id FROM Inscription i WHERE i.enfant.id = :studentId) AND NOT EXISTS (SELECT a FROM TutoringAttempt a WHERE a.student.id = :studentId AND a.question.axis.session.id = s.id)")
    List<TutoringSession> findPendingByStudentId(@Param("studentId") Long studentId);
}
