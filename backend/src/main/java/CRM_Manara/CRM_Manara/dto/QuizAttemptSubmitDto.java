package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record QuizAttemptSubmitDto(
        Long enfantId,
        Integer elapsedSeconds,
        List<QuizAnswerSubmitDto> answers
) {
}
