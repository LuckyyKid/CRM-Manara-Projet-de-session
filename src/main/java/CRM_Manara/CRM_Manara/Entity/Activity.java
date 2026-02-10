package CRM_Manara.CRM_Manara.Entity;

import CRM_Manara.CRM_Manara.status;
import CRM_Manara.CRM_Manara.typeActivity;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "Activity")

public class Activity {

    @Id
    @Column(name= "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ActivyName")
    private String activyName;

    @Column(name = "Description")
    private String description;

    @Column(name = "AgeMin")
    private int ageMin;

    @Column(name = "AgeMax")
    private int ageMax;

    @Column(name = "Capacity")
    private int capacity;

    @Temporal(TemporalType.DATE)
    @Column(name = "dateCreation",nullable = false, updatable = false)
    private Date dateCreation;

    @PrePersist
    private void onCreate() {
        this.dateCreation = new Date();
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "Start",nullable = false)
    private Date startTime;

    @Column(name = "End",nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date endTime;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "activity_animateur",
           joinColumns = @JoinColumn(name = "activity_id"),
           inverseJoinColumns = @JoinColumn(name = "animateur_id")
   )
   private List<Animateur> animateurs = new ArrayList<>();

    @Column(name = "Adresse")
    private String adresse;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "typeActivity")
    private typeActivity type;

    protected  Activity() {
    }

    public Activity (String activyName, String description, int ageMin, int ageMax, int capacity,
                      status status, typeActivity type, String adresse, Date startTime, Date endTime ) {
    this.activyName = activyName;
    this.description = description;
    this.ageMin = ageMin;
    this.ageMax = ageMax;
    this.capacity = capacity;
    this.startTime = startTime;
    this.endTime = endTime;
    this.adresse = adresse;
    this.type = type;
    this.status = status;

    }

    public Long getId() {
        return id;
    }

    public String getActivyName() {
        return activyName;
    }
    public void setActivyName(String activyName) {
        this.activyName = activyName;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public int getAgeMin() {
        return ageMin;
    }
    public void setAgeMin(int ageMin) {
        this.ageMin = ageMin;
    }
    public int getAgeMax() {
        return ageMax;
    }
    public void setAgeMax(int ageMax) {
        this.ageMax = ageMax;
    }
    public int getCapacity() {
        return capacity;
    }
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    public Date getDateCreation() {
        return dateCreation;
    }

    public Date getStartTime() {
        return startTime;
    }
    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
    public Date getEndTime() {
        return endTime;
    }
    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }
    public List<Animateur> getAnimateurs() {
        return animateurs;
    }
    public void addAnimateur(Animateur animateur) {
        animateurs.add(animateur);
        animateur.getActivities().add(this);
    }
}
