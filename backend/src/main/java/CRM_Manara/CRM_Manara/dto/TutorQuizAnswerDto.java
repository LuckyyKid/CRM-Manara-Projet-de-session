package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record TutorQuizAnswerDto(
        Long questionId,
        String axisTitle,
        String angle,
        String questionText,
        String expectedAnswer,
        String answerText,
        List<String> options
) {
}
