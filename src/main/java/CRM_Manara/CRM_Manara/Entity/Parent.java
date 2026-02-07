package CRM_Manara.CRM_Manara.Entity;

import jakarta.persistence.*;

@Entity

@Table(name = "Parent")

public class Parent {

    @Id
    @Column(name= "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;





}
