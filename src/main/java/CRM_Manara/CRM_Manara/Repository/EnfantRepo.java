package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Model.Entity.Enfant;
import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnfantRepo extends JpaRepository<Enfant, Long> {

    @Query ("SELECT E FROM Enfant E WHERE E.parent.nom = : nom ")
    public  User getEnfantByParent(@Param("nom") String nom);

    @Query ("SELECT E FROM Enfant  E JOIN E.inscriptions i WHERE i.id = : id")
    public  User getInscriptionById(@Param("id") int id);

    @Query("SELECT E FROM Enfant  E WHERE E.nom = : nom  ")
    public  User getEnfantByNom(@Param("nom") String nom);



}
