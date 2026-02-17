package CRM_Manara.CRM_Manara.Repository;


import CRM_Manara.CRM_Manara.Model.Entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentRepo extends CrudRepository<User,Integer> {


    @Query ("SELECT P FROM Parent P JOIN P.enfants E WHERE E.nom = : nom ")
    public  User getParentByEnfant(@Param("nom") String nom);

    @Query("SELECT E FROM Enfant  E WHERE E.nom = : nom  ")
    public  User getEnfantByNom(@Param("nom") String nom);






}
