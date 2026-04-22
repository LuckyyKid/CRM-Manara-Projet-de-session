package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record AnimateurHomeworkStudentDetailDto(
        Long enfantId,
        String enfantName,
        long assignedCount,
        long submittedCount,
        long remainingCount,
        Double averageScorePercent,
        List<HomeworkDto> assignments,
        List<HomeworkAttemptDto> attempts
) {
}
