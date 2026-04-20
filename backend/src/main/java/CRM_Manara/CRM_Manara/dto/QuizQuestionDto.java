package CRM_Manara.CRM_Manara.dto;

public record QuizQuestionDto(
        Long id,
        String angle,
        String type,
        String questionText,
        String expectedAnswer,
        int position
) {
}
