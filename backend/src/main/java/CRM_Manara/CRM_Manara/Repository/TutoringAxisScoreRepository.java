package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.TutoringAxisScore;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TutoringAxisScoreRepository extends JpaRepository<TutoringAxisScore, Long> {
    List<TutoringAxisScore> findByStudentId(Long studentId);
    List<TutoringAxisScore> findByAxisId(Long axisId);
    Optional<TutoringAxisScore> findByStudentIdAndAxisId(Long studentId, Long axisId);
    List<TutoringAxisScore> findByAxisIdAndScoreLessThan(Long axisId, double threshold);
    void deleteByAxisSessionId(Long sessionId);
}
