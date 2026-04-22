package CRM_Manara.CRM_Manara.dto;

public record SportPracticePlanCreateRequestDto(
        String title,
        String sourceNotes,
        Long animationId
) {
}
