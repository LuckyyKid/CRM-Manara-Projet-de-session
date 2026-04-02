package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record ParentActivitiesResponseDto(
        List<EnfantSummaryDto> enfants,
        List<InscriptionDto> inscriptions,
        List<ParentActivityViewDto> activities
) {
}
