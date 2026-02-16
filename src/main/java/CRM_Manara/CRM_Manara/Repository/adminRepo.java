package CRM_Manara.CRM_Manara.Repository;

import org.apache.catalina.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface adminRepo extends CrudRepository<User,Integer> {


}
