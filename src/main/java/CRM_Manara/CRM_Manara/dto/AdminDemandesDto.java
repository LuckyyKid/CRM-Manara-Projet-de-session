package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record AdminDemandesDto(
        List<ParentDto> pendingParents,
        List<EnfantDto> pendingEnfants,
        List<AdminInscriptionReviewDto> pendingInscriptions,
        List<AdminInscriptionReviewDto> processedInscriptions
) {
}
