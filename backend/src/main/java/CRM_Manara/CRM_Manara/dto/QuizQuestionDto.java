package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record QuizQuestionDto(
        Long id,
        String angle,
        String type,
        String questionText,
        String expectedAnswer,
        int position,
        List<String> options
) {
}
