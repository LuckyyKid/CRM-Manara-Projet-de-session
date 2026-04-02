package CRM_Manara.CRM_Manara.dto;

public record InscriptionDto(
        Long id,
        String status,
        String presenceStatus,
        String incidentNote,
        EnfantSummaryDto enfant,
        AnimationSummaryDto animation
) {
}
