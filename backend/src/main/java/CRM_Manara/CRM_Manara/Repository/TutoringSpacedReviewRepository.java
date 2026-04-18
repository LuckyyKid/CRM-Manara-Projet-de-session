package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.TutoringSpacedReview;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TutoringSpacedReviewRepository extends JpaRepository<TutoringSpacedReview, Long> {
    List<TutoringSpacedReview> findByStudentIdAndNextReviewDateLessThanEqual(Long studentId, LocalDate date);
    Optional<TutoringSpacedReview> findByStudentIdAndAxisId(Long studentId, Long axisId);
    void deleteByAxisSessionId(Long sessionId);
}
