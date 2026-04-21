package CRM_Manara.CRM_Manara.dto;

public record HomeworkExerciseDto(
        Long id,
        String axisTitle,
        String difficulty,
        String questionText,
        String expectedAnswer,
        String targetMistake,
        int position
) {
}
