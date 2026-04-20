package CRM_Manara.CRM_Manara.dto;

public record QuizCreateRequestDto(
        String title,
        String sourceNotes,
        Long animationId
) {
}
