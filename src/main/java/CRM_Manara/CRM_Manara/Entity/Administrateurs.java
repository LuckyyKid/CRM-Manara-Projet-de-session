package CRM_Manara.CRM_Manara.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Administrateurs")

public class Administrateurs {

    @Id
    @Column(name= "ID")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


}
