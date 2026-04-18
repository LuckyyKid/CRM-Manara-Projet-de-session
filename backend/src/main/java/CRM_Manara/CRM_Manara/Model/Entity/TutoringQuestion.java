package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;

// Une question générée par l'IA pour un axe pédagogique
@Entity
@Table(name = "tutoring_question")
public class TutoringQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // L'axe auquel appartient cette question
    @ManyToOne
    @JoinColumn(name = "axis_id")
    private TutoringAxis axis;

    // "mcq" = choix multiple, "dev" = réponse développée
    @Column(name = "type")
    private String type;

    // Angle pédagogique : reconnaissance, application, piege, transfert, justification
    @Column(name = "angle")
    private String angle;

    // Difficulté de 1 à 5
    @Column(name = "difficulty")
    private int difficulty;

    // Le texte de la question
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    // Pour les QCM : JSON array des options ex: ["A","B","C","D"]
    @Column(name = "options_json", columnDefinition = "TEXT")
    private String optionsJson;

    // La bonne réponse
    @Column(name = "correct_answer")
    private String correctAnswer;

    // Explication de la réponse
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    // L'erreur fréquente que cette question cible
    @Column(name = "targeted_error")
    private String targetedError;

    protected TutoringQuestion() {}

    public TutoringQuestion(TutoringAxis axis, String type, String angle, int difficulty,
                             String content, String optionsJson, String correctAnswer,
                             String explanation, String targetedError) {
        this.axis = axis;
        this.type = type;
        this.angle = angle;
        this.difficulty = difficulty;
        this.content = content;
        this.optionsJson = optionsJson;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.targetedError = targetedError;
    }

    public Long getId() { return id; }
    public TutoringAxis getAxis() { return axis; }
    public String getType() { return type; }
    public String getAngle() { return angle; }
    public int getDifficulty() { return difficulty; }
    public String getContent() { return content; }
    public String getOptionsJson() { return optionsJson; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public String getTargetedError() { return targetedError; }
}
