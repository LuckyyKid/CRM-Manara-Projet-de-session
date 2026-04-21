package CRM_Manara.CRM_Manara.dto;

public record TutorQuizAnswerDto(
        Long questionId,
        String axisTitle,
        String angle,
        String questionText,
        String expectedAnswer,
        String answerText
) {
}
