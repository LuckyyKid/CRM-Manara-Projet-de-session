package CRM_Manara.CRM_Manara.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ParentQuizDto(
        QuizDto quiz,
        List<EnfantSummaryDto> eligibleChildren,
        boolean alreadySubmitted,
        LocalDateTime latestSubmittedAt
) {
}
