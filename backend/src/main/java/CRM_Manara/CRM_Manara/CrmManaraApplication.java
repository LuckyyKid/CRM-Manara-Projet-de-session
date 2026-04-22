package CRM_Manara.CRM_Manara;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class CrmManaraApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrmManaraApplication.class, args);
	}

}
