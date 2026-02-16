package CRM_Manara.CRM_Manara.Repository;

import CRM_Manara.CRM_Manara.Entity.Enfant;
import org.apache.catalina.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ParentRepo extends CrudRepository<User,Integer> {

    @Query("SELECT u from Parent u WHERE u.email = : email AND u.password  = : password ")
    public User getByEmailAndPassword(@Param("email") String email, @Param("password") String password);

    @Query ("SELECT P FROM Parent P JOIN p.enfants e WHERE e.nom = : nom ")
    public  User getParentByEnfant(@Param("nom") String nom);

    @Query("SELECT E FROM Enfant  E WHERE e.nom = : nom  ")
    public  User getEnfantByNom(@Param("nom") String nom);






}
