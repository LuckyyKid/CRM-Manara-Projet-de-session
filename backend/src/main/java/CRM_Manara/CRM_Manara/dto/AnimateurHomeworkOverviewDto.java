package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record AnimateurHomeworkOverviewDto(
        long assignedCount,
        long submittedCount,
        long remainingCount,
        long studentCount,
        List<AnimateurHomeworkStudentRowDto> students
) {
}
