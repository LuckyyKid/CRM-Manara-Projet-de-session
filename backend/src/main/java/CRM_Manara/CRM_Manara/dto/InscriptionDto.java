package CRM_Manara.CRM_Manara.dto;

public record InscriptionDto(
        Long id,
        String statusInscription,
        String presenceStatus,
        String incidentNote,
        EnfantSummaryDto enfant,
        AnimationSummaryDto animation
) {
}
