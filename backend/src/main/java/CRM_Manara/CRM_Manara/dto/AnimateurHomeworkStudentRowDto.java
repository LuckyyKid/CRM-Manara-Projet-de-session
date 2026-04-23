package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;

public record AnimateurHomeworkStudentRowDto(
        Long enfantId,
        String enfantName,
        long assignedCount,
        long submittedCount,
        long remainingCount,
        Double averageScorePercent,
        LocalDateTime latestSubmittedAt,
        String difficultyStatus,
        String difficultyLabel,
        String weakestAxisTitle
) {
}
