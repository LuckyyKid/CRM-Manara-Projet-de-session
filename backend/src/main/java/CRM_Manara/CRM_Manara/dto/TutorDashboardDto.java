package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TutorDashboardDto(
        int enrolledChildrenCount,
        int quizResponderCount,
        int quizAttemptCount,
        Double quizParticipationPercent,
        Double averageStudentAge,
        int quizCount,
        int axisCount,
        int questionCount,
        Double globalProgressPercent,
        Integer averageResponseTimeSeconds,
        String progressStatus,
        String nextSessionSuggestion,
        LocalDateTime lastQuizCreatedAt,
        List<TutorAxisProgressDto> axes,
        List<TutorAxisProgressDto> persistentAxes
) {
}
