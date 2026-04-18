package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.TutoringAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TutoringAttemptRepository extends JpaRepository<TutoringAttempt, Long> {
    List<TutoringAttempt> findByStudentIdAndQuestionAxisSessionId(Long studentId, Long sessionId);
    List<TutoringAttempt> findByStudentIdAndQuestionAxisId(Long studentId, Long axisId);
    List<TutoringAttempt> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<TutoringAttempt> findByQuestionAxisSessionId(Long sessionId);
    void deleteByQuestionAxisSessionId(Long sessionId);
}
