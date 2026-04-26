package CRM_Manara.CRM_Manara.dto;

public record BillingChildCoverageDto(
        Long enfantId,
        String nom,
        String prenom,
        boolean active,
        boolean covered
) {
}
