package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;
import java.util.List;

public record HomeworkAttemptDto(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        Long enfantId,
        String enfantName,
        LocalDateTime submittedAt,
        Integer elapsedSeconds,
        Double scorePercent,
        String status,
        List<TutorQuizAnswerDto> answers
) {
}
