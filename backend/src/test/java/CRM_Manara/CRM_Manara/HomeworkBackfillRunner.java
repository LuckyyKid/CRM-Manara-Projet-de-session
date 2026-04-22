package CRM_Manara.CRM_Manara;

import CRM_Manara.CRM_Manara.Model.Entity.QuizAttempt;
import CRM_Manara.CRM_Manara.Repository.QuizAttemptRepo;
import CRM_Manara.CRM_Manara.service.HomeworkService;
import org.hibernate.Hibernate;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.transaction.support.TransactionTemplate;

public class HomeworkBackfillRunner {
    public static void main(String[] args) {
        long attemptId = args.length > 0 ? Long.parseLong(args[0]) : 0L;
        if (attemptId <= 0) {
            throw new IllegalArgumentException("Attempt id is required.");
        }

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(CrmManaraApplication.class)
                .web(WebApplicationType.NONE)
                .run()) {
            QuizAttemptRepo quizAttemptRepo = context.getBean(QuizAttemptRepo.class);
            HomeworkService homeworkService = context.getBean(HomeworkService.class);
            TransactionTemplate transactionTemplate = context.getBean(TransactionTemplate.class);

            QuizAttempt attempt = transactionTemplate.execute(status -> {
                QuizAttempt loaded = quizAttemptRepo.findById(attemptId)
                        .orElseThrow(() -> new IllegalArgumentException("Quiz attempt not found: " + attemptId));
                Hibernate.initialize(loaded.getEnfant());
                Hibernate.initialize(loaded.getQuiz());
                Hibernate.initialize(loaded.getQuiz().getAnimateur());
                Hibernate.initialize(loaded.getQuiz().getAnimation());
                if (loaded.getQuiz().getAnimation() != null) {
                    Hibernate.initialize(loaded.getQuiz().getAnimation().getActivity());
                }
                Hibernate.initialize(loaded.getAnswers());
                loaded.getAnswers().forEach(answer -> {
                    Hibernate.initialize(answer.getQuestion());
                    Hibernate.initialize(answer.getQuestion().getAxis());
                });
                return loaded;
            });

            homeworkService.createAutomaticHomeworkFromQuizAttempt(attempt);
            System.out.println("Backfill completed for attempt " + attemptId);
        }
    }
}
