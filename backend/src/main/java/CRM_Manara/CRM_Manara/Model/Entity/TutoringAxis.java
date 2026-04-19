package CRM_Manara.CRM_Manara.Model.Entity;

import jakarta.persistence.*;

// Un axe pédagogique extrait par l'IA (ex: "Les fractions", "La syntaxe des phrases")
@Entity
@Table(name = "tutoring_axis")
public class TutoringAxis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // La séance à laquelle cet axe appartient
    @ManyToOne
    @JoinColumn(name = "session_id")
    private TutoringSession session;

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;

    protected TutoringAxis() {}

    public TutoringAxis(TutoringSession session, String name, String description) {
        this.session = session;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public TutoringSession getSession() { return session; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
}
