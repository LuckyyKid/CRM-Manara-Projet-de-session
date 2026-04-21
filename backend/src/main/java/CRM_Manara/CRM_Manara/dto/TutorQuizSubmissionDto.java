package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TutorQuizSubmissionDto(
        Long id,
        Long quizId,
        String quizTitle,
        Long animationId,
        String activityName,
        Long enfantId,
        String enfantName,
        LocalDateTime submittedAt,
        Integer elapsedSeconds,
        Double scorePercent,
        String status,
        List<TutorQuizAnswerDto> answers
) {
}
