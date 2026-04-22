package CRM_Manara.CRM_Manara.dto;

public record SportPracticePlanItemDto(
        Long id,
        String title,
        String instructions,
        String purpose,
        String durationLabel,
        String safetyTip,
        int position
) {
}
