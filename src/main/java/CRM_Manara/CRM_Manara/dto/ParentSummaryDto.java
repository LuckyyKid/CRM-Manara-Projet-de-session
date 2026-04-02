package CRM_Manara.CRM_Manara.dto;

public record ParentSummaryDto(
        Long id,
        String nom,
        String prenom,
        String email,
        boolean enabled
) {
}
