package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;

public record TutorAxisProgressDto(
        String axisTitle,
        int quizCount,
        int questionCount,
        Double scorePercent,
        Integer averageResponseTimeSeconds,
        String status,
        String latestQuizTitle,
        LocalDateTime latestQuizCreatedAt
) {
}
