package CRM_Manara.CRM_Manara.Repository;

import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<User,Integer> {

    @Query("SELECT u from Parent u WHERE u.email = : email AND u.password  = : password ")
    public User getByEmailAndPassword(@Param("email") String email, @Param("password") String password);


}
