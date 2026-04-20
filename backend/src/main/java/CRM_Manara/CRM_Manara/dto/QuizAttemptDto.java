package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;

public record QuizAttemptDto(
        Long id,
        Long quizId,
        String quizTitle,
        Long enfantId,
        String enfantName,
        LocalDateTime submittedAt,
        Integer elapsedSeconds,
        Double scorePercent,
        String status
) {
}
