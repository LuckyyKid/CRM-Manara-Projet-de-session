package CRM_Manara.CRM_Manara.dto;

import java.util.List;

public record AnimateurHomeworkStudentDetailDto(
        Long enfantId,
        String enfantName,
        long assignedCount,
        long submittedCount,
        long remainingCount,
        Double averageScorePercent,
        String difficultyStatus,
        String difficultyLabel,
        List<String> weakAxes,
        List<QuizAttemptDto> quizAttempts,
        List<TutorQuizAnswerDto> failedQuestions,
        List<HomeworkDto> assignments,
        List<HomeworkAttemptDto> attempts
) {
}
