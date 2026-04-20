package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;
import java.util.List;

public record QuizDto(
        Long id,
        String title,
        String sourceNotes,
        LocalDateTime createdAt,
        Long animationId,
        String activityName,
        List<QuizAxisDto> axes
) {
}
