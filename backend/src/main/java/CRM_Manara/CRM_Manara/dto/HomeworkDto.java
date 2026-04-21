package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record HomeworkDto(
        Long id,
        Long enfantId,
        String enfantName,
        Long animationId,
        String activityName,
        String title,
        String summary,
        String status,
        LocalDateTime createdAt,
        LocalDate dueDate,
        List<HomeworkExerciseDto> exercises,
        Double latestScorePercent,
        LocalDateTime latestSubmittedAt
) {
}
