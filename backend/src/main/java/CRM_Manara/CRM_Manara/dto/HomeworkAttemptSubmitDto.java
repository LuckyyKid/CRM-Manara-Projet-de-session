package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record HomeworkAttemptSubmitDto(
        Integer elapsedSeconds,
        List<HomeworkAnswerSubmitDto> answers
) {
}
