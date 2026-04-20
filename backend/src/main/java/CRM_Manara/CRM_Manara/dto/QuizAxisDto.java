package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record QuizAxisDto(
        Long id,
        String title,
        String summary,
        int position,
        List<QuizQuestionDto> questions
) {
}
