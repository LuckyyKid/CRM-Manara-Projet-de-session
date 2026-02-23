package CRM_Manara.CRM_Manara.Model.Entity;

import CRM_Manara.CRM_Manara.Model.Entity.Enum.status;
import CRM_Manara.CRM_Manara.Model.Entity.Enum.typeActivity;
import jakarta.persistence.*;

import java.util.Date;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "typeActivity")
    private typeActivity type;

    protected  Activity() {
    }

    public Activity (String activyName, String description, int ageMin, int ageMax, int capacity,
                      status status, typeActivity type ) {
    this.activyName = activyName;
    this.description = description;
    this.ageMin = ageMin;
    this.ageMax = ageMax;
    this.capacity = capacity;
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
    public status getStatus() {
        return status;
    }
    public typeActivity getType() {
        return type;
    }

}
