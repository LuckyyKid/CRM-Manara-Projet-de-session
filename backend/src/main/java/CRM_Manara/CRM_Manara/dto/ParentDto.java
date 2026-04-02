package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record ParentDto(
        Long id,
        String nom,
        String prenom,
        String adresse,
        UserDto user,
        List<EnfantSummaryDto> enfants
) {
}
