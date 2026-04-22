package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;
import java.util.List;

public record SportPracticePlanDto(
        Long id,
        Long animationId,
        String activityName,
        String title,
        String summary,
        String sourceNotes,
        LocalDateTime createdAt,
        List<SportPracticePlanItemDto> items
) {
}
