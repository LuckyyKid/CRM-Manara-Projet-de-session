package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record AnimateurHomeworkOverviewDto(
        long assignedCount,
        long submittedCount,
        long remainingCount,
        long studentCount,
        long strugglingStudentCount,
        String weakestAxisTitle,
        Double weakestAxisScorePercent,
        String mostFailedQuestionText,
        Long mostFailedQuestionCount,
        List<AnimateurHomeworkStudentRowDto> students
) {
}
